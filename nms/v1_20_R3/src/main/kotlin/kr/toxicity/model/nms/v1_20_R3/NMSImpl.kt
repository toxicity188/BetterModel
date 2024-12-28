package kr.toxicity.model.nms.v1_20_R3

import com.google.common.collect.ImmutableList
import com.google.gson.JsonParser
import com.mojang.datafixers.util.Pair
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox
import kr.toxicity.model.api.nms.*
import kr.toxicity.model.api.tracker.EntityTracker
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Slime
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform.*
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot.*
import org.bukkit.inventory.EquipmentSlot.HEAD
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.util.Transformation
import org.joml.Vector3f
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class NMSImpl : NMS {

    companion object {
        private const val INJECT_NAME = "bettermodel_channel_handler"

        @Suppress("UNCHECKED_CAST")
        private val slimeSize = Slime::class.java.declaredFields.first {
            EntityDataAccessor::class.java.isAssignableFrom(it.type)
        }.run {
            isAccessible = true
            get(null) as EntityDataAccessor<Int>
        }

        private fun Class<*>.serializers() = declaredFields.filter { f ->
            EntityDataAccessor::class.java.isAssignableFrom(f.type)
        }

        private fun Field.toSerializerId() = run {
            isAccessible = true
            (get(null) as EntityDataAccessor<*>).id
        }

        private val transformSet = Display::class.java.serializers().subList(0, 6).map { e ->
            e.toSerializerId()
        }.toSet()
        private val transformSetWithItem = transformSet + ItemDisplay::class.java.serializers().first().toSerializerId()
    }

    private class PacketBundlerImpl(
        private val list: MutableList<Packet<ClientGamePacketListener>>
    ) : PacketBundler, MutableList<Packet<ClientGamePacketListener>> by list {
        override fun send(player: Player) {
            if (isNotEmpty()) (player as CraftPlayer).handle.connection.send(ClientboundBundlePacket(this))
        }
    }

    private fun Int.toEntity() = MinecraftServer.getServer().allLevels.firstNotNullOfOrNull {
        it.entityLookup.get(this)
    }

    private fun Entity.toVoid(bundler: PacketBundlerImpl) {
        bundler.add(ClientboundSetEquipmentPacket(id, EquipmentSlot.entries.map { e ->
            Pair.of(e, Items.AIR.defaultInstance)
        }))
        val inv = isInvisible
        isInvisible = true
        bundler.add(ClientboundSetEntityDataPacket(
            id,
            entityData.nonDefaultValues!!
        ))
        isInvisible = inv
    }

    inner class PlayerChannelHandlerImpl(
        private val player: Player
    ) : PlayerChannelHandler, ChannelDuplexHandler() {
        private val connection = (player as CraftPlayer).handle.connection
        private val entityUUIDMap = ConcurrentHashMap<UUID, EntityTracker>()

        init {
            val pipeLine = connection.connection.channel.pipeline()
            pipeLine.toMap().forEach {
                if (it.value is Connection) pipeLine.addBefore(it.key, INJECT_NAME, this)
            }
        }

        private var showPlayerLimb = true
        override fun showPlayerLimb(): Boolean = showPlayerLimb
        override fun showPlayerLimb(show: Boolean) {
            showPlayerLimb = show
        }

        override fun close() {
            val channel = connection.connection.channel
            channel.eventLoop().submit {
                channel.pipeline().remove(INJECT_NAME)
            }
            unregisterAll()
        }

        override fun unregisterAll() {
            entityUUIDMap.values.toList().forEach {
                it.remove(player)
            }
        }
        override fun player(): Player = player
        private fun send(packet: Packet<*>) = connection.send(packet)

        private fun Int.toTracker() = toEntity()?.let {
            entityUUIDMap[it.uuid]
        }

        override fun startTrack(tracker: EntityTracker) {
            val entity = (tracker.entity as CraftEntity).handle
            entityUUIDMap.computeIfAbsent(entity.uuid) {
                tracker
            }
        }

        override fun endTrack(tracker: EntityTracker) {
            val e = tracker.entity
            val handle = (e as CraftEntity).handle
            entityUUIDMap.remove(handle.uuid)
            send(ClientboundSetEntityDataPacket(handle.id, handle.entityData.nonDefaultValues!!))
            if (e is LivingEntity) {
                e.equipment?.let { i ->
                    send(ClientboundSetEquipmentPacket(handle.id, org.bukkit.inventory.EquipmentSlot.entries.mapNotNull {
                        runCatching {
                            it to i.getItem(it)
                        }.getOrDefault(null)
                    }.map { (type, item) ->
                        Pair.of(when (type) {
                            HAND -> EquipmentSlot.MAINHAND
                            OFF_HAND -> EquipmentSlot.OFFHAND
                            FEET -> EquipmentSlot.FEET
                            LEGS -> EquipmentSlot.LEGS
                            CHEST -> EquipmentSlot.CHEST
                            HEAD -> EquipmentSlot.HEAD
                        }, CraftItemStack.asNMSCopy(item))
                    }))
                }
            }
            send(ClientboundSetPassengersPacket(handle))
        }

        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            when (msg) {
                is ClientboundAddEntityPacket -> {
                    msg.id.toEntity()?.let { e ->
                        Bukkit.getRegionScheduler().run(BetterModel.inst(), e.bukkitEntity.location) {
                            EntityTracker.tracker(e.bukkitEntity)?.spawn(player)
                        }
                    }
                }
                is ClientboundTeleportEntityPacket -> {
                    msg.id.toTracker()?.let {
                        if (it.world() == player.world.uid) {
                            PacketBundlerImpl(mutableListOf()).run {
                                mount(it, this)
                                send(player)
                            }
                        } else {
                            it.remove()
                        }
                    }
                }
                is ClientboundRemoveEntitiesPacket -> {
                    msg.entityIds
                        .asSequence()
                        .mapNotNull {
                            it.toTracker()
                        }
                        .forEach {
                            it.remove()
                        }
                }
                is ClientboundSetEntityDataPacket -> if (msg.id.toTracker() != null) return
                is ClientboundSetEquipmentPacket -> if (msg.entity.toTracker() != null) return
            }
            super.write(ctx, msg, promise)
        }

        private fun EntityTracker.remove() {
            entityUUIDMap.remove(uuid())
            remove(player)
        }
    }

    override fun mount(tracker: EntityTracker, bundler: PacketBundler) {
        val entity = (tracker.entity as CraftEntity).handle
        val map = tracker.renderers().mapNotNull {
            (it as? ModelDisplayImpl)?.display
        }
        entity.passengers = ImmutableList.builder<Entity>()
            .addAll(map)
            .addAll(entity.passengers.filter { e ->
                e.valid
            })
            .build()
        val packet = ClientboundSetPassengersPacket(entity)
        (bundler as PacketBundlerImpl).run {
            entity.toVoid(this)
            add(packet)
        }
    }

    override fun inject(player: Player): PlayerChannelHandlerImpl = PlayerChannelHandlerImpl(player)

    override fun createBundler(): PacketBundler = PacketBundlerImpl(mutableListOf())
    private fun PacketBundler.unwrap(): PacketBundlerImpl = this as PacketBundlerImpl

    override fun create(location: Location): ModelDisplay = ModelDisplayImpl(ItemDisplay(EntityType.ITEM_DISPLAY, (location.world as CraftWorld).handle).apply {
        billboardConstraints = Display.BillboardConstraints.FIXED
        moveTo(
            location.x,
            location.y,
            location.z,
            0F,
            0F
        )
        valid = true
        persist = false
        itemTransform = ItemDisplayContext.FIXED
        transformationInterpolationDelay = -1
        entityData.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 1)
    })

    private inner class ModelDisplayImpl(
        val display: ItemDisplay
    ) : ModelDisplay {
        override fun close() {
            display.valid = false
        }

        override fun spawn(bundler: PacketBundler) {
            bundler.unwrap().add(addPacket)
            val f = display.transformationInterpolationDuration
            frame(0)
            bundler.unwrap().add(ClientboundSetEntityDataPacket(display.id, display.entityData.nonDefaultValues!!))
            frame(f)
        }

        override fun frame(frame: Int) {
            display.transformationInterpolationDuration = frame
        }

        override fun remove(bundler: PacketBundler) {
            bundler.unwrap().add(removePacket)
        }

        override fun display(transform: org.bukkit.entity.ItemDisplay.ItemDisplayTransform) {
            display.itemTransform = when (transform) {
                NONE -> ItemDisplayContext.NONE
                THIRDPERSON_LEFTHAND -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                THIRDPERSON_RIGHTHAND -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                FIRSTPERSON_LEFTHAND -> ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                FIRSTPERSON_RIGHTHAND -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                org.bukkit.entity.ItemDisplay.ItemDisplayTransform.HEAD -> ItemDisplayContext.HEAD
                GUI -> ItemDisplayContext.GUI
                GROUND -> ItemDisplayContext.GROUND
                FIXED -> ItemDisplayContext.FIXED
            }
        }

        override fun teleport(location: Location, bundler: PacketBundler) {
            display.moveTo(
                location.x,
                location.y,
                location.z,
                location.yaw,
                0F
            )
            bundler.unwrap().add(ClientboundTeleportEntityPacket(display))
        }

        private var itemChanged = false

        override fun item(itemStack: ItemStack) {
            itemChanged = true
            display.itemStack = CraftItemStack.asNMSCopy(itemStack)
        }

        override fun transform(transformation: Transformation) {
            display.setTransformation(com.mojang.math.Transformation(
                transformation.translation,
                transformation.leftRotation,
                transformation.scale,
                transformation.rightRotation
            ))
        }

        override fun send(bundler: PacketBundler) {
            bundler.unwrap().add(dataPacket)
        }

        private val dataPacket
            get(): ClientboundSetEntityDataPacket {
                val set = if (itemChanged) transformSetWithItem else transformSet
                val list = arrayListOf<SynchedEntityData.DataValue<*>>()
                display.entityData.packDirty()?.let(list::addAll)
                display.entityData.nonDefaultValues?.let(list::addAll)
                val result = ClientboundSetEntityDataPacket(display.id, list.distinctBy {
                    it.id
                }.filter {
                    set.contains(it.id)
                })
                itemChanged = false
                return result
            }

        private val removePacket
            get() = ClientboundRemoveEntitiesPacket(display.id)

        private val addPacket
            get() = ClientboundAddEntityPacket(
                display.id,
                display.uuid,
                display.x,
                display.y,
                display.z,
                display.xRot,
                display.yRot,
                display.type,
                0,
                display.deltaMovement,
                display.yHeadRot.toDouble()
            )
    }

    override fun tint(itemStack: ItemStack, toggle: Boolean): ItemStack {
        val meta = itemStack.itemMeta
        if (meta is LeatherArmorMeta) {
            itemStack.itemMeta = meta.apply {
                setColor(if (toggle) Color.fromRGB(0xFF8060) else Color.WHITE)
            }
        }
        return itemStack
    }

    override fun createHitBox(entity: org.bukkit.entity.Entity, supplier: TransformSupplier, namedBoundingBox: NamedBoundingBox, listener: HitBoxListener): HitBox {
        val handle = (entity as CraftLivingEntity).handle
        val newBox = namedBoundingBox.center()
        val height = newBox.length() / 2
        return HitBoxImpl(
            namedBoundingBox.name,
            newBox,
            supplier,
            listener,
            handle
        ).apply {
            entityData.registrationLocked = false
            entityData.define(slimeSize, 1)
            entityData.set(slimeSize, (height / 0.52).roundToInt(), true)
            refreshDimensions()
            handle.level().addFreshEntity(this)
        }
    }

    override fun version(): NMSVersion = NMSVersion.V1_20_R3

    override fun adapt(entity: org.bukkit.entity.LivingEntity): EntityAdapter {
        val handle = (entity as CraftLivingEntity).handle
        return object : EntityAdapter {
            override fun onWalk(): Boolean {
                val delta = handle.deltaMovement.length()
                val attribute = handle.attributes
                return if (handle.onGround) delta / (attribute.getInstance(Attributes.MOVEMENT_SPEED)?.value ?: 0.7) > 0.4
                else delta / (attribute.getInstance(Attributes.FLYING_SPEED)?.value ?: 0.4) > 0.1
            }

            override fun scale(): Double {
                return 1.0
            }

            override fun bodyYaw(): Float {
                return handle.visualRotationYInDegrees
            }

            override fun yaw(): Float {
                return handle.bukkitYaw
            }

            override fun passengerPosition(): Vector3f {
                return handle.passengerPosition()
            }
        }
    }

    override fun isSlim(player: Player): Boolean {
        val encodedValue = (player as CraftPlayer).handle.gameProfile.properties.get("textures").iterator().next().value
        val decodedValue = String(Base64.getDecoder().decode(encodedValue))
        val json = JsonParser.parseString(decodedValue).asJsonObject
        val skinObject = json.getAsJsonObject("textures").getAsJsonObject("SKIN")
        if(!skinObject.has("metadata")) return false
        if(!skinObject.getAsJsonObject("metadata").has("model")) return false
        val model = skinObject.getAsJsonObject("metadata").get("model").asString
        return model == "slim"
    }
}
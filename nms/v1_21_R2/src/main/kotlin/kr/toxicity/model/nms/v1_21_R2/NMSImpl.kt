package kr.toxicity.model.nms.v1_21_R2

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup
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
import kr.toxicity.model.api.tracker.ModelRotation
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.level.entity.LevelEntityGetterAdapter
import net.minecraft.world.level.entity.PersistentEntitySectionManager
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform.FIRSTPERSON_LEFTHAND
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform.FIRSTPERSON_RIGHTHAND
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform.FIXED
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform.GROUND
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform.GUI
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform.NONE
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.util.Transformation
import org.joml.Vector3f
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NMSImpl : NMS {

    companion object {
        private const val INJECT_NAME = "bettermodel_channel_handler"

        //Spigot
        private val getConnection: (ServerCommonPacketListenerImpl) -> Connection = if (BetterModel.IS_PAPER) {
            {
                it.connection
            }
        } else {
            ServerCommonPacketListenerImpl::class.java.declaredFields.first { f ->
                f.type == Connection::class.java
            }.apply {
                isAccessible = true
            }.let { get ->
                {
                    get[it] as Connection
                }
            }
        }
        private val entityTracker = ServerLevel::class.java.fields.firstOrNull {
            it.type == PersistentEntitySectionManager::class.java
        }?.apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        private val ServerLevel.levelGetter
            get(): LevelEntityGetter<Entity> {
                return if (BetterModel.IS_PAPER) {
                    `moonrise$getEntityLookup`()
                } else {
                    entityTracker?.get(this)?.let {
                        (it as PersistentEntitySectionManager<*>).entityGetter as LevelEntityGetter<Entity>
                    } ?: throw RuntimeException("LevelEntityGetter")
                }
            }
        private val getEntityById: (LevelEntityGetter<Entity>, Int) -> Entity? = if (BetterModel.IS_PAPER)  { g, i ->
            (g as EntityLookup)[i]
        } else LevelEntityGetterAdapter::class.java.declaredFields.first {
            net.minecraft.world.level.entity.EntityLookup::class.java.isAssignableFrom(it.type)
        }.let {
            it.isAccessible = true
            { e, i ->
                (it[e] as net.minecraft.world.level.entity.EntityLookup<*>).getEntity(i) as? Entity
            }
        }
        private fun Int.toEntity() = MinecraftServer.getServer().allLevels.firstNotNullOfOrNull {
            getEntityById(it.levelGetter, this)
        }
        //Spigot

        private fun Class<*>.serializers() = declaredFields.filter { f ->
            EntityDataAccessor::class.java.isAssignableFrom(f.type)
        }

        private fun Field.toSerializerId() = run {
            isAccessible = true
            (get(null) as EntityDataAccessor<*>).id
        }

        private val sharedFlag = Entity::class.java.serializers().first().toSerializerId()
        private val itemId = ItemDisplay::class.java.serializers().first().toSerializerId()
        private val transformSet = Display::class.java.serializers().subList(0, 6).map { e ->
            e.toSerializerId()
        }.toSet() + itemId + sharedFlag
    }

    private class PacketBundlerImpl(
        private val list: MutableList<Packet<in ClientGamePacketListener>>
    ) : PacketBundler, MutableList<Packet<in ClientGamePacketListener>> by list {
        override fun copy(): PacketBundler = PacketBundlerImpl(ArrayList(list))
        override fun send(player: Player) {
            if (isNotEmpty()) (player as CraftPlayer).handle.connection.send(ClientboundBundlePacket(this))
        }
    }

    override fun hide(player: Player, entity: org.bukkit.entity.Entity) {
        val handle = (entity as CraftEntity).handle
        val inv = handle.isInvisible
        handle.isInvisible = true
        (player as CraftPlayer).handle.connection.send(ClientboundBundlePacket(listOf(
            ClientboundSetEquipmentPacket(handle.id, EquipmentSlot.entries.map { e ->
                Pair.of(e, Items.AIR.defaultInstance)
            }),
            ClientboundSetEntityDataPacket(
                handle.id,
                handle.entityData.pack()
            ))
        ))
        handle.isInvisible = inv
    }

    inner class PlayerChannelHandlerImpl(
        private val player: Player
    ) : PlayerChannelHandler, ChannelDuplexHandler() {
        private val connection = (player as CraftPlayer).handle.connection
        private val entityUUIDMap = ConcurrentHashMap<UUID, EntityTracker>()

        init {
            val pipeLine = getConnection(connection).channel.pipeline()
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

        private fun Int.toTracker() = toEntity()?.let {
            entityUUIDMap[it.uuid]
        }

        override fun player(): Player = player
        private fun send(packet: Packet<*>) = connection.send(packet)

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
            send(ClientboundSetEntityDataPacket(handle.id, handle.entityData.pack()))
            if (e is LivingEntity) {
                e.equipment?.let { i ->
                    send(ClientboundSetEquipmentPacket(handle.id, org.bukkit.inventory.EquipmentSlot.entries.mapNotNull {
                        it to (runCatching {
                            i.getItem(it)
                        }.getOrNull() ?: return@mapNotNull null)
                    }.map { (type, item) ->
                        Pair.of(when (type) {
                            HAND -> EquipmentSlot.MAINHAND
                            OFF_HAND -> EquipmentSlot.OFFHAND
                            FEET -> EquipmentSlot.FEET
                            LEGS -> EquipmentSlot.LEGS
                            CHEST -> EquipmentSlot.CHEST
                            HEAD -> EquipmentSlot.HEAD
                            BODY -> EquipmentSlot.BODY
                        }, CraftItemStack.asNMSCopy(item))
                    }))
                }
            }
            send(ClientboundSetPassengersPacket(handle))
        }

        private fun <T : ClientGamePacketListener> Packet<in T>.handle(): Packet<in T>? {
            when (this) {
                is ClientboundBundlePacket -> return ClientboundBundlePacket(subPackets().mapNotNull {
                    it.handle()
                })
                is ClientboundAddEntityPacket -> {
                    id.toEntity()?.let { e ->
                        if (entityUUIDMap[e.uuid] != null) return this
                        BetterModel.inst().scheduler().task(e.bukkitEntity.location) {
                            EntityTracker.tracker(e.bukkitEntity)?.let {
                                if (it.autoSpawn()) it.spawn(player)
                            }
                        }
                    }
                }
                is ClientboundTeleportEntityPacket -> {
                    id.toTracker()?.let {
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
                    entityIds
                        .asSequence()
                        .mapNotNull {
                            it.toTracker()
                        }
                        .forEach {
                            it.remove()
                        }
                }
                is ClientboundSetEntityDataPacket -> if (id.toTracker() != null) return ClientboundSetEntityDataPacket(id, packedItems().map {
                    if (it.id == sharedFlag) SynchedEntityData.DataValue<Byte>(
                        it.id,
                        EntityDataSerializers.BYTE,
                        ((it.value() as Byte).toInt() or (1 shl 5) and (1 shl 6).inv()).toByte()
                    ) else it
                })
                is ClientboundSetEquipmentPacket -> if (entity.toTracker() != null) return null
            }
            return this
        }

        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            if (msg is Packet<*>) {
                super.write(ctx, msg.handle() ?: return, promise)
                return
            }
            super.write(ctx, msg, promise)
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            when (msg) {
                is ServerboundSetCarriedItemPacket -> {
                    if (connection.player.id.toTracker() != null) {
                        connection.send(ClientboundSetHeldSlotPacket(player.inventory.heldItemSlot))
                        return
                    }
                }
                is ServerboundPlayerActionPacket -> {
                    if (connection.player.id.toTracker() != null) return
                }
            }
            super.channelRead(ctx, msg)
        }

        private fun EntityTracker.remove() {
            remove(player)
        }
    }

    override fun mount(tracker: EntityTracker, bundler: PacketBundler) {
        val entity = (tracker.entity as CraftEntity).handle
        val map = tracker.displays().mapNotNull {
            (it as? ModelDisplayImpl)?.display
        }
        entity.passengers = ImmutableList.builder<Entity>()
            .addAll(map)
            .addAll(entity.passengers.filter { e ->
                e.valid
            })
            .build()
        val packet = ClientboundSetPassengersPacket(entity)
        (bundler as PacketBundlerImpl).add(packet)
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
            location.yaw,
            0F
        )
        valid = true
        persist = false
        itemTransform = ItemDisplayContext.FIXED
        transformationInterpolationDelay = -1
        entityData.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 3)
    })

    private inner class ModelDisplayImpl(
        val display: ItemDisplay
    ) : ModelDisplay {

        override fun rotate(rotation: ModelRotation, bundler: PacketBundler) {
            display.xRot = rotation.x
            display.yRot = rotation.y
            bundler.unwrap().add(ClientboundMoveEntityPacket.Rot(
                display.id,
                rotation.y.packDegree(),
                rotation.x.packDegree(),
                display.onGround
            ))
        }

        override fun sync(entity: EntityAdapter) {
            display.setGlowingTag(entity.glow())
            if (BetterModel.inst().configManager().followMobInvisibility()) display.isInvisible = entity.invisible()
        }

        override fun close() {
            display.valid = false
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

        override fun spawn(bundler: PacketBundler) {
            bundler.unwrap().add(addPacket)
            val f = display.transformationInterpolationDuration
            frame(0)
            bundler.unwrap().add(ClientboundSetEntityDataPacket(display.id, display.entityData.pack()))
            frame(f)
        }

        override fun frame(frame: Int) {
            display.transformationInterpolationDuration = frame
        }

        override fun remove(bundler: PacketBundler) {
            bundler.unwrap().add(removePacket)
        }

        override fun teleport(location: Location, bundler: PacketBundler) {
            display.moveTo(
                location.x,
                location.y,
                location.z,
                location.yaw,
                0F
            )
            bundler.unwrap().add(ClientboundTeleportEntityPacket.teleport(display.id, PositionMoveRotation.of(display), emptySet(), display.onGround))
        }
        override fun item(itemStack: ItemStack) {
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
            if (display.isInvisible) {
                val item = display.itemStack
                display.itemStack = Items.AIR.defaultInstance
                bundler.unwrap().add(dataPacket)
                display.itemStack = item
            } else {
                bundler.unwrap().add(dataPacket)
            }
        }

        private val dataPacket
            get(): ClientboundSetEntityDataPacket {
                val result = ClientboundSetEntityDataPacket(display.id, display.entityData.pack().filter {
                    transformSet.contains(it.id)
                })
                return result
            }

        private val removePacket
            get() = ClientboundRemoveEntitiesPacket(display.id)

        private val addPacket
            get() = display.addPacket
    }

    private val Entity.addPacket
        get() = ClientboundAddEntityPacket(
            id,
            uuid,
            x,
            y,
            z,
            xRot,
            yRot,
            type,
            0,
            deltaMovement,
            yHeadRot.toDouble()
        )

    override fun tint(itemStack: ItemStack, toggle: Boolean): ItemStack {
        val meta = itemStack.itemMeta
        if (meta is LeatherArmorMeta) {
            itemStack.itemMeta = meta.apply {
                setColor(if (toggle) Color.fromRGB(0xFF8060) else Color.WHITE)
            }
        }
        return itemStack
    }

    override fun createHitBox(entity: EntityAdapter, supplier: TransformSupplier, namedBoundingBox: NamedBoundingBox, listener: HitBoxListener): HitBox? {
        val handle = (entity.entity() as? CraftLivingEntity)?.handle ?: return null
        val scale = entity.scale()
        val newBox = namedBoundingBox.center() * scale
        val height = newBox.length() / 2
        return HitBoxImpl(
            namedBoundingBox.name,
            newBox,
            supplier,
            listener,
            handle,
            entity
        ).apply {
            attributes.getInstance(Attributes.SCALE)!!.baseValue = height / 0.52
            refreshDimensions()
            handle.level().addFreshEntity(this)
        }
    }

    override fun version(): NMSVersion = NMSVersion.V1_21_R2

    override fun adapt(entity: LivingEntity): EntityAdapter {
        val handle = (entity as CraftLivingEntity).handle
        return object : EntityAdapter {
            override fun entity(): LivingEntity = entity
            override fun invisible(): Boolean = handle.isInvisible || handle.hasEffect(MobEffects.INVISIBILITY)
            override fun glow(): Boolean = handle.isCurrentlyGlowing

            override fun onWalk(): Boolean {
                val delta = handle.deltaMovement.length()
                val attribute = handle.attributes
                return if (handle.onGround) delta / (attribute.getInstance(Attributes.MOVEMENT_SPEED)?.value ?: 0.7) > 0.4
                else delta / (attribute.getInstance(Attributes.FLYING_SPEED)?.value ?: 0.4) > 0.1
            }

            override fun scale(): Double {
                return handle.attributes.getInstance(Attributes.SCALE)?.value ?: 1.0
            }

            override fun pitch(): Float {
                return handle.xRot
            }

            override fun bodyYaw(): Float {
                return handle.visualRotationYInDegrees
            }

            override fun yaw(): Float {
                return handle.bukkitYaw
            }
            
            override fun passengerPosition(): Vector3f {
                return handle.passengerPosition(scale())
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
package kr.toxicity.model.nms.v1_21_R3

import com.google.common.collect.ImmutableList
import com.mojang.datafixers.util.Pair
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kr.toxicity.model.api.ModelRenderer
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox
import kr.toxicity.model.api.nms.*
import kr.toxicity.model.api.tracker.EntityTracker
import net.minecraft.core.component.DataComponents
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.DyedItemColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.Vector3f
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NMSImpl : NMS {

    companion object {
        private const val INJECT_NAME = "betterengine_channel_handler"
        private val hitBoxMap = ConcurrentHashMap<Int, HitBoxImpl>()

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
        private val list: MutableList<Packet<in ClientGamePacketListener>>
    ) : PacketBundler, MutableList<Packet<in ClientGamePacketListener>> by list {
        override fun send(player: Player) {
            if (isNotEmpty()) (player as CraftPlayer).handle.connection.send(ClientboundBundlePacket(this))
        }
    }

    private fun Int.toEntity() = MinecraftServer.getServer().allLevels.firstNotNullOfOrNull {
        it.`moonrise$getEntityLookup`().get(this)
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
            val handle = (tracker.entity as CraftEntity).handle
            entityUUIDMap.remove(handle.uuid)
            send(ClientboundSetPassengersPacket(handle))
        }

        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            when (msg) {
                is ClientboundAddEntityPacket -> {
                    msg.id.toEntity()?.let { e ->
                        Bukkit.getRegionScheduler().run(ModelRenderer.inst(), e.bukkitEntity.location) {
                            EntityTracker.tracker(e.bukkitEntity)?.spawn(player)
                        }
                    }
                }
                is ClientboundTeleportEntityPacket -> {
                    msg.id.toTracker()?.let {
                        if (it.entity.world.uid == player.world.uid) {
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
                map.none { it.uuid == e.uuid }
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
        persist = false
        itemTransform = ItemDisplayContext.FIXED
        transformationInterpolationDelay = -1
        entityData.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 1)
    })

    private inner class ModelDisplayImpl(
        val display: ItemDisplay
    ) : ModelDisplay {

        override fun spawn(bundler: PacketBundler) {
            bundler.unwrap().add(addPacket)
            val f = display.transformationInterpolationDuration
            frame(0)
            bundler.unwrap().add(ClientboundSetEntityDataPacket(display.id, display.entityData.packAll()!!))
            frame(f)
        }

        override fun frame(frame: Int) {
            display.transformationInterpolationDuration = frame
        }

        override fun remove(bundler: PacketBundler) {
            bundler.unwrap().add(removePacket)
        }

        override fun teleport(location: Location) {
            display.moveTo(
                location.x,
                location.y,
                location.z,
                location.yaw,
                0F
            )
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
                val result = ClientboundSetEntityDataPacket(display.id, display.entityData.packAll()!!.filter {
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
        return CraftItemStack.asBukkitCopy(CraftItemStack.asNMSCopy(itemStack).apply {
            set(DataComponents.DYED_COLOR, if (toggle) DyedItemColor(0xFF8060, false) else DyedItemColor(0xFFFFFF, false))
            set(DataComponents.CUSTOM_MODEL_DATA, get(DataComponents.CUSTOM_MODEL_DATA)?.let {
                CustomModelData(it.floats, it.flags, it.strings, if (toggle) listOf(0xFF8060) else listOf(0xFFFFFF))
            })
        })
    }

    override fun createHitBox(entity: org.bukkit.entity.Entity, supplier: TransformSupplier, namedBoundingBox: NamedBoundingBox, listener: HitBoxListener): HitBox {
        val handle = (entity as CraftLivingEntity).handle
        val scale = adapt(entity).scale()
        val newBox = namedBoundingBox.center() * scale
        val height = newBox.length() / 2
        return HitBoxImpl(
            namedBoundingBox.name,
            newBox,
            supplier,
            listener,
            handle
        ) {
            hitBoxMap.remove(it.id)
        }.apply {
            hitBoxMap[id] = this
            attributes.getInstance(Attributes.SCALE)!!.baseValue = height / 0.52
            refreshDimensions()
            handle.level().addFreshEntity(this)
        }
    }
    override fun version(): NMSVersion = NMSVersion.V1_21_R3

    override fun passengerPosition(entity: org.bukkit.entity.Entity): Vector3f {
        return (entity as CraftEntity).handle.passengerPosition()
    }

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
                return handle.attributes.getInstance(Attributes.SCALE)?.value ?: 1.0
            }

            override fun bodyYaw(): Float {
                return handle.visualRotationYInDegrees
            }

            override fun yaw(): Float {
                return handle.bukkitYaw
            }
        }
    }
}
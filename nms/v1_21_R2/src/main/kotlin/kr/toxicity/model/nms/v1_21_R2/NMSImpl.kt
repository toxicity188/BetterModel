package kr.toxicity.model.nms.v1_21_R2

import com.google.common.collect.ImmutableList
import com.mojang.datafixers.util.Pair
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kr.toxicity.model.api.nms.*
import kr.toxicity.model.api.tracker.EntityTracker
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.*
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.util.BoundingBox
import org.bukkit.util.Transformation
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NMSImpl : NMS {

    companion object {
        private const val INJECT_NAME = "betterengine_channel_handler"
    }

    private class PacketBundlerImpl(
        private val list: MutableList<Packet<in ClientGamePacketListener>>
    ) : PacketBundler, MutableList<Packet<in ClientGamePacketListener>> by list {
        override fun send(player: Player) {
            if (isNotEmpty()) (player as CraftPlayer).handle.connection.send(ClientboundBundlePacket(list))
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
            val handle = (tracker.entity as CraftEntity).handle
            entityUUIDMap.remove(handle.uuid)
            send(ClientboundSetPassengersPacket(handle))
        }

        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            when (msg) {
                is ClientboundAddEntityPacket -> {
                    msg.id.toEntity()?.let {
                        EntityTracker.tracker(it.bukkitEntity)?.spawn(player)
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
            val bundle = PacketBundlerImpl(mutableListOf())
            renderers().forEach {
                it.remove(bundle)
            }
            bundle.send(player)
        }
    }

    override fun mount(tracker: EntityTracker, bundler: PacketBundler) {
        val entity = (tracker.entity as CraftEntity).handle
        val p = entity.passengers
        entity.passengers = ImmutableList.builder<Entity>()
            .addAll(tracker.renderers().mapNotNull {
                (it as? ModelDisplayImpl)?.display
            })
            .addAll(p)
            .build()
        val packet = ClientboundSetPassengersPacket(entity)
        entity.passengers = p
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
            bundler.unwrap().add(dataPacket)
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

        override fun item(itemStack: ItemStack) {
            display.itemStack = CraftItemStack.asNMSCopy(itemStack)
        }

        override fun transform(transformation: Transformation) {
            display.setTransformation(
                com.mojang.math.Transformation(
                    transformation.translation,
                    transformation.leftRotation,
                    transformation.scale,
                    transformation.rightRotation
                )
            )
        }

        override fun send(bundler: PacketBundler) {
            bundler.unwrap().add(dataPacket)
        }

        private val dataPacket
            get() = ClientboundSetEntityDataPacket(display.id, display.entityData.nonDefaultValues!!)

        private val teleportPacket
            get() = ClientboundTeleportEntityPacket.teleport(display.id, PositionMoveRotation.of(display), emptySet(), display.onGround)

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

    override fun boundingBox(entity: org.bukkit.entity.Entity, box: BoundingBox) {
        (entity as CraftEntity).handle.boundingBox = AABB(
            box.minX,
            box.minY,
            box.minZ,
            box.maxX,
            box.maxY,
            box.maxZ
        )
    }

    override fun version(): NMSVersion = NMSVersion.V1_21_R2
}
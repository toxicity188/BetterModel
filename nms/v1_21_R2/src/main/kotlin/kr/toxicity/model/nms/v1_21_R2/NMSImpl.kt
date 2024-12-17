package kr.toxicity.model.nms.v1_21_R2

import kr.toxicity.model.api.nms.ModelDisplay
import kr.toxicity.model.api.nms.NMS
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.item.ItemDisplayContext
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NMSImpl : NMS {
    override fun create(location: Location): ModelDisplay {
        val display = ItemDisplay(EntityType.ITEM_DISPLAY, (location.world as CraftWorld).handle).apply {
            billboardConstraints = Display.BillboardConstraints.FIXED
            moveTo(
                location.x,
                location.y,
                location.z,
                location.yaw,
                0F
            )
            itemTransform = ItemDisplayContext.FIXED
            transformationInterpolationDelay = -1
            entityData.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 1)
        }
        return object : ModelDisplay {

            private val connectionMap = ConcurrentHashMap<UUID, ServerGamePacketListenerImpl>()

            override fun spawn(player: Player) {
                connectionMap.computeIfAbsent(player.uniqueId) {
                    (player as CraftPlayer).handle.connection.apply {
                        send(addPacket)
                        send(dataPacket)
                    }
                }
            }

            override fun frame(frame: Int) {
                display.transformationInterpolationDuration = frame
            }

            override fun remove(player: Player) {
                connectionMap.remove(player.uniqueId)?.send(removePacket)
            }

            override fun remove() {
                connectionMap.values.forEach { listener ->
                    listener.send(removePacket)
                }
                connectionMap.clear()
            }

            override fun teleport(location: Location) {
                display.moveTo(
                    location.x,
                    location.y,
                    location.z,
                    location.yaw,
                    0F
                )
                connectionMap.values.forEach {
                    it.send(teleportPacket)
                }
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
                connectionMap.values.forEach {
                    it.send(dataPacket)
                }
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
    }
}
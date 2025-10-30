/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.nms.v1_20_R4

import kr.toxicity.model.api.entity.BaseEntity
import kr.toxicity.model.api.nms.DisplayTransformer
import kr.toxicity.model.api.nms.ModelDisplay
import kr.toxicity.model.api.nms.PacketBundler
import kr.toxicity.model.api.tracker.ModelRotation
import kr.toxicity.model.api.util.lock.SingleLock
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

internal class ModelDisplayImpl(
    val display: ItemDisplay,
    val yOffset: Double
) : ModelDisplay {

    private val entityData = display.entityData
    private val entityDataLock = SingleLock()
    private val forceGlow = AtomicBoolean()
    private val forceInvisibility = AtomicBoolean()

    override fun id(): Int = display.id
    override fun uuid(): UUID = display.uuid
    override fun rotate(rotation: ModelRotation, bundler: PacketBundler) {
        if (!display.valid) return
        display.xRot = rotation.x
        display.yRot = rotation.y
        bundler += ClientboundMoveEntityPacket.Rot(
            display.id,
            rotation.packedY(),
            rotation.packedX(),
            display.onGround
        )
    }

    override fun invisible(invisible: Boolean) {
        if (forceInvisibility.compareAndSet(!invisible, invisible)) {
            entityDataLock.accessToLock {
                entityData.markDirty(ITEM_SERIALIZER)
            }
        }
    }

    override fun syncEntity(entity: BaseEntity) {
        display.valid = !entity.dead()
        display.onGround = entity.ground()
        val beforeInvisible = display.isInvisible
        val afterInvisible = entity.invisible()
        entityDataLock.accessToLock {
            display.setGlowingTag(entity.glow() || forceGlow.get())
            if (CONFIG.followMobInvisibility() && beforeInvisible != afterInvisible) {
                display.isInvisible = afterInvisible
                entityData.markDirty(ITEM_SERIALIZER)
            }
        }
    }

    override fun syncPosition(location: Location) {
        display.setOldPosAndRot()
        display.setPos(Vec3(location.x, location.y, location.z))
    }


    override fun spawn(showItem: Boolean, bundler: PacketBundler) {
        bundler += addPacket
    }

    override fun remove(bundler: PacketBundler) {
        bundler += removePacket
    }

    override fun teleport(location: Location, bundler: PacketBundler) {
        display.moveTo(
            location.x,
            location.y,
            location.z,
            location.yaw,
            0F
        )
        bundler += ClientboundTeleportEntityPacket(display)
    }

    override fun sendPosition(adapter: BaseEntity, bundler: PacketBundler) {
        val handle = adapter.handle() as Entity
        if (display.position() == Vec3(handle.xOld, handle.yOld, handle.zOld)) return
        bundler += ClientboundTeleportEntityPacket(display)
    }

    override fun display(transform: org.bukkit.entity.ItemDisplay.ItemDisplayTransform) {
        entityDataLock.accessToLock {
            display.itemTransform = ItemDisplayContext.BY_ID.apply(transform.ordinal)
        }
    }

    override fun moveDuration(duration: Int) {
        entityDataLock.accessToLock {
            entityData[Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID] = duration
        }
    }

    override fun item(itemStack: ItemStack) {
        entityDataLock.accessToLock {
            display.itemStack = itemStack.asVanilla()
        }
    }

    override fun brightness(block: Int, sky: Int) {
        entityDataLock.accessToLock {
            display.brightnessOverride = if (block < 0 && sky < 0) null else Brightness(
                block,
                sky
            )
        }
    }

    override fun viewRange(range: Float) {
        entityDataLock.accessToLock {
            display.viewRange = range
        }
    }

    override fun shadowRadius(radius: Float) {
        entityDataLock.accessToLock {
            display.shadowRadius = radius
        }
    }

    override fun glow(glow: Boolean) {
        if (!forceGlow.compareAndSet(!glow, glow)) return
        entityDataLock.accessToLock {
            display.setGlowingTag(display.isCurrentlyGlowing || glow)
        }
    }

    override fun glowColor(glowColor: Int) {
        entityDataLock.accessToLock {
            display.glowColorOverride = glowColor
        }
    }

    override fun billboard(billboard: org.bukkit.entity.Display.Billboard) {
        entityDataLock.accessToLock {
            display.billboardConstraints = Display.BillboardConstraints.BY_ID.apply(billboard.ordinal)
        }
    }

    override fun createTransformer(): DisplayTransformer = DisplayTransformerImpl(display)

    override fun invisible(): Boolean = entityDataLock.accessToLock {
        display.isInvisible || forceInvisibility.get() || display.itemStack.`is`(Items.AIR)
    }

    override fun sendDirtyEntityData(bundler: PacketBundler) {
        entityDataLock.accessToLock {
            entityData.pack(
                clean = true,
                itemFilter = { it.isDirty },
                valueFilter = { ITEM_ENTITY_DATA.contains(it.id) }
            )
        }?.markVisible(!invisible())?.run {
            bundler += ClientboundSetEntityDataPacket(display.id, this)
        }
    }

    override fun sendEntityData(showItem: Boolean, bundler: PacketBundler) {
        entityDataLock.accessToLock {
            entityData.pack(
                valueFilter = { ITEM_ENTITY_DATA.contains(it.id) }
            )
        }?.markVisible(showItem && !invisible())?.run {
            bundler += ClientboundSetEntityDataPacket(display.id, this)
        }
    }

    private fun List<SynchedEntityData.DataValue<*>>.markVisible(showItem: Boolean) = map {
        if (it.id == ITEM_SERIALIZER.id) SynchedEntityData.DataValue(
            it.id,
            EntityDataSerializers.ITEM_STACK,
            if (showItem) display.itemStack else EMPTY_ITEM
        ) else it
    }

    private val addPacket
        get() = ClientboundAddEntityPacket(
            display.id,
            display.uuid,
            display.x,
            display.y + yOffset,
            display.z,
            display.xRot,
            display.yRot,
            display.type,
            0,
            display.deltaMovement,
            display.yHeadRot.toDouble()
        )

    private val removePacket
        get() = ClientboundRemoveEntitiesPacket(display.id)

    private class DisplayTransformerImpl(
        source: ItemDisplay
    ) : DisplayTransformer {
        private val id = source.id
        private val entityData = TransformationData()
        private val entityDataLock = SingleLock()

        override fun transform(
            duration: Int,
            position: Vector3f,
            scale: Vector3f,
            rotation: Quaternionf,
            bundler: PacketBundler
        ) {
            entityDataLock.accessToLock {
                entityData.transform(
                    duration,
                    position,
                    scale,
                    rotation
                )
                entityData.packDirty()
            }?.run {
                bundler += ClientboundSetEntityDataPacket(id, this)
            }
        }

        override fun sendTransformation(bundler: PacketBundler) {
            entityDataLock.accessToLock {
                entityData.pack()
            }?.run {
                bundler += ClientboundSetEntityDataPacket(id, this)
            }
        }
    }
}
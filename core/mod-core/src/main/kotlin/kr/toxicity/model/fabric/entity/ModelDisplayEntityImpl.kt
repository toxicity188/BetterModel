/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.entity

import kr.toxicity.model.api.entity.BaseEntity
import kr.toxicity.model.api.nms.DisplayTransformer
import kr.toxicity.model.api.nms.ModelDisplay
import kr.toxicity.model.api.nms.PacketBundler
import kr.toxicity.model.api.platform.PlatformBillboard
import kr.toxicity.model.api.platform.PlatformItemStack
import kr.toxicity.model.api.platform.PlatformItemTransform
import kr.toxicity.model.api.platform.PlatformLocation
import kr.toxicity.model.api.tracker.ModelRotation
import kr.toxicity.model.api.util.lock.SingleLock
import kr.toxicity.model.fabric.manager.markDirty
import kr.toxicity.model.fabric.mixin.DisplayAccessor
import kr.toxicity.model.fabric.mixin.EntityAccessor
import kr.toxicity.model.fabric.mixin.ItemDisplayAccessor
import kr.toxicity.model.fabric.network.pack
import kr.toxicity.model.fabric.network.plusAssign
import kr.toxicity.model.fabric.unwarp
import kr.toxicity.model.util.CONFIG
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ModelDisplayEntityImpl(
    val display: Display.ItemDisplay,
    val yOffset: Double
) :
    ModelDisplay {
    private val entityData: SynchedEntityData = display.entityData
    private val entityDataLock: SingleLock = SingleLock()

    private val forceGlow = AtomicBoolean()
    private val forceInvisibility = AtomicBoolean()

    override fun id(): Int = display.id

    override fun uuid(): UUID = display.uuid

    override fun rotate(rotation: ModelRotation, bundler: PacketBundler) {
        display.xRot = rotation.x
        display.yRot = rotation.y
        bundler += ClientboundMoveEntityPacket.Rot(
            display.id,
            rotation.packedY(),
            rotation.packedX(),
            display.onGround()
        )
    }

    override fun invisible(invisible: Boolean) {
        if (forceInvisibility.compareAndSet(!invisible, invisible)) {
            entityData.packDirty()
            entityDataLock.accessToLock {
                entityData.markDirty(ItemDisplayAccessor.getDataItemStackId())
            }
        }
    }

    override fun syncEntity(entity: BaseEntity) {
        display.setOnGround(entity.ground())

        val beforeInvisible = display.isInvisible
        val afterInvisible = entity.invisible()

        entityDataLock.accessToLock {
            display.setGlowingTag(entity.glow() || forceGlow.get())
            if (CONFIG.followMobInvisibility() && beforeInvisible != afterInvisible) {
                display.isInvisible = afterInvisible
                entityData.markDirty(ItemDisplayAccessor.getDataItemStackId())
            }
        }
    }

    override fun syncPosition(location: PlatformLocation) {
        display.setOldPosAndRot()
        display.setPos(Vec3(location.x(), location.y(), location.z()))
    }

    override fun spawn(showItem: Boolean, bundler: PacketBundler) {
        bundler += createAddPacket()
    }

    override fun remove(bundler: PacketBundler) {
        bundler += createRemovePacket()
    }

    override fun teleport(location: PlatformLocation, bundler: PacketBundler) {
        display.snapTo(
            location.x(),
            location.y(),
            location.z(),
            location.yaw(),
            0F
        )

        bundler += ClientboundTeleportEntityPacket.teleport(
            display.id,
            PositionMoveRotation.of(display),
            emptySet(),
            display.onGround()
        )
    }

    override fun sendPosition(adapter: BaseEntity, bundler: PacketBundler) {
        val handle = adapter.handle() as Entity
        if (display.position() == display.oldPosition()) {
            return
        }

        bundler += ClientboundEntityPositionSyncPacket(
            display.id,
            PositionMoveRotation.of(handle),
            handle.onGround()
        )
    }

    override fun display(transform: PlatformItemTransform) {
        entityDataLock.accessToLock {
            display.itemTransform = ItemDisplayContext.BY_ID.apply(transform.ordinal)
        }
    }

    override fun moveDuration(duration: Int) {
        entityDataLock.accessToLock {
            entityData[DisplayAccessor.getDataPosRotInterpolationDurationId()] = duration
        }
    }

    override fun item(itemStack: PlatformItemStack) {
        entityDataLock.accessToLock {
            display.itemStack = itemStack.unwarp()
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

    override fun billboard(billboard: PlatformBillboard) {
        entityDataLock.accessToLock {
            display.billboardConstraints = Display.BillboardConstraints.BY_ID.apply(billboard.ordinal)
        }
    }

    override fun createTransformer(): DisplayTransformer = DisplayTransformerImpl(display)

    override fun invisible(): Boolean {
        return entityDataLock.accessToLock {
            display.isInvisible ||
                forceInvisibility.get() ||
                display.itemStack.`is`(Items.AIR)
        }
    }

    override fun sendDirtyEntityData(bundler: PacketBundler) {
        entityDataLock.accessToLock {
            entityData.pack(
                clean = true,
                itemFilter = { it.isDirty },
                valueFilter = { ACCESSOR_IDS.contains(it.id) }
            )
        }?.markVisible(!invisible())?.run {
            bundler += ClientboundSetEntityDataPacket(display.id, this)
        }
    }

    override fun sendEntityData(showItem: Boolean, bundler: PacketBundler) {
        entityDataLock.accessToLock {
            entityData.pack(
                valueFilter = { ACCESSOR_IDS.contains(it.id) }
            )
        }?.markVisible(showItem && !invisible())?.run {
            bundler += ClientboundSetEntityDataPacket(display.id, this)
        }
    }

    private fun List<SynchedEntityData.DataValue<*>>.markVisible(showItem: Boolean) = map {
        if (it.id == ItemDisplayAccessor.getDataItemStackId().id) SynchedEntityData.DataValue(
            it.id,
            EntityDataSerializers.ITEM_STACK,
            if (showItem) display.itemStack else ItemStack.EMPTY
        ) else it
    }

    private fun createAddPacket() = ClientboundAddEntityPacket(
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

    private fun createRemovePacket() = ClientboundRemoveEntitiesPacket(display.id)

    companion object {
        private val ACCESSOR_IDS by lazy {
            listOf(
                EntityAccessor.getDataSharedFlagsId(),

                DisplayAccessor.getDataPosRotInterpolationDurationId(),

                // index: 7 ~ last
                DisplayAccessor.getDataBillboardRenderConstraintsId(),
                DisplayAccessor.getDataBrightnessOverrideId(),
                DisplayAccessor.getDataViewRangeId(),
                DisplayAccessor.getDataShadowRadiusId(),
                DisplayAccessor.getDataShadowStrengthId(),
                DisplayAccessor.getDataWidthId(),
                DisplayAccessor.getDataHeightId(),
                DisplayAccessor.getDataGlowColorOverrideId(),

                // all
                ItemDisplayAccessor.getDataItemStackId(),
                ItemDisplayAccessor.getDataItemDisplayId()
            ).map { accessor ->
                accessor.id
            }
        }
    }
}

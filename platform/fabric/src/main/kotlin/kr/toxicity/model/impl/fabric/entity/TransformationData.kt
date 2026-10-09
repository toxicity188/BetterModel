/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric.entity

import kr.toxicity.model.api.nms.AnimationBundler
import kr.toxicity.model.api.util.MathUtil
import kr.toxicity.model.impl.fabric.network.plusAssign
import kr.toxicity.model.mixin.DisplayAccessor
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import org.joml.Quaternionf
import org.joml.Vector3f

class TransformationData {
    private var duration = 0

    private val durationDataValue
        get() = SynchedEntityData.DataValue(
            DisplayAccessor.`bettermodel$getDataTransformationInterpolationDurationId`().id,
            DisplayAccessor.`bettermodel$getDataTransformationInterpolationDurationId`().serializer,
            duration
        )

    private val translation = Item(
        Vector3f(),
        DisplayAccessor.`bettermodel$getDataTranslationId`(),
        { a, b ->
            // unchecked cast
            MathUtil.isSimilar(a, b)
        },
        { a, b ->
            // unchecked cast
            (a as Vector3f).set(b)
        }
    )

    private val scale = Item(
        Vector3f(),
        DisplayAccessor.`bettermodel$getDataScaleId`(),
        { a, b ->
            // unchecked cast
            MathUtil.isSimilar(a, b)
        },
        { a, b ->
            // unchecked cast
            (a as Vector3f).set(b)
        }
    )

    private val rotation = Item(
        Quaternionf(),
        DisplayAccessor.`bettermodel$getDataLeftRotationId`(),
        { a, b ->
            // unchecked cast
            MathUtil.isSimilar(a as Quaternionf, b as Quaternionf)
        },
        { a, b ->
            // unchecked cast
            (a as Quaternionf).set(b)
        }
    )

    fun packDirty(entityId: Int, dest: AnimationBundler) {
        val i = translation.cleanIndex + scale.cleanIndex + rotation.cleanIndex
        if (i == 0) return
        dest.standard += ClientboundSetEntityDataPacket(entityId, buildList(i + 2) {
            add(INTERPOLATION_DELAY_VALUE)
            translation.value?.let { add(it) }
            rotation.value?.let { add(it) }
            scale.value?.let { add(it) }
            add(durationDataValue)
        })
    }

    fun transform(
        duration: Int,
        translation: Vector3f,
        scale: Vector3f,
        rotation: Quaternionf
    ) {
        this.duration = duration
        this.translation.set(translation)
        this.scale.set(scale)
        this.rotation.set(rotation)
    }

    fun pack(): List<SynchedEntityData.DataValue<*>> {
        return listOf(
            INTERPOLATION_DELAY_VALUE,
            durationDataValue,
            translation.forceValue,
            scale.forceValue,
            rotation.forceValue
        )
    }

    private class Item<T : Any>(
        initialValue: T,
        private val accessor: EntityDataAccessor<T>,
        private val dirtyChecker: (T, T) -> Boolean,
        private val setter: (T, T) -> Unit
    ) {
        private val _value: T = initialValue
        private var _dirty = false

        val dirty
            get() = _dirty

        val cleanIndex
            get() = if (dirty) 1 else 0

        val value
            get() = if (_dirty) {
                _dirty = false
                forceValue
            } else {
                null
            }

        val forceValue
            get() = SynchedEntityData.DataValue(
                accessor.id,
                accessor.serializer,
                _value
            )

        fun set(other: T) {
            if (dirtyChecker(_value, other)) {
                return
            }

            _dirty = true
            setter(_value, other)
        }
    }

    companion object {
        private val INTERPOLATION_DELAY_VALUE = DisplayAccessor.`bettermodel$getDataTransformationInterpolationStartDeltaTicksId`()
            .let { accessor ->
                SynchedEntityData.DataValue(accessor.id, accessor.serializer, 0)
            }
    }
}

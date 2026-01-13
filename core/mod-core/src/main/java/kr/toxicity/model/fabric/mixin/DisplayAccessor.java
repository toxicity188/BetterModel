/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Display.class)
public interface DisplayAccessor {
    @Accessor("DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID")
    static @NotNull EntityDataAccessor<Integer> getDataTransformationInterpolationStartDeltaTicksId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID")
    static @NotNull EntityDataAccessor<Integer> getDataTransformationInterpolationDurationId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_POS_ROT_INTERPOLATION_DURATION_ID")
    static @NotNull EntityDataAccessor<Integer> getDataPosRotInterpolationDurationId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_TRANSLATION_ID")
    static @NotNull EntityDataAccessor<Vector3fc> getDataTranslationId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_SCALE_ID")
    static @NotNull EntityDataAccessor<Vector3fc> getDataScaleId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_LEFT_ROTATION_ID")
    static @NotNull EntityDataAccessor<Quaternionfc> getDataLeftRotationId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_RIGHT_ROTATION_ID")
    static @NotNull EntityDataAccessor<Quaternionfc> getDataRightRotationId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_BILLBOARD_RENDER_CONSTRAINTS_ID")
    static @NotNull EntityDataAccessor<Byte> getDataBillboardRenderConstraintsId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_BRIGHTNESS_OVERRIDE_ID")
    static @NotNull EntityDataAccessor<Integer> getDataBrightnessOverrideId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_VIEW_RANGE_ID")
    static @NotNull EntityDataAccessor<Float> getDataViewRangeId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_SHADOW_RADIUS_ID")
    static @NotNull EntityDataAccessor<Float> getDataShadowRadiusId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_SHADOW_STRENGTH_ID")
    static @NotNull EntityDataAccessor<Float> getDataShadowStrengthId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_WIDTH_ID")
    static @NotNull EntityDataAccessor<Float> getDataWidthId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_HEIGHT_ID")
    static @NotNull EntityDataAccessor<Float> getDataHeightId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor("DATA_GLOW_COLOR_OVERRIDE_ID")
    static @NotNull EntityDataAccessor<Integer> getDataGlowColorOverrideId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }
}

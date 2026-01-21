/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.mixin;

import kr.toxicity.model.impl.fabric.attachment.BetterModelAttachments;
import kr.toxicity.model.impl.fabric.entity.EntityHook;
import kr.toxicity.model.impl.fabric.events.ServerEntityDismountCallback;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class)
public abstract class EntityMixin implements EntityHook {
    @Shadow
    public abstract Level level();

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public @Nullable String bettermodel$getModelData() {
        return ((AttachmentTarget) this).getAttached(BetterModelAttachments.MODEL_DATA);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void bettermodel$setModelData(@Nullable String modelData) {
        ((AttachmentTarget) this).setAttached(BetterModelAttachments.MODEL_DATA, modelData);
    }

    @Inject(
        method = "removePassenger",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void bettermodel$invokeDismountCallbacks(@NotNull Entity passenger, @NotNull CallbackInfo ci) {
        if (level().isClientSide()) return;

        Entity vehicle = betterModel$entity();
        if (vehicle != passenger.getVehicle() &&
            !ServerEntityDismountCallback.EVENT.invoker().onDismount(passenger, vehicle)
        ) {
            ci.cancel();
        }
    }

    @Unique
    private @NotNull Entity betterModel$entity() {
        return (Entity) (Object) this;
    }
}

/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import kr.toxicity.model.impl.fabric.events.ServerLivingEntityJumpCallback;
import kr.toxicity.model.impl.fabric.events.ServerMobEffectLoadCallback;
import kr.toxicity.model.impl.fabric.events.ServerMobEffectUnloadCallback;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    private LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;jumpFromGround()V"
        )
    )
    private void bettermodel$invokeJumpCallbacks(@NotNull CallbackInfo ci) {
        if (level().isClientSide()) return;

        ServerLivingEntityJumpCallback.EVENT.invoker().onJump(bettermodel$livingEntity());
    }

    @Inject(
        method = "onEffectAdded",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/effect/MobEffect;addAttributeModifiers(Lnet/minecraft/world/entity/ai/attributes/AttributeMap;I)V",
            shift = At.Shift.AFTER
        )
    )
    private void bettermodel$invokeEffectLoadCallbacks(@NotNull MobEffectInstance effect, @NotNull Entity source, @NotNull CallbackInfo ci) {
        if (level().isClientSide()) return;

        ServerMobEffectLoadCallback.EVENT.invoker().onLoad(bettermodel$livingEntity(), effect);
    }

    @Inject(
        method = "onEffectsRemoved",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/effect/MobEffect;removeAttributeModifiers(Lnet/minecraft/world/entity/ai/attributes/AttributeMap;)V",
            shift = At.Shift.AFTER
        )
    )
    private void bettermodel$invokeEffectUnloadCallbacks(@NotNull Collection<MobEffectInstance> effects, @NotNull CallbackInfo ci, @Local @NotNull MobEffectInstance effect) {
        if (level().isClientSide()) return;

        ServerMobEffectUnloadCallback.EVENT.invoker().onUnload(bettermodel$livingEntity(), effect);
    }

    @Unique
    private @NotNull LivingEntity bettermodel$livingEntity() {
        return (LivingEntity) (Object) this;
    }
}

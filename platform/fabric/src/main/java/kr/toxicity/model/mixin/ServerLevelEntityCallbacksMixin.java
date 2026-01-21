/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.mixin;

import kr.toxicity.model.impl.fabric.events.ServerMobEffectLoadCallback;
import kr.toxicity.model.impl.fabric.events.ServerMobEffectUnloadCallback;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
public abstract class ServerLevelEntityCallbacksMixin {

    @Inject(
        method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "TAIL")
    )
    private void bettermodel$invokeLoadCallbacks(Entity entity, CallbackInfo ci) {
        if (entity.level().isClientSide()) return;

        if (entity instanceof LivingEntity livingEntity) {
            for (MobEffectInstance effect : livingEntity.getActiveEffects()) {
                ServerMobEffectLoadCallback.EVENT.invoker().onLoad(livingEntity, effect);
            }
        }
    }

    @Inject(
        method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "HEAD")
    )
    private void bettermodel$invokeUnloadCallbacks(Entity entity, CallbackInfo ci) {
        if (entity.level().isClientSide()) return;

        if (entity instanceof LivingEntity livingEntity) {
            for (MobEffectInstance effect : livingEntity.getActiveEffects()) {
                ServerMobEffectUnloadCallback.EVENT.invoker().onUnload(livingEntity, effect);
            }
        }
    }
}

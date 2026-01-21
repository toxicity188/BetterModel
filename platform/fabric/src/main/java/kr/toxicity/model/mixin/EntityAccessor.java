/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Entity.class)
public interface EntityAccessor {
    @Accessor(value = "DATA_SHARED_FLAGS_ID")
    static @NotNull EntityDataAccessor<Byte> bettermodel$getDataSharedFlagsId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }
}

/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Display.ItemDisplay.class)
public interface ItemDisplayAccessor {
    @Accessor(value = "DATA_ITEM_STACK_ID")
    static @NotNull EntityDataAccessor<ItemStack> bettermodel$getDataItemStackId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Accessor(value = "DATA_ITEM_DISPLAY_ID")
    static @NotNull EntityDataAccessor<Byte> bettermodel$getDataItemDisplayId() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }
}

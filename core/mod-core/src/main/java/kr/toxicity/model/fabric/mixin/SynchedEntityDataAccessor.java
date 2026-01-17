/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = SynchedEntityData.class)
public interface SynchedEntityDataAccessor {
    @Accessor(value = "itemsById")
    @NotNull SynchedEntityData.DataItem<?>[] getItemsById();

    @Accessor(value = "isDirty")
    void setDirty(boolean dirty);

    @Invoker(value = "getItem")
    <T> SynchedEntityData.@NotNull DataItem<T> getItem(@NotNull EntityDataAccessor<T> key);
}

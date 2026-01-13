/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric.platform;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.platform.PlatformItemStack;
import kr.toxicity.model.api.platform.PlatformNamespace;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record FabricItemStack(@NotNull ItemStack stack) implements PlatformItemStack {
    @Override
    public boolean isAir() {
        return stack.isEmpty();
    }

    @Override
    public @NotNull PlatformItemStack enchant(boolean enchant) {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, enchant);
        return this;
    }

    @Override
    public @NotNull PlatformItemStack modelData(int customModelData, @Nullable PlatformNamespace namespace) {
        stack.set(
            DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(List.of((float) customModelData), List.of(), List.of(), List.of())
        );
        if (namespace != null && BetterModel.platform().version().useItemModelName()) {
            stack.set(
                DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath(namespace.path(), namespace.namespace())
            );
        }

        return this;
    }

    @Override
    public @NotNull PlatformItemStack clone() {
        return new FabricItemStack(stack.copy());
    }
}

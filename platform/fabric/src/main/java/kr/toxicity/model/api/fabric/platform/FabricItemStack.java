/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric.platform;

import kr.toxicity.model.api.platform.PlatformItemStack;
import kr.toxicity.model.api.platform.PlatformNamespace;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a Fabric item stack wrapped as a {@link PlatformItemStack}.
 *
 * @param source the source NMS item stack
 * @since 2.0.0
 */
public record FabricItemStack(@NotNull ItemStack source) implements PlatformItemStack {
    @ApiStatus.Internal
    public FabricItemStack {
    }

    /**
     * Creates a FabricItemStack from the source.
     *
     * @param source the source item stack
     * @return the instance
     * @since 2.0.0
     */
    public static @NotNull FabricItemStack of(@NotNull ItemStack source) {
        return new FabricItemStack(source);
    }

    @Override
    public boolean isAir() {
        return source.isEmpty();
    }

    @Override
    public @NotNull PlatformItemStack enchant(boolean enchant) {
        source.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, enchant);
        return this;
    }

    @Override
    public @NotNull PlatformItemStack modelData(int customModelData, @Nullable PlatformNamespace namespace) {
        source.set(
            DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(List.of((float) customModelData), List.of(), List.of(), List.of())
        );
        source.set(
            DataComponents.ITEM_MODEL,
            namespace == null ? null : Identifier.fromNamespaceAndPath(namespace.namespace(), namespace.path())
        );
        return this;
    }

    @Override
    public @NotNull PlatformItemStack clone() {
        return FabricAdapter.adapt(source.copy());
    }
}

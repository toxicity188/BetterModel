/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a namespace within a resource pack, providing access to standard subdirectories.
 * <p>
 * This class organizes resources into 'items', 'models', and 'textures' categories.
 * </p>
 *
 * @since 1.15.2
 */
public final class PackNamespace {

    private final PackBuilder items, models, textures;

    PackNamespace(@NotNull PackAssets assets, @NotNull String namespace) {
        var subPath = assets.path.resolve("assets", namespace);
        items = new PackBuilder(assets, subPath.resolve("items"));
        models = new PackBuilder(assets, subPath.resolve("models"));
        textures = new PackBuilder(assets, subPath.resolve("textures", "item"));
    }

    /**
     * Returns the builder for the 'items' directory.
     *
     * @return the items builder
     * @since 1.15.2
     */
    public @NotNull PackBuilder items() {
        return items;
    }

    /**
     * Returns the builder for the 'models' directory.
     *
     * @return the models builder
     * @since 1.15.2
     */
    public @NotNull PackBuilder models() {
        return models;
    }

    /**
     * Returns the builder for the 'textures/item' directory.
     *
     * @return the textures builder
     * @since 1.15.2
     */
    public @NotNull PackBuilder textures() {
        return textures;
    }
}

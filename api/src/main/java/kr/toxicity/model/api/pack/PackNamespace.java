/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

public final class PackNamespace {

    private final PackBuilder items, models, textures;

    PackNamespace(@NotNull PackAssets assets, @NotNull String namespace) {
        var subPath = assets.path.resolve("assets", namespace);
        items = new PackBuilder(assets, subPath.resolve("items"));
        models = new PackBuilder(assets, subPath.resolve("models"));
        textures = new PackBuilder(assets, subPath.resolve("textures", "item"));
    }

    public @NotNull PackBuilder items() {
        return items;
    }

    public @NotNull PackBuilder models() {
        return models;
    }

    public @NotNull PackBuilder textures() {
        return textures;
    }
}

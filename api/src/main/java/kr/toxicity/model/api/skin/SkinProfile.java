/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.skin;

import com.mojang.authlib.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public record SkinProfile(
        @NotNull UUID id,
        @NotNull String name,
        @NotNull Collection<Property> textures
) {}
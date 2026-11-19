/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains resource-pack building and packaging contracts.
 * <p>
 * Pack types describe resource paths, namespaces, byte resources, overlays,
 * metadata, built-in assets, obfuscation, build results, and zipper support.
 * They provide the API shape for producing Minecraft resource packs from loaded
 * model data without embedding platform delivery logic.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * PackPath path = new PackPath("assets/bettermodel/models/example.json");
 * PackByte entry = new PackByte(path, bytes);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.pack;

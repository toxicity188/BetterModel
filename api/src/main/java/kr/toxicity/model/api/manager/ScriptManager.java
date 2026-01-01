/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.script.AnimationScript;
import kr.toxicity.model.api.script.ScriptBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the parsing and registration of animation scripts.
 * <p>
 * This manager allows for the creation of custom script logic that can be embedded within animations.
 * </p>
 *
 * @since 1.15.2
 */
public interface ScriptManager {
    /**
     * Parses a raw script string into an {@link AnimationScript}.
     *
     * @param script the raw script string
     * @return the parsed script, or null if parsing failed
     * @since 1.15.2
     */
    @Nullable AnimationScript build(@NotNull String script);

    /**
     * Registers a new script builder.
     *
     * @param name the name of the parser/builder
     * @param script the script builder instance
     * @since 1.15.2
     */
    void addBuilder(@NotNull String name, @NotNull ScriptBuilder script);
}

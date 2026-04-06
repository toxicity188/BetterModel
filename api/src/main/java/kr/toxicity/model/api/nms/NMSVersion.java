/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.nms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Enumerates supported Minecraft server versions and their associated metadata.
 * <p>
 * This enum maps internal version identifiers to Minecraft versions and resource pack formats.
 * </p>
 *
 * @since 1.15.2
 */
@RequiredArgsConstructor
@Getter
public enum NMSVersion {
    /**
     * Minecraft 1.21.4
     * @since 1.15.2
     */
    V1_21_R3(46),
    /**
     * Minecraft 1.21.5
     * @since 1.15.2
     */
    V1_21_R4(55),
    /**
     * Minecraft 1.21.6 - 1.21.8
     * @since 1.15.2
     */
    V1_21_R5(64),
    /**
     * Minecraft 1.21.9 - 1.21.10
     * @since 1.15.2
     */
    V1_21_R6(69),
    /**
     * Minecraft 1.21.11
     * @since 1.15.2
     */
    V1_21_R7(75),
    /**
     * Minecraft 26.1
     * @since 3.0.0
     */
    V26_R1(84)
    ;
    /**
     * The resource pack format version (pack.mcmeta).
     */
    private final int metaVersion;

    /**
     * Returns the oldest supported version.
     *
     * @return the first version enum
     * @since 1.15.2
     */
    public static @NotNull NMSVersion first() {
        return values()[0];
    }

    /**
     * Returns the latest supported version.
     *
     * @return the last version enum
     * @since 1.15.2
     */
    public static @NotNull NMSVersion latest() {
        var entries = values();
        return entries[entries.length - 1];
    }
}

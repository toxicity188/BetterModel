/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
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
     * Minecraft 1.20.5 - 1.20.6
     * @since 1.15.2
     */
    V1_20_R4(20,4, 32),
    /**
     * Minecraft 1.21 - 1.21.1
     * @since 1.15.2
     */
    V1_21_R1(21,1, 34),
    /**
     * Minecraft 1.21.2 - 1.21.3
     * @since 1.15.2
     */
    V1_21_R2(21,2, 42),
    /**
     * Minecraft 1.21.4
     * @since 1.15.2
     */
    V1_21_R3(21,3, 46),
    /**
     * Minecraft 1.21.5
     * @since 1.15.2
     */
    V1_21_R4(21,4, 55),
    /**
     * Minecraft 1.21.6 - 1.21.8
     * @since 1.15.2
     */
    V1_21_R5(21,5, 64),
    /**
     * Minecraft 1.21.9 - 1.21.10
     * @since 1.15.2
     */
    V1_21_R6(21,6, 69),
    /**
     * Minecraft 1.21.11
     * @since 1.15.2
     */
    V1_21_R7(21,7, 75)
    ;
    /**
     * The major version number (e.g., 21 for 1.21).
     */
    private final int version;
    /**
     * The sub-version number.
     */
    private final int subVersion;
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

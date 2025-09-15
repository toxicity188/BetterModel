/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.version;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Minecraft version.
 * @param major title
 * @param minor main update
 * @param patch minor update
 */
public record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {
    /**
     * 1.21.8
     */
    public static final MinecraftVersion V1_21_8 = of(1, 21, 8);
    /**
     * 1.21.7
     */
    public static final MinecraftVersion V1_21_7 = of(1, 21, 7);
    /**
     * 1.21.6
     */
    public static final MinecraftVersion V1_21_6 = of(1, 21, 6);
    /**
     * 1.21.5
     */
    public static final MinecraftVersion V1_21_5 = of(1, 21, 5);
    /**
     * 1.21.4
     */
    public static final MinecraftVersion V1_21_4 = of(1, 21, 4);
    /**
     * 1.21.3
     */
    public static final MinecraftVersion V1_21_3 = of(1, 21, 3);
    /**
     * 1.21.2
     */
    public static final MinecraftVersion V1_21_2 = of(1, 21, 2);
    /**
     * 1.21.1
     */
    public static final MinecraftVersion V1_21_1 = of(1, 21, 1);
    /**
     * 1.21
     */
    public static final MinecraftVersion V1_21 = of(1, 21, 0);
    /**
     * 1.20.6
     */
    public static final MinecraftVersion V1_20_6 = of(1, 20, 6);
    /**
     * 1.20.5
     */
    public static final MinecraftVersion V1_20_5 = of(1, 20, 5);

    /**
     * Comparator
     */
    private static final Comparator<MinecraftVersion> COMPARATOR = Comparator.comparing(MinecraftVersion::major)
            .thenComparing(MinecraftVersion::minor)
            .thenComparing(MinecraftVersion::patch);

    /**
     * Parses version from string
     * @param version version like "1.21.8"
     */
    public static @NotNull MinecraftVersion parse(@NotNull String version) {
        var split = version.split("\\.");
        return of(
                split.length > 0 ? Integer.parseInt(split[0]) : 0,
                split.length > 1 ? Integer.parseInt(split[1]) : 0,
                split.length > 2 ? Integer.parseInt(split[2]) : 0
        );
    }

    /**
     * Creates version
     * @param major major
     * @param minor minor
     * @param patch patch
     * @return Minecraft version
     */
    public static @NotNull MinecraftVersion of(int major, int minor, int patch) {
        return new MinecraftVersion(major, minor, patch);
    }

    /**
     * Checks this version is greater or equals than another.
     * @param other other
     * @return greater or not
     */
    public boolean isGreaterOrEquals(@NotNull MinecraftVersion other) {
        return compareTo(other) >= 0;
    }

    /**
     * Checks this version should be use modern resource.
     * @return use modern resource
     */
    public boolean useModernResource() {
        return isGreaterOrEquals(V1_21_4);
    }

    /**
     * Checks this version should be use item model namespace.
     * @return use item model namespace.
     */
    public boolean useItemModelName() {
        return isGreaterOrEquals(V1_21_2);
    }

    @Override
    public int compareTo(@NotNull MinecraftVersion o) {
        return COMPARATOR.compare(this, o);
    }

    @NotNull
    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
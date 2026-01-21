/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Pack util
 */
@ApiStatus.Internal
public final class PackUtil {
    /**
     * No initializer
     */
    private PackUtil() {
        throw new RuntimeException();
    }

    private static final Pattern REPLACE_SOURCE = Pattern.compile("[^a-z0-9_.]");

    /**
     * Asserts that the given raw string is a valid pack name.
     * Throws a {@link IllegalArgumentException} if the name contains illegal characters.
     * @param raw The raw string to validate.
     */
    public static void assertPackName(@NotNull String raw) {
        if (REPLACE_SOURCE.matcher(raw).find()) throw new IllegalArgumentException("Illegal pack name: " + raw);
    }

    /**
     * Converts some path to compatible with Minecraft resource location
     * @param raw raw path
     * @return converted path
     */
    public static @NotNull String toPackName(@NotNull String raw) {
        return REPLACE_SOURCE.matcher(raw.toLowerCase()).replaceAll("_");
    }
}

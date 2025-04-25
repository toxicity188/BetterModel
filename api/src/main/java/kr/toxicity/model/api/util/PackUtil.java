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
     * Private initializer
     */
    private PackUtil() {
        throw new RuntimeException();
    }

    private static final Pattern REPLACE_SOURCE = Pattern.compile("[^a-z0-9_.]");

    public static @NotNull String toPackName(@NotNull String raw) {
        return REPLACE_SOURCE.matcher(raw.toLowerCase()).replaceAll("_");
    }
}

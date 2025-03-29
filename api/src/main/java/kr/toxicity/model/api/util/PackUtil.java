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

    /**
     * Pack validator
     */
    public static final Pattern PATH_PATTERN = Pattern.compile("^[a-z0-9/._-]+$");

    /**
     * Checks some path is valid
     * @param path path
     * @param message error message
     */
    public static void validatePath(@NotNull String path, @NotNull String message) {
        if (!PATH_PATTERN.matcher(path).find()) throw new RuntimeException(message);
    }
}

package kr.toxicity.model.api.util;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class PackUtil {
    private PackUtil() {
        throw new RuntimeException();
    }

    public static final Pattern PATH_PATTERN = Pattern.compile("[a-z0-9/._-]");

    public static void validatePath(@NotNull String path, @NotNull String message) {
        if (!PATH_PATTERN.matcher(path).find()) throw new RuntimeException(message);
    }
}

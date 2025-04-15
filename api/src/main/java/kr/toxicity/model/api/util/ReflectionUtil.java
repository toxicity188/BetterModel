package kr.toxicity.model.api.util;

import org.jetbrains.annotations.NotNull;

public final class ReflectionUtil {
    private ReflectionUtil() {
        throw new RuntimeException();
    }

    public static boolean classExists(@NotNull String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

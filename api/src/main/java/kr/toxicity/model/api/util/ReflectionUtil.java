package kr.toxicity.model.api.util;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Reflection util
 */
@ApiStatus.Internal
public final class ReflectionUtil {
    /**
     * No initializer
     */
    private ReflectionUtil() {
        throw new RuntimeException();
    }

    /**
     * Checks some class is existing.
     * @param clazz class path
     * @return exists
     */
    public static boolean classExists(@NotNull String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

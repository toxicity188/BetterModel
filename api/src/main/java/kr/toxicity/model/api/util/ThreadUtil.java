/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import kr.toxicity.model.api.BetterModel;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;

/**
 * Thread util
 */
@ApiStatus.Internal
public final class ThreadUtil {
    /**
     * No initializer
     */
    private ThreadUtil() {
        throw new RuntimeException();
    }

    /**
     * Checks current thread is tick safe with Folia
     * @return tick safe with Folia
     */
    public static boolean isFoliaSafe() {
        return !BetterModel.IS_FOLIA || isTickThread();
    }

    /**
     * Checks current thread is tick thread
     * @return is tick thread
     */
    public static boolean isTickThread() {
        return Bukkit.isPrimaryThread();
    }
}

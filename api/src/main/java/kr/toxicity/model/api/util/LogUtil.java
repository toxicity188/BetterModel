/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.config.DebugConfig;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Log util
 */
@ApiStatus.Internal
public final class LogUtil {
    /**
     * No initializer
     */
    private LogUtil() {
        throw new RuntimeException();
    }

    /**
     * Handles exception message
     * @param message message
     * @param throwable exception
     */
    public static void handleException(@NotNull String message, @NotNull Throwable throwable) {
        var list = new ArrayList<String>();
        list.add(message);
        list.add("Reason: " + throwable.getMessage());
        if (BetterModel.config().debug().has(DebugConfig.DebugOption.EXCEPTION)) {
            list.add("Stack trace:");
            try (
                    var byteArray = new ByteArrayOutputStream();
                    var print = new PrintStream(byteArray)
            ) {
                throwable.printStackTrace(print);
                list.add(byteArray.toString(StandardCharsets.UTF_8));
            } catch (IOException e) {
                list.add("Unknown");
            }
        } else list.add("If you want to see the stack trace, set debug.exception to true in config.yml");
        BetterModel.plugin().logger().warn(list.toArray(String[]::new));
    }

    /**
     * Logs debug if some option is matched.
     * @param option option
     * @param log log
     */
    public static void debug(@NotNull DebugConfig.DebugOption option, @NotNull Supplier<String> log) {
        debug(option, () -> BetterModel.plugin().logger().info("DEBUG-" + option + ": " + log.get()));
    }

    /**
     * Runs debug if some option is matched.
     * @param option option
     * @param runnable debug task
     */
    public static void debug(@NotNull DebugConfig.DebugOption option, @NotNull Runnable runnable) {
        if (BetterModel.config().debug().has(option)) runnable.run();
    }
}

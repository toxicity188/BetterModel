package kr.toxicity.model.api.util;

import kr.toxicity.model.api.BetterModel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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
        if (BetterModel.inst().configManager().debug().exception()) {
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
        BetterModel.inst().logger().warn(list.toArray(String[]::new));
    }
}

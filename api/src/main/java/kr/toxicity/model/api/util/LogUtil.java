package kr.toxicity.model.api.util;

import kr.toxicity.model.api.BetterModel;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LogUtil {
    private LogUtil() {
        throw new RuntimeException();
    }

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
        }
        BetterModel.inst().logger().warn(list.toArray(String[]::new));
    }
}

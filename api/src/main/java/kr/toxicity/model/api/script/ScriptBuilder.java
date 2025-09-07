package kr.toxicity.model.api.script;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Script parser
 */
@FunctionalInterface
public interface ScriptBuilder {
    /**
     * Build an entity script by data
     * @param data script data
     * @return script
     */
    @NotNull AnimationScript build(@NotNull ScriptData data);

    record ScriptData(
            @Nullable String args,
            @NotNull ScriptMetaData metadata
    ) {}

    interface ScriptMetaData {

        @NotNull @Unmodifiable
        Map<String, String> toMap();

        default @Nullable Boolean asBoolean(@NotNull String key) {
            var get = toMap().get(key);
            if (get == null) return null;
            return switch (get) {
                case "true" -> true;
                case "false" -> false;
                default -> null;
            };
        }

        default @Nullable Number asNumber(@NotNull String key) {
            var get = toMap().get(key);
            if (get == null) return null;
            try {
                return new BigDecimal(get);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        default @Nullable String asString(@NotNull String key) {
            return toMap().get(key);
        }
    }
}

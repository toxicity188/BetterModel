package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * A raw children of model.
 */
public sealed interface ModelChildren {

    /**
     * Singleton parser instance.
     */
    Parser PARSER = new Parser();

    /**
     * Parser
     */
    final class Parser implements Function<JsonElement, ModelChildren> {

        /**
         * Private initializer.
         */
        private Parser() {
        }

        @Override
        public ModelChildren apply(JsonElement element) {
            if (element.isJsonPrimitive()) return new ModelUUID(UUID.fromString(element.getAsString()));
            else if (element.isJsonObject()) {
                var object = element.getAsJsonObject();
                return new ModelGroup(
                        object.getAsJsonPrimitive("name").getAsString(),
                        Float3.PARSER.apply(object.get("origin")),
                        Float3.PARSER.apply(object.get("rotation")),
                        UUID.fromString(object.getAsJsonPrimitive("uuid").getAsString()),
                        object.getAsJsonArray("children").asList().stream().map(this).toList(),
                        object.getAsJsonPrimitive("visibility").getAsBoolean()
                );
            } else throw new RuntimeException();
        }
    }

    /**
     * A raw element's uuid.
     * @param uuid
     */
    record ModelUUID(@NotNull UUID uuid) implements ModelChildren {
    }

    /**
     * A raw group of model.
     * @param name group name
     * @param origin origin
     * @param rotation rotation
     * @param uuid uuid
     * @param children children
     * @param visibility visibility
     */
    record ModelGroup(
            @NotNull String name,
            @NotNull Float3 origin,
            @NotNull Float3 rotation,
            @NotNull UUID uuid,
            @NotNull List<ModelChildren> children,
            boolean visibility
    ) implements ModelChildren {
    }
}

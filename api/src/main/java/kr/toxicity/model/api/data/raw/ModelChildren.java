package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
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
            if (element.isJsonPrimitive()) return new ModelUUID(element.getAsString());
            else if (element.isJsonObject()) {
                var object = element.getAsJsonObject();
                return new ModelGroup(
                        object.getAsJsonPrimitive("name").getAsString(),
                        Float3.PARSER.apply(object.get("origin")),
                        Float3.PARSER.apply(object.get("rotation")),
                        object.getAsJsonPrimitive("uuid").getAsString(),
                        object.getAsJsonArray("children").asList().stream().map(this).toList(),
                        Optional.ofNullable(object.getAsJsonPrimitive("visibility")).map(JsonPrimitive::getAsBoolean).orElse(true)
                );
            } else throw new RuntimeException();
        }
    }

    /**
     * A raw element's uuid.
     * @param uuid
     */
    record ModelUUID(@NotNull String uuid) implements ModelChildren {
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
            @NotNull String uuid,
            @NotNull List<ModelChildren> children,
            boolean visibility
    ) implements ModelChildren {
    }
}

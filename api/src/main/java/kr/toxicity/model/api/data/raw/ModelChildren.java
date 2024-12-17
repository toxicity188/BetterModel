package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public sealed interface ModelChildren {

    Parser PARSER = new Parser();

    final class Parser implements Function<JsonElement, ModelChildren> {
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
                        object.getAsJsonArray("children").asList().stream().map(this).toList()
                );
            } else throw new RuntimeException();
        }
    }

    record ModelUUID(@NotNull UUID uuid) implements ModelChildren {
    }

    record ModelGroup(
            @NotNull String name,
            @NotNull Float3 origin,
            @NotNull Float3 rotation,
            @NotNull UUID uuid,
            @NotNull List<ModelChildren> children
    ) implements ModelChildren {
    }
}

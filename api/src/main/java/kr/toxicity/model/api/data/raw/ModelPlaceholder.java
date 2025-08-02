package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static kr.toxicity.model.api.util.CollectionUtil.associate;

public record ModelPlaceholder(
        @NotNull Map<String, String> variables
) {
    public static final ModelPlaceholder EMPTY = new ModelPlaceholder(Collections.emptyMap());
    public static final JsonDeserializer<ModelPlaceholder> PARSER = (json, typeOfT, context) -> new ModelPlaceholder(associate(
            Arrays.stream(json.getAsString().split("\n"))
                    .map(entry -> entry.split("=", 2))
                    .filter(array -> array.length == 2),
            array -> array[0],
            array -> array[1]
    ));


    public @NotNull String parseVariable(@NotNull String expression) {
        for (var entry : variables.entrySet()) {
            expression = expression.replace(entry.getKey(), entry.getValue());
        }
        return expression;
    }
}

package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.List;

/**
 * A raw children of the model.
 */
@ApiStatus.Internal
public sealed interface ModelChildren {

    /**
     * Singleton parser instance.
     */
    Parser PARSER = new Parser();

    /**
     * Parser
     */
    final class Parser implements JsonDeserializer<ModelChildren> {

        /**
         * Private initializer.
         */
        private Parser() {
        }

        @Override
        public ModelChildren deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (element.isJsonPrimitive()) return new ModelUUID(element.getAsString());
            else if (element.isJsonObject()) return context.deserialize(element, ModelGroup.class);
            else throw new RuntimeException();
        }
    }

    /**
     * A raw element's uuid.
     * @param uuid uuid
     */
    record ModelUUID(@NotNull String uuid) implements ModelChildren {
    }

    /**
     * A raw group of models.
     * @param name group name
     * @param origin origin
     * @param rotation rotation
     * @param uuid uuid
     * @param children children
     * @param _visibility visibility
     */
    record ModelGroup(
            @NotNull String name,
            @Nullable Float3 origin,
            @Nullable Float3 rotation,
            @NotNull String uuid,
            @NotNull List<ModelChildren> children,
            @Nullable @SerializedName("visibility") Boolean _visibility
    ) implements ModelChildren {
        @Override
        @NotNull
        public Float3 origin() {
            return origin != null ? origin : Float3.ZERO;
        }

        @Override
        @NotNull
        public Float3 rotation() {
            return rotation != null ? rotation : Float3.ZERO;
        }

        public boolean visibility() {
            return !Boolean.FALSE.equals(_visibility);
        }
    }
}

package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializer;
import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.blueprint.BlueprintChildren;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static kr.toxicity.model.api.util.CollectionUtil.filterIsInstance;
import static kr.toxicity.model.api.util.CollectionUtil.mapToList;

/**
 * A raw children of the model.
 */
@ApiStatus.Internal
public sealed interface ModelChildren {

    /**
     * Parser
     */
    JsonDeserializer<ModelChildren> PARSER = (json, typeOfT, context) -> {
        if (json.isJsonPrimitive()) return new ModelUUID(json.getAsString());
        else if (json.isJsonObject()) return context.deserialize(json, ModelGroup.class);
        else throw new RuntimeException();
    };

    /**
     * Converts children to blueprint children
     * @param elementMap element map
     * @return children
     */
    @NotNull BlueprintChildren toBlueprint(@NotNull @Unmodifiable Map<String, ModelElement> elementMap);

    /**
     * A raw element's uuid.
     * @param uuid uuid
     */
    record ModelUUID(@NotNull String uuid) implements ModelChildren {
        @Override
        public @NotNull BlueprintChildren toBlueprint(@NotNull @Unmodifiable Map<String, ModelElement> elementMap) {
            return new BlueprintChildren.BlueprintElement(Objects.requireNonNull(elementMap.get(uuid())));
        }
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
        /**
         * Gets origin
         * @return origin
         */
        @Override
        @NotNull
        public Float3 origin() {
            return origin != null ? origin : Float3.ZERO;
        }

        /**
         * Gets rotation
         * @return rotation
         */
        @Override
        @NotNull
        public Float3 rotation() {
            return rotation != null ? rotation : Float3.ZERO;
        }

        /**
         * Gets visibility
         * @return visibility
         */
        public boolean visibility() {
            return !Boolean.FALSE.equals(_visibility);
        }

        @Override
        public @NotNull BlueprintChildren toBlueprint(@NotNull @Unmodifiable Map<String, ModelElement> elementMap) {
            var child = mapToList(children, c -> c.toBlueprint(elementMap));
            var filtered = filterIsInstance(child, BlueprintChildren.BlueprintElement.class).toList();
            return new BlueprintChildren.BlueprintGroup(
                    BoneTagRegistry.parse(name()),
                    origin(),
                    rotation().invertXZ(),
                    child,
                    filtered.isEmpty() ? visibility() : filtered.stream().anyMatch(element -> element.element().visibility())
            );
        }
    }
}

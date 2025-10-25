/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializer;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.blueprint.BlueprintChildren;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
        else if (json.isJsonObject()) {
            var children = json.getAsJsonObject().getAsJsonArray("children");
            return new ModelOutliner(
                    context.deserialize(json, ModelGroup.class),
                    children.asList()
                            .stream()
                            .map(child -> (ModelChildren) context.deserialize(child, ModelChildren.class))
                            .toList()
            );
        }
        else throw new RuntimeException();
    };

    /**
     * Converts children to blueprint children
     * @param context context
     * @return children
     */
    @NotNull BlueprintChildren toBlueprint(@NotNull ModelLoadContext context);

    /**
     * Flattens this children tree
     * @return flatten stream
     */
    @NotNull Stream<ModelChildren> flatten();

    /**
     * Gets uuid
     * @return uuid
     */
    @NotNull String uuid();

    /**
     * A raw element's uuid.
     * @param uuid uuid
     */
    record ModelUUID(@NotNull String uuid) implements ModelChildren {
        @Override
        public @NotNull BlueprintChildren toBlueprint(@NotNull ModelLoadContext context) {
            return new BlueprintChildren.BlueprintElement(Objects.requireNonNull(context.elements.get(uuid())));
        }

        @Override
        public @NotNull Stream<ModelChildren> flatten() {
            return Stream.of(this);
        }
    }

    /**
     * A raw outliner of models.
     * @param group group
     * @param children children
     */
    record ModelOutliner(
            @NotNull ModelGroup group,
            @NotNull @Unmodifiable List<ModelChildren> children
    ) implements ModelChildren {

        @Override
        public @NotNull BlueprintChildren toBlueprint(@NotNull ModelLoadContext context) {
            var child = mapToList(children, c -> c.toBlueprint(context));
            var filtered = filterIsInstance(child, BlueprintChildren.BlueprintElement.class).toList();
            var selectedGroup = context.groups.getOrDefault(uuid(), group);
            return new BlueprintChildren.BlueprintGroup(
                    BoneTagRegistry.parse(selectedGroup.name()),
                    selectedGroup.origin(),
                    selectedGroup.rotation().invertXZ(),
                    child,
                    filtered.isEmpty() ? selectedGroup.visibility() : filtered.stream().anyMatch(element -> element.element().visibility())
            );
        }

        @Override
        public @NotNull Stream<ModelChildren> flatten() {
            return Stream.concat(
                    Stream.of(this),
                    children.stream().flatMap(ModelChildren::flatten)
            );
        }

        @Override
        public @NotNull String uuid() {
            return group.uuid();
        }
    }
}

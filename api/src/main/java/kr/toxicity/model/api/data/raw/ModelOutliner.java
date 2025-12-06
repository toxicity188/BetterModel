/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.JsonDeserializer;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.blueprint.BlueprintElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.filterIsInstance;
import static kr.toxicity.model.api.util.CollectionUtil.mapToList;

/**
 * A outliner of the model.
 */
@ApiStatus.Internal
public sealed interface ModelOutliner {

    /**
     * Parser
     */
    JsonDeserializer<ModelOutliner> PARSER = (json, typeOfT, context) -> {
        if (json.isJsonPrimitive()) return new Reference(json.getAsString());
        else if (json.isJsonObject()) {
            var children = json.getAsJsonObject().getAsJsonArray("children");
            return new Tree(
                context.deserialize(json, ModelGroup.class),
                children.asList()
                    .stream()
                    .map(child -> (ModelOutliner) context.deserialize(child, ModelOutliner.class))
                    .toList()
            );
        }
        else throw new RuntimeException();
    };

    /**
     * Converts outliner to blueprint element
     * @param context context
     * @return element
     */
    @NotNull BlueprintElement toBlueprint(@NotNull ModelLoadContext context);

    /**
     * Flattens this outliner tree
     * @return flatten stream
     */
    @NotNull Stream<ModelOutliner> flatten();

    /**
     * Gets uuid
     * @return uuid
     */
    @NotNull String uuid();

    /**
     * An uuid reference of model element
     * @param uuid uuid
     */
    record Reference(@NotNull String uuid) implements ModelOutliner {
        @Override
        public @NotNull BlueprintElement toBlueprint(@NotNull ModelLoadContext context) {
            return Objects.requireNonNull(context.elements.get(uuid())).toBlueprint();
        }

        @Override
        public @NotNull Stream<ModelOutliner> flatten() {
            return Stream.of(this);
        }
    }

    /**
     * A tree of models
     * @param group group (legacy BlockBench)
     * @param children children
     */
    record Tree(
        @NotNull ModelGroup group,
        @NotNull @Unmodifiable List<ModelOutliner> children
    ) implements ModelOutliner {

        @Override
        public @NotNull BlueprintElement toBlueprint(@NotNull ModelLoadContext context) {
            var child = mapToList(children, c -> c.toBlueprint(context));
            var filtered = filterIsInstance(child, BlueprintElement.Cube.class).toList();
            var selectedGroup = context.groups.getOrDefault(uuid(), group);
            return new BlueprintElement.Group(
                UUID.fromString(selectedGroup.uuid()),
                BoneTagRegistry.parse(selectedGroup.name()),
                selectedGroup.origin(),
                selectedGroup.rotation().invertXZ(),
                child,
                filtered.isEmpty() ? selectedGroup.visibility() : filtered.stream().anyMatch(BlueprintElement.Cube::visibility)
            );
        }

        @Override
        public @NotNull Stream<ModelOutliner> flatten() {
            return Stream.concat(
                Stream.of(this),
                children.stream().flatMap(ModelOutliner::flatten)
            );
        }

        @Override
        public @NotNull String uuid() {
            return group.uuid();
        }
    }
}

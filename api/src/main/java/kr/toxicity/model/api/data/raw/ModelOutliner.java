/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
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
 * Represents the hierarchical structure (outliner) of a model.
 * <p>
 * The outliner defines the parent-child relationships between groups and elements (cubes, locators, etc.).
 * It can be either a direct reference to an element (UUID string) or a tree node (Group) containing children.
 * </p>
 *
 * @since 1.15.2
 */
@ApiStatus.Internal
public sealed interface ModelOutliner {

    /**
     * A JSON deserializer that parses the outliner structure.
     * <p>
     * It distinguishes between leaf nodes (UUID strings) and branch nodes (Groups with children).
     * </p>
     * @since 1.15.2
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
     * Converts this outliner node into a processed {@link BlueprintElement}.
     *
     * @param context the model loading context
     * @return the blueprint element
     * @since 1.15.2
     */
    @NotNull BlueprintElement toBlueprint(@NotNull ModelLoadContext context);

    /**
     * Flattens the outliner tree into a stream of all nodes.
     *
     * @return a stream of all outliner nodes
     * @since 1.15.2
     */
    @NotNull Stream<ModelOutliner> flatten();

    /**
     * Returns the UUID of this outliner node.
     *
     * @return the UUID string
     * @since 1.15.2
     */
    @NotNull String uuid();

    /**
     * Represents a leaf node in the outliner, referencing a specific element by UUID.
     *
     * @param uuid the UUID of the referenced element
     * @since 1.15.2
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
     * Represents a branch node (Group) in the outliner, containing child nodes.
     *
     * @param group the group definition
     * @param children the list of child outliner nodes
     * @since 1.15.2
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

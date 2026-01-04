/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.Float3;
import kr.toxicity.model.api.data.Float4;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static kr.toxicity.model.api.util.CollectionUtil.*;

/**
 * Represents the raw data structure of a model file, typically parsed from a .bbmodel JSON file.
 * <p>
 * This record holds all the top-level components of a BlockBench model, including metadata, elements, textures, and animations.
 * It serves as the initial data container before being processed into a {@link ModelBlueprint}.
 * </p>
 *
 * @param meta the metadata of the model
 * @param resolution the texture resolution
 * @param elements the list of cube/mesh elements
 * @param outliner the hierarchical structure of the model
 * @param textures the list of textures used in the model
 * @param animations the list of animations
 * @param groups the list of groups (used in BlockBench 5.0.0+)
 * @param placeholder the animation variable placeholders
 * @since 1.15.2
 */
@ApiStatus.Internal
public record ModelData(
    @NotNull ModelMeta meta,
    @NotNull ModelResolution resolution,
    @NotNull List<ModelElement> elements,
    @NotNull List<ModelOutliner> outliner,
    @NotNull List<ModelTexture> textures,
    @Nullable List<ModelAnimation> animations,
    @Nullable List<ModelGroup> groups,
    @Nullable @SerializedName("animation_variable_placeholders") ModelPlaceholder placeholder
) {
    /**
     * The GSON parser configured for deserializing model data.
     * @since 1.15.2
     */
    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Float3.class, Float3.PARSER)
        .registerTypeAdapter(Float4.class, Float4.PARSER)
        .registerTypeAdapter(ModelMeta.class, ModelMeta.PARSER)
        .registerTypeAdapter(ModelOutliner.class, ModelOutliner.PARSER)
        .registerTypeAdapter(ModelPlaceholder.class, ModelPlaceholder.PARSER)
        .registerTypeAdapter(ModelElement.class, ModelElement.PARSER)
        .create();

    /**
     * Converts this raw model data into a processed {@link ModelBlueprint}.
     *
     * @param name the name to assign to the blueprint
     * @return the result of the loading process, containing the blueprint and any errors
     * @since 1.15.2
     */
    public @NotNull ModelLoadResult loadBlueprint(@NotNull String name) {
        return loadBlueprint(name, BetterModel.config().enableStrictLoading());
    }

    /**
     * Converts this raw model data into a processed {@link ModelBlueprint} with a specific loading mode.
     *
     * @param name the name to assign to the blueprint
     * @param strict whether to use strict loading mode (fail on unsupported features)
     * @return the result of the loading process, containing the blueprint and any errors
     * @since 1.15.2
     */
    public @NotNull ModelLoadResult loadBlueprint(@NotNull String name, boolean strict) {
        var context = new ModelLoadContext(
            name,
            placeholder(),
            meta(),
            associate(elements(), ModelElement::uuid),
            associate(groups(), ModelGroup::uuid),
            mapToSet(outliner().stream().flatMap(ModelOutliner::flatten), ModelOutliner::uuid),
            strict
        );
        var group = mapToList(outliner(), outliner -> outliner.toBlueprint(context));
        return new ModelLoadResult(
            new ModelBlueprint(
                context.name,
                resolution(),
                mapToList(textures(), texture -> texture.toBlueprint(context)),
                group,
                associate(animations().stream().map(raw -> raw.toBlueprint(context, group)), BlueprintAnimation::name)
            ),
            context.errors
        );
    }

    /**
     * Asserts that the model does not contain any unsupported element types.
     *
     * @throws RuntimeException if an unsupported element is found
     * @since 1.15.2
     */
    public void assertSupported() {
        elements().stream()
            .filter(e -> !e.isSupported())
            .findFirst()
            .ifPresent(e -> {
                throw new RuntimeException("This model file has unsupported element type: " + e.type());
            });
    }

    /**
     * Returns the animation variable placeholders, or an empty placeholder if none are defined.
     *
     * @return the animation variable placeholders
     * @since 1.15.2
     */
    @Override
    public @NotNull ModelPlaceholder placeholder() {
        return placeholder != null ? placeholder : ModelPlaceholder.EMPTY;
    }

    /**
     * Returns the list of animations, or an empty list if none are defined.
     *
     * @return the list of animations
     * @since 1.15.2
     */
    @Override
    @NotNull
    public List<ModelAnimation> animations() {
        return animations != null ? animations : Collections.emptyList();
    }

    /**
     * Returns the list of groups, or an empty list if none are defined.
     *
     * @return the list of groups
     * @since 1.15.2
     */
    @Override
    public @NotNull List<ModelGroup> groups() {
        return groups != null ? groups : Collections.emptyList();
    }
}

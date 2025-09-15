/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonObject;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.data.raw.Float3;
import kr.toxicity.model.api.data.raw.ModelElement;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.PackUtil;
import kr.toxicity.model.api.util.json.JsonObjectBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Quaternionf;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.*;

/**
 * A children of blueprint (group, element).
 */
public sealed interface BlueprintChildren {

    /**
     * Blueprint group
     * @param name group name
     * @param origin origin
     * @param rotation rotation
     * @param children children
     * @param visibility visibility
     */
    record BlueprintGroup(
            @NotNull BoneName name,
            @NotNull Float3 origin,
            @NotNull Float3 rotation,
            @NotNull List<BlueprintChildren> children,
            boolean visibility
    ) implements BlueprintChildren {

        /**
         * Gets origin
         * @return origin
         */
        @Override
        @NotNull
        public Float3 origin() {
            return origin.invertXZ();
        }

        /**
         * Gets JSON name of blueprint.
         * @param parent parent
         * @return name
         */
        public @NotNull String jsonName(@NotNull ModelBlueprint parent) {
            return PackUtil.toPackName(parent.name() + "_" + name.rawName());
        }

        /**
         * Gets blueprint legacy json
         * @param skipLog skip log
         * @param parent parent
         * @return json
         */
        public @Nullable BlueprintJson buildLegacyJson(
                boolean skipLog,
                @NotNull ModelBlueprint parent
        ) {
            Predicate<BlueprintElement> filter = element -> MathUtil.checkValidDegree(element.identifierDegree());
            if (!skipLog) filter = filterWithWarning(
                    filter,
                    element -> "The model " + parent.name() + "'s cube \"" + element.element.name() + "\" has an invalid rotation which does not supported in legacy client (<=1.21.3) " + element.element.rotation()
            );
            return buildJson(-2, 1, scale(), parent, Float3.ZERO, filterIsInstance(children, BlueprintElement.class).filter(filter));
        }

        /**
         * Gets blueprint modern json
         * @param parent parent
         * @return json
         */
        @Nullable
        @Unmodifiable
        public List<BlueprintJson> buildModernJson(
                @NotNull ModelBlueprint parent
        ) {
            var scale = scale();
            var list = mapIndexed(
                    group(
                            filterIsInstance(children, BlueprintElement.class),
                            BlueprintElement::identifierDegree
                    ),
                    (i, entry) -> buildJson(0, i + 1, scale, parent, entry.getKey(), entry.getValue().stream())
            ).filter(Objects::nonNull)
                    .toList();
            return list.isEmpty() ? null : list;
        }

        private @Nullable BlueprintJson buildJson(
                int tint,
                int number,
                float scale,
                @NotNull ModelBlueprint parent,
                @NotNull Float3 identifier,
                @NotNull Stream<BlueprintElement> cubes
        ) {
            if (parent.textures().isEmpty()) return null;
            var cubeElement = cubes
                    .filter(c -> c.element.hasTexture())
                    .toList();
            if (cubeElement.isEmpty()) return null;
            return new BlueprintJson(jsonName(parent) + "_" + number, JsonObjectBuilder.builder()
                    .jsonObject("textures", textures -> {
                        var index = 0;
                        for (BlueprintTexture texture : parent.textures()) {
                            textures.property(Integer.toString(index++), texture.packName(parent.name()));
                        }
                        textures.property("particle", parent.textures().getFirst().packName(parent.name()));
                    })
                    .jsonArray("elements", mapToJson(cubeElement, cube -> cube.buildJson(tint, scale, parent, this, identifier)))
                    .jsonObject("display", display -> display.jsonObject("fixed", fixed -> {
                        if (!identifier.equals(Float3.ZERO)) {
                            fixed.jsonArray("rotation", identifier.convertToMinecraftDegree().toJson());
                        }
                    }))
                    .build());
        }

        /**
         * Gets cube scale of this model
         * @return scale
         */
        public float scale() {
            return (float) filterIsInstance(children, BlueprintElement.class)
                    .mapToDouble(e -> e.element.max(origin) / 16F)
                    .max()
                    .orElse(1F);
        }

        /**
         * Gets single hit-box
         * @return bounding box
         */
        public @Nullable NamedBoundingBox hitBox() {
            return filterIsInstance(children, BlueprintElement.class).map(be -> {
                var element = be.element;
                var from = element.from()
                        .minus(origin)
                        .toBlockScale()
                        .toVector();
                var to = element.to()
                        .minus(origin)
                        .toBlockScale()
                        .toVector();
                return ModelBoundingBox.of(
                        from.x,
                        from.y,
                        from.z,
                        to.x,
                        to.y,
                        to.z
                ).invert();
            }).max(Comparator.comparingDouble(ModelBoundingBox::length))
                    .map(max -> new NamedBoundingBox(name, max))
                    .orElse(null);
        }
    }

    /**
     * Blueprint element.
     * @param element raw element
     */
    record BlueprintElement(@NotNull ModelElement element) implements BlueprintChildren {

        private @NotNull Float3 identifierDegree() {
            return MathUtil.identifier(element.rotation());
        }

        private static @NotNull Float3 centralize(@NotNull Float3 target, @NotNull Float3 groupOrigin, float scale) {
            return target.minus(groupOrigin).div(scale);
        }

        private static @NotNull Float3 deltaPosition(@NotNull Float3 target, @NotNull Quaternionf quaternionf) {
            return target.rotate(quaternionf).minus(target);
        }

        private @NotNull JsonObject buildJson(
                int tint,
                float scale,
                @NotNull ModelBlueprint parent,
                @NotNull BlueprintGroup group,
                @NotNull Float3 identifier
        ) {
            var qua = MathUtil.toQuaternion(identifier.toVector()).invert();
            var centerOrigin = centralize(element.origin(), group.origin, scale);
            var groupDelta = deltaPosition(centerOrigin, qua);
            var inflate = new Float3(element.inflate() / scale);
            return JsonObjectBuilder.builder()
                    .jsonArray("from", centralize(element.from(), group.origin, scale)
                            .plus(groupDelta)
                            .plus(Float3.CENTER)
                            .minus(inflate)
                            .toJson())
                    .jsonArray("to", centralize(element.to(), group.origin, scale)
                            .plus(groupDelta)
                            .plus(Float3.CENTER)
                            .plus(inflate)
                            .toJson())
                    .jsonObject("faces", Objects.requireNonNull(element.faces()).toJson(parent, tint))
                    .jsonObject("rotation", Optional.of(element.rotation().minus(identifier))
                            .filter(r -> !Float3.ZERO.equals(r))
                            .map(rot -> {
                                var rotation = getRotation(rot);
                                rotation.add("origin", centerOrigin
                                        .plus(groupDelta)
                                        .plus(Float3.CENTER)
                                        .toJson());
                                return rotation;
                            })
                            .orElse(null))
                    .build();
        }

        private @NotNull JsonObject getRotation(@NotNull Float3 rot) {
            var rotation = new JsonObject();
            if (Math.abs(rot.x()) > 0) {
                rotation.addProperty("angle", rot.x());
                rotation.addProperty("axis", "x");
            } else if (Math.abs(rot.y()) > 0) {
                rotation.addProperty("angle", rot.y());
                rotation.addProperty("axis", "y");
            } else if (Math.abs(rot.z()) > 0) {
                rotation.addProperty("angle", rot.z());
                rotation.addProperty("axis", "z");
            }
            return rotation;
        }
    }
}

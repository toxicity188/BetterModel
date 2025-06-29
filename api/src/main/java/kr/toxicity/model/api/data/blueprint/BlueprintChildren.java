package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.raw.Float3;
import kr.toxicity.model.api.data.raw.ModelChildren;
import kr.toxicity.model.api.data.raw.ModelElement;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.PackUtil;
import kr.toxicity.model.api.util.json.JsonObjectBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.*;

/**
 * A children of blueprint (group, element).
 */
public sealed interface BlueprintChildren {

    /**
     * Gets children from raw children
     * @param children raw children
     * @param elementMap element map
     * @return children
     */
    static @NotNull BlueprintChildren from(@NotNull ModelChildren children, @NotNull @Unmodifiable Map<String, ModelElement> elementMap) {
        return switch (children) {
            case ModelChildren.ModelGroup modelGroup -> {
                var child = mapToList(modelGroup.children(), c -> from(c, elementMap));
                yield new BlueprintGroup(
                        BoneTagRegistry.parse(modelGroup.name()),
                        modelGroup.origin(),
                        modelGroup.rotation().invertXZ(),
                        child,
                        filterIsInstance(child, BlueprintElement.class).anyMatch(element -> element.element.visibility())
                );
            }
            case ModelChildren.ModelUUID modelUUID -> new BlueprintElement(Objects.requireNonNull(elementMap.get(modelUUID.uuid())));
        };
    }

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

        private static final JsonArray MAX_SCALE_ARRAY = new JsonArray(3);

        static {
            MAX_SCALE_ARRAY.add(4);
            MAX_SCALE_ARRAY.add(4);
            MAX_SCALE_ARRAY.add(4);
        }

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
         * Gets blueprint json
         * @param scale scale
         * @param parent parent
         * @return json
         */
        public @Nullable BlueprintJson buildJson(
                float scale,
                @NotNull ModelBlueprint parent
        ) {
            return buildJson(-2, 1, scale, parent, Float3.ZERO, filterIsInstance(children, BlueprintElement.class)
                    .filter(filterWithWarning(
                            element -> MathUtil.checkValidDegree(element.identifierDegree()),
                            element -> "The model " + parent.name() + "'s cube \"" + element.element.name() + "\" has an invalid rotation which does not supported in legacy client (<=1.21.3) " + element.element.rotation()
                    ))
            );
        }

        /**
         * Gets blueprint modern json
         * @param scale scale
         * @param parent parent
         * @return json
         */
        @Nullable
        @Unmodifiable
        public List<BlueprintJson> buildModernJson(
                float scale,
                @NotNull ModelBlueprint parent
        ) {
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
            return new BlueprintJson(jsonName(parent) + "_" + number, () -> JsonObjectBuilder.builder()
                    .jsonObject("textures", textures -> {
                        var index = 0;
                        for (BlueprintTexture texture : parent.textures()) {
                            textures.property(Integer.toString(index++), texture.packName(parent.name()));
                        }
                        textures.property("particle", parent.textures().getFirst().packName(parent.name()));
                    })
                    .jsonArray("elements", mapToJson(cubeElement, cube -> cube.buildJson(tint, scale, parent, this, identifier)))
                    .jsonObject("display", display -> display.jsonObject("fixed", fixed -> {
                        fixed.jsonArray("scale", MAX_SCALE_ARRAY);
                        if (!identifier.equals(Float3.ZERO)) {
                            fixed.jsonArray("rotation", identifier.convertToMinecraftDegree().toJson());
                        }
                    }))
                    .build());
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
            var rot = element.rotation();
            return rot == null ? Float3.ZERO : MathUtil.identifier(rot);
        }

        private static @NotNull Float3 centralize(@NotNull Float3 target, @NotNull Float3 groupOrigin, float scale) {
            return target.minus(groupOrigin).div(scale);
        }

        private @NotNull JsonObject buildJson(
                int tint,
                float scale,
                @NotNull ModelBlueprint parent,
                @NotNull BlueprintGroup group,
                @NotNull Float3 identifier
        ) {
            var centerOrigin = centralize(element.origin(), group.origin, scale);
            var qua = MathUtil.toQuaternion(identifier.toVector()).invert();
            var rotOrigin = centerOrigin
                    .rotate(qua)
                    .minus(centerOrigin);
            var inflate = new Float3(element.inflate() / scale);
            return JsonObjectBuilder.builder()
                    .jsonArray("from", centralize(element.from(), group.origin, scale)
                            .plus(rotOrigin)
                            .plus(Float3.CENTER)
                            .minus(inflate)
                            .toJson())
                    .jsonArray("to", centralize(element.to(), group.origin, scale)
                            .plus(rotOrigin)
                            .plus(Float3.CENTER)
                            .plus(inflate)
                            .toJson())
                    .jsonObject("faces", element.faces().toJson(parent, tint))
                    .jsonObject("rotation", Optional.ofNullable(element.rotation())
                            .map(r -> r.minus(identifier))
                            .filter(r -> !Float3.ZERO.equals(r))
                            .map(rot -> {
                                var rotation = getRotation(rot);
                                rotation.add("origin", centerOrigin
                                        .plus(rotOrigin)
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

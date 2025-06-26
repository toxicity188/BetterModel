package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTagRegistry;
import kr.toxicity.model.api.data.raw.Float3;
import kr.toxicity.model.api.data.raw.ModelChildren;
import kr.toxicity.model.api.data.raw.ModelElement;
import kr.toxicity.model.api.util.EntityUtil;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.PackUtil;
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
    static BlueprintChildren from(@NotNull ModelChildren children, @NotNull @Unmodifiable Map<String, ModelElement> elementMap) {
        return switch (children) {
            case ModelChildren.ModelGroup modelGroup -> {
                var child = mapToList(modelGroup.children(), c -> from(c, elementMap));
                yield new BlueprintGroup(
                        modelGroup.name(),
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
            @NotNull String name,
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
         * Creates bone name
         * @return bone name
         */
        public @NotNull BoneName boneName() {
            return BoneTagRegistry.parse(name);
        }

        /**
         * Gets JSON name of blueprint.
         * @param parent parent
         * @return name
         */
        public @NotNull String jsonName(@NotNull ModelBlueprint parent) {
            return PackUtil.toPackName(parent.name() + "_" + name);
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
                    .filter(element -> MathUtil.checkValidDegree(element.identifierDegree())));
        }

        /**
         * Gets blueprint modern json
         * @param scale scale
         * @param parent parent
         * @return json
         */
        public @Nullable List<BlueprintJson> buildModernJson(
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
            var texturedCube = cubes
                    .filter(c -> c.element.hasTexture())
                    .toList();
            if (texturedCube.isEmpty()) return null;
            return new BlueprintJson(jsonName(parent) + "_" + number, () -> {
                var object = new JsonObject();
                var textureObject = new JsonObject();
                var index = 0;
                for (BlueprintTexture texture : parent.textures()) {
                    textureObject.addProperty(Integer.toString(index++), texture.packName(parent.name()));
                }
                textureObject.addProperty("particle", parent.textures().getFirst().packName(parent.name()));
                object.add("textures", textureObject);
                var elements = new JsonArray();
                for (BlueprintElement cube : texturedCube) {
                    cube.buildJson(tint, scale, parent, this, identifier, elements);
                }
                if (elements.isEmpty()) return null;
                object.add("elements", elements);
                var display = new JsonObject();
                var fixed = new JsonObject();
                fixed.add("scale", MAX_SCALE_ARRAY);
                if (!identifier.equals(Float3.ZERO)) fixed.add("rotation", identifier.convertToMinecraftDegree().toJson());
                display.add("fixed", fixed);
                object.add("display", display);
                return object;
            });
        }

        /**
         * Gets single hit-box
         * @return bounding box
         */
        public @Nullable NamedBoundingBox hitBox() {
            var max = EntityUtil.max(filterIsInstance(children, BlueprintElement.class).map(be -> {
                var element = be.element;
                var from = element.from()
                        .minus(origin)
                        .toVector()
                        .div(16);
                var to = element.to()
                        .minus(origin)
                        .toVector()
                        .div(16);
                return ModelBoundingBox.of(
                        from.x,
                        from.y,
                        from.z,
                        to.x,
                        to.y,
                        to.z
                ).invert();
            }).toList());
            return max != null ? new NamedBoundingBox(boneName(), max) : null;
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

        private void buildJson(
                int tint,
                float scale,
                @NotNull ModelBlueprint parent,
                @NotNull BlueprintGroup group,
                @NotNull Float3 identifier,
                @NotNull JsonArray targetArray
        ) {
            var centerOrigin = centralize(element.origin(), group.origin, scale);
            var qua = MathUtil.toQuaternion(identifier.toVector()).invert();
            var rotOrigin = centerOrigin
                    .rotate(qua)
                    .minus(centerOrigin);
            var object = new JsonObject();
            var inflate = new Float3(element.inflate() / scale);
            object.add("from", centralize(element.from(), group.origin, scale)
                    .plus(rotOrigin)
                    .plus(Float3.CENTER)
                    .minus(inflate)
                    .toJson());
            object.add("to", centralize(element.to(), group.origin, scale)
                    .plus(rotOrigin)
                    .plus(Float3.CENTER)
                    .plus(inflate)
                    .toJson());
            var rot = element.rotation();
            if (rot != null) {
                rot = rot.minus(identifier);
                if (!Float3.ZERO.equals(rot)) {
                    var rotation = getRotation(rot);
                    rotation.add("origin", centerOrigin
                            .plus(rotOrigin)
                            .plus(Float3.CENTER)
                            .toJson());
                    object.add("rotation", rotation);
                }
            }
            object.add("faces", element.faces().toJson(parent, tint));
            targetArray.add(object);
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

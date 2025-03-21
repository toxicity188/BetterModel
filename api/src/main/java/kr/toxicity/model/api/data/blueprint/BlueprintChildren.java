package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.toxicity.model.api.BetterModel;
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

/**
 * A children of blueprint (group, element).
 */
public sealed interface BlueprintChildren {

    static BlueprintChildren from(@NotNull ModelChildren children, @NotNull @Unmodifiable Map<String, ModelElement> elementMap, float scale) {
        return switch (children) {
            case ModelChildren.ModelGroup modelGroup -> {
                PackUtil.validatePath(modelGroup.name(), "Group name must be [a-z0-9/._-]: " + modelGroup.name());
                yield new BlueprintGroup(
                    modelGroup.name(),
                    modelGroup.origin(),
                    modelGroup.rotation(),
                    modelGroup.children().stream().map(c -> from(c, elementMap, scale)).toList(),
                    modelGroup.visibility()
                );
            }
            case ModelChildren.ModelUUID modelUUID -> new BlueprintElement(Objects.requireNonNull(elementMap.get(modelUUID.uuid())), scale);
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

        /**
         * Gets json name of blueprint.
         * @param parent parent
         * @return name
         */
        public @NotNull String jsonName(@NotNull ModelBlueprint parent) {
            return parent.name().toLowerCase() + "_" + name.toLowerCase();
        }

        /**
         * Gets blueprint json
         * @param parent parent
         * @return json
         */
        public BlueprintJson buildJson(
                @NotNull ModelBlueprint parent
        ) {
            var list = new ArrayList<BlueprintElement>();
            for (BlueprintChildren child : children) {
                if (child instanceof BlueprintElement element) {
                    list.add(element);
                }
            }
            return buildJson(-2, 1, parent, Float3.ZERO, list);
        }

        /**
         * Gets blueprint modern json
         * @param parent parent
         * @return json
         */
        public List<BlueprintJson> buildModernJson(
                @NotNull ModelBlueprint parent
        ) {
            var list = new ArrayList<BlueprintJson>();
            var floatMap = new HashMap<Float3, List<BlueprintElement>>();
            for (BlueprintChildren child : children) {
                if (child instanceof BlueprintElement element) {
                    floatMap.computeIfAbsent(element.identifierDegree(), u -> new ArrayList<>()).add(element);
                }
            }
            var i = 0;
            for (Map.Entry<Float3, List<BlueprintElement>> entry : floatMap.entrySet()) {
                list.add(buildJson(0, ++i, parent, entry.getKey(), entry.getValue()));
            }
            return list;
        }

        private BlueprintJson buildJson(
                int tint,
                int number,
                @NotNull ModelBlueprint parent,
                @NotNull Float3 identifier,
                @NotNull List<BlueprintElement> cubes
        ) {
            var object = new JsonObject();
            var textureObject = new JsonObject();
            var index = 0;
            for (BlueprintTexture texture : parent.textures()) {
                textureObject.addProperty(Integer.toString(index++), BetterModel.inst().configManager().namespace() + ":item/" + parent.name() + "_" + texture.name());
            }
            object.add("textures", textureObject);
            var elements = new JsonArray();
            for (BlueprintElement cube : cubes) {
                cube.buildJson(tint, parent, this, identifier, elements);
            }
            if (elements.isEmpty()) return null;
            object.add("elements", elements);
            if (!identifier.equals(Float3.ZERO)) {
                var display = new JsonObject();
                var fixed = new JsonObject();
                fixed.add("rotation", identifier.convertToMinecraftDegree().toJson());
                display.add("fixed", fixed);
                object.add("display", display);
            }
            return new BlueprintJson(jsonName(parent) + "_" + number, object);
        }

        /**
         * Gets single hit-box
         * @return bounding box
         */
        public @Nullable NamedBoundingBox hitBox() {
            var elements = new ArrayList<ModelBoundingBox>();
            for (BlueprintChildren child : children) {
                if (child instanceof BlueprintElement element) {
                    var model = element.element;
                    var from = model.from()
                            .minus(origin)
                            .toVector()
                            .div(16);
                    var to = model.to()
                            .minus(origin)
                            .toVector()
                            .div(16);
                    elements.add(new ModelBoundingBox(
                            from.x,
                            from.y,
                            from.z,
                            to.x,
                            to.y,
                            to.z
                    ));
                }
            }
            var max = EntityUtil.max(elements);
            return max != null ? new NamedBoundingBox(name, max) : null;
        }
    }

    /**
     * Blueprint element.
     * @param element raw element
     * @param scale display scale
     */
    record BlueprintElement(@NotNull ModelElement element, float scale) implements BlueprintChildren {

        private @NotNull Float3 identifierDegree() {
            var rot = element.rotation();
            return rot == null ? Float3.ZERO : MathUtil.identifier(rot);
        }

        private @NotNull Float3 centralize(@NotNull Float3 target, @NotNull Float3 groupOrigin) {
            return target.minus(groupOrigin).div(scale);
        }

        private void buildJson(
                int tint,
                @NotNull ModelBlueprint parent,
                @NotNull BlueprintGroup group,
                @NotNull Float3 identifier,
                @NotNull JsonArray targetArray
        ) {
            if (!element.hasTexture()) return;
            var centerOrigin = centralize(element.origin(), group.origin);
            var qua = MathUtil.toQuaternion(identifier.toVector()).invert();
            var rotOrigin = centerOrigin
                    .rotate(qua)
                    .minus(centerOrigin);
            var object = new JsonObject();
            var inflate = new Float3(element.inflate() / scale);
            object.add("from", centralize(element.from(), group.origin)
                    .plus(rotOrigin)
                    .plus(Float3.CENTER)
                    .minus(inflate)
                    .toJson());
            object.add("to", centralize(element.to(), group.origin)
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

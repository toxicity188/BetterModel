package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.toxicity.model.api.data.raw.Float3;
import kr.toxicity.model.api.data.raw.ModelChildren;
import kr.toxicity.model.api.data.raw.ModelElement;
import kr.toxicity.model.api.util.EntityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public sealed interface BlueprintChildren {

    static BlueprintChildren from(@NotNull ModelChildren children, @NotNull @Unmodifiable Map<UUID, ModelElement> elementMap, float scale) {
        return switch (children) {
            case ModelChildren.ModelGroup modelGroup -> new BlueprintGroup(
                    modelGroup.name(),
                    modelGroup.origin(),
                    modelGroup.rotation(),
                    modelGroup.children().stream().map(c -> from(c, elementMap, scale)).toList(),
                    modelGroup.visibility()
            );
            case ModelChildren.ModelUUID modelUUID -> new BlueprintElement(Objects.requireNonNull(elementMap.get(modelUUID.uuid())), scale);
        };
    }

    record BlueprintGroup(
            @NotNull String name,
            @NotNull Float3 origin,
            @NotNull Float3 rotation,
            @NotNull List<BlueprintChildren> children,
            boolean visibility
    ) implements BlueprintChildren {

        public @NotNull String jsonName(@NotNull ModelBlueprint parent) {
            return parent.name() + "_" + name;
        }

        public BlueprintJson buildJson(
                int tint,
                @NotNull ModelBlueprint parent
        ) {
            var object = new JsonObject();
            var textureObject = new JsonObject();
            var index = 0;
            for (BlueprintTexture texture : parent.textures()) {
                textureObject.addProperty(Integer.toString(index++), "bettermodel:item/" + parent.name() + "_" + texture.name());
            }
            object.add("textures", textureObject);
            var elements = new JsonArray();
            for (BlueprintChildren child : children) {
                if (child instanceof BlueprintElement element) {
                    element.buildJson(tint, parent, this, elements);
                }
            }
            if (elements.isEmpty()) return null;
            object.add("elements", elements);
            return new BlueprintJson(jsonName(parent), object);
        }

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

    record BlueprintElement(@NotNull ModelElement element, float scale) implements BlueprintChildren {
        private void buildJson(
                int tint,
                @NotNull ModelBlueprint parent,
                @NotNull BlueprintGroup group,
                @NotNull JsonArray targetArray
        ) {
            if (!element.hasTexture()) return;
            var object = new JsonObject();
            var origin = element.origin()
                    .minus(group.origin)
                    .div(scale)
                    .plus(Float3.CENTER);
            var inflate = new Float3(element.inflate(), element.inflate(), element.inflate()).div(scale);
            object.add("from", element.from()
                    .minus(group.origin)
                    .div(scale)
                    .plus(Float3.CENTER)
                    .minus(origin)
                    .minus(inflate)
                    .plus(origin)
                    .toJson());
            object.add("to", element.to()
                    .minus(group.origin)
                    .div(scale)
                    .plus(Float3.CENTER)
                    .minus(origin)
                    .plus(inflate)
                    .plus(origin)
                    .toJson());
            var rot = element.rotation();
            if (rot != null) {
                var rotation = getRotation(rot);
                rotation.add("origin", origin.toJson());
                object.add("rotation", rotation);
            }
            object.add("faces", element.faces().toJson(parent.resolution(), tint));
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

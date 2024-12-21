package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.toxicity.model.api.data.raw.Float3;
import kr.toxicity.model.api.data.raw.ModelChildren;
import kr.toxicity.model.api.data.raw.ModelElement;
import kr.toxicity.model.api.util.EntityUtil;
import kr.toxicity.model.api.util.VectorPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3f;

import java.util.*;

public sealed interface BlueprintChildren {

    static BlueprintChildren from(@NotNull ModelChildren children, @NotNull @Unmodifiable Map<UUID, ModelElement> elementMap) {
        return switch (children) {
            case ModelChildren.ModelGroup modelGroup -> new BlueprintGroup(
                    modelGroup.name(),
                    modelGroup.origin(),
                    modelGroup.rotation(),
                    modelGroup.children().stream().map(c -> from(c, elementMap)).toList()
            );
            case ModelChildren.ModelUUID modelUUID -> new BlueprintElement(Objects.requireNonNull(elementMap.get(modelUUID.uuid())));
        };
    }

    record BlueprintGroup(
            @NotNull String name,
            @NotNull Float3 origin,
            @NotNull Float3 rotation,
            @NotNull List<BlueprintChildren> children
    ) implements BlueprintChildren {

        public @NotNull String jsonName(@NotNull ModelBlueprint parent) {
            return parent.name() + "_" + name;
        }

        public boolean buildJson(
                int tint,
                @NotNull ModelBlueprint parent,
                @NotNull List<BlueprintJson> list
        ) {
            var object = new JsonObject();
            var textureObject = new JsonObject();
            var index = 0;
            for (BlueprintTexture texture : parent.textures()) {
                textureObject.addProperty(Integer.toString(index++), "modelrenderer:item/" + parent.name() + "_" + texture.name());
            }
            object.add("textures", textureObject);
            var elements = new JsonArray();
            for (BlueprintChildren child : children) {
                switch (child) {
                    case BlueprintElement blueprintElement -> blueprintElement.buildJson(tint, parent, this, elements);
                    case BlueprintGroup blueprintGroup -> blueprintGroup.buildJson(tint, parent, list);
                }
            }
            if (elements.isEmpty()) return false;
            object.add("elements", elements);
            list.add(new BlueprintJson(jsonName(parent), object));
            return true;
        }


        public @NotNull Map<String, NamedBoundingBox> boxes(float scale) {
            var map = new HashMap<String, NamedBoundingBox>();
            var elements = new ArrayList<VectorPair>();
            for (BlueprintChildren child : children) {
                switch (child) {
                    case BlueprintGroup group -> map.putAll(group.boxes(scale));
                    case BlueprintElement element -> {
                        var model = element.element;
                        elements.add(new VectorPair(
                                model.from()
                                        .toVector()
                                        .div(16)
                                        .mul(scale),
                                model.to()
                                        .toVector()
                                        .div(16)
                                        .mul(scale)
                        ));
                    }
                }
            }
            map.put(name, EntityUtil.box(name, elements));
            return map;
        }
    }

    record BlueprintElement(@NotNull ModelElement element) implements BlueprintChildren {
        private void buildJson(
                int tint,
                @NotNull ModelBlueprint parent,
                @NotNull BlueprintGroup group,
                @NotNull JsonArray targetArray
        ) {
            if (!element.hasTexture()) return;
            var object = new JsonObject();
            object.add("from", element.from()
                    .minus(group.origin)
                    .div((float) parent.scale())
                    .plus(Float3.CENTER)
                    .toJson());
            object.add("to", element.to()
                    .minus(group.origin)
                    .div((float) parent.scale())
                    .plus(Float3.CENTER)
                    .toJson());
            var rot = element.rotation();
            if (rot != null && rot.absMax() > 0) {
                var rotation = getRotation(rot);
                rotation.add("origin", element.origin()
                        .minus(group.origin)
                        .div((float) parent.scale())
                        .plus(Float3.CENTER)
                        .toJson());
                object.add("rotation", rotation);
            }
            object.add("faces", element.faces().toJson(parent.resolution(), tint));
            targetArray.add(object);
        }

        private @NotNull JsonObject getRotation(@NotNull Float3 rot) {
            var rotation = new JsonObject();
            if (Math.abs(rot.x()) >= 22.5) {
                rotation.addProperty("angle", rot.x());
                rotation.addProperty("axis", "x");
            } else if (Math.abs(rot.y()) >= 22.5) {
                rotation.addProperty("angle", rot.y());
                rotation.addProperty("axis", "y");
            } else if (Math.abs(rot.z()) >= 22.5) {
                rotation.addProperty("angle", rot.z());
                rotation.addProperty("axis", "z");
            }
            return rotation;
        }
    }
}

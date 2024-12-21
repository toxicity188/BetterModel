package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.toxicity.model.api.data.raw.Float3;
import kr.toxicity.model.api.data.raw.ModelChildren;
import kr.toxicity.model.api.data.raw.ModelElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
            var scale = (float) parent.scale();
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
            if (rot != null && rot.absMax() > 0) {
                var rotation = getRotation(rot);
                rotation.add("origin", origin.toJson());
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

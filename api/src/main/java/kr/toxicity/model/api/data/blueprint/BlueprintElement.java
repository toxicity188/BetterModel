/*
 * This source file is part of BetterModel.
 * Copyright (c) 2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonObject;
import io.github.toxicity188.javamesh.MeshBuilder;
import io.github.toxicity188.javamesh.MeshPoint;
import io.github.toxicity188.javamesh.MeshShape;
import kr.toxicity.model.api.bone.BoneIKChainConfig;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTags;
import kr.toxicity.model.api.data.Float2;
import kr.toxicity.model.api.data.Float3;
import kr.toxicity.model.api.pack.PackObfuscator;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.PackUtil;
import kr.toxicity.model.api.util.json.JsonObjectBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Quaternionf;

import java.util.*;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.*;

/**
 * Represents a processed element within a model blueprint.
 * <p>
 * This is a sealed interface with implementations for different types of elements like bones, groups, cubes, and locators.
 * </p>
 *
 * @since 1.15.2
 */
public sealed interface BlueprintElement {

    String MESH_TRIANGLE_SINGLE = "mesh_triangle_single";

    String MESH_TRIANGLE_DUPLEX = "mesh_triangle_duplex";

    String MESH_PIXEL = "mesh_pixel";

    /**
     * Represents an element that acts as a bone in the model's armature.
     *
     * @since 1.15.2
     */
    sealed interface Bone extends BlueprintElement {

        /**
         * Returns the UUID of the bone.
         *
         * @return the UUID
         * @since 1.15.2
         */
        @NotNull UUID uuid();

        /**
         * Returns the name of the bone.
         *
         * @return the bone name
         * @since 1.15.2
         */
        @NotNull BoneName name();

        /**
         * Returns the origin (pivot point) of the bone.
         *
         * @return the origin
         * @since 1.15.2
         */
        default @NotNull Float3 origin() {
            return Float3.ZERO;
        }

        /**
         * Calculates the bounding box for this bone to be used as a hitbox.
         *
         * @return the named bounding box, or null if no cubes are present
         * @since 3.6.0
         */
        default @Nullable ModelBoundingBox hitbox() {
            return null;
        }
    }

    /**
     * Returns the rotation of the element.
     *
     * @return the rotation, defaulting to zero
     * @since 1.15.2
     */
    default @NotNull Float3 rotation() {
        return Float3.ZERO;
    }

    /**
     * Checks if the element is visible.
     *
     * @return true if visible, false otherwise
     * @since 1.15.2
     */
    default boolean visibility() {
        return false;
    }

    /**
     * Represents a group of elements, forming a bone in the hierarchy.
     *
     * @param uuid the UUID of the group
     * @param name the name of the group/bone
     * @param origin the pivot point of the group
     * @param rotation the rotation of the group
     * @param children the list of child elements
     * @param visibility whether the group is visible
     * @since 1.15.2
     */
    record Group(
        @NotNull UUID uuid,
        @NotNull BoneName name,
        @NotNull Float3 origin,
        @NotNull Float3 rotation,
        @NotNull List<BlueprintElement> children,
        boolean visibility
    ) implements Bone {

        /**
         * Returns the origin with inverted X and Z axes.
         *
         * @return the inverted origin
         * @since 1.15.2
         */
        @Override
        @NotNull
        public Float3 origin() {
            return origin.invertXZ();
        }

        /**
         * Gets JSON name of this element
         * @param context context
         * @return the JSON name
         */
        public @NotNull String jsonName(@NotNull BlueprintLoadContext context) {
            return PackUtil.toPackName(context.name() + "_" + name.rawName());
        }

        /**
         * Builds item model JSON for modern clients, grouping textured cubes by compatible display rotations.
         * <pre>{@code
         * var models = group.buildModernJson(obfuscator, context);
         * }</pre>
         *
         * @param obfuscator the obfuscator for model and texture names
         * @param context the load context
         * @return a list of generated blueprint JSONs, or null if no cubes have textured faces
         * @since 1.15.2
         */
        @Nullable
        @Unmodifiable
        public List<BlueprintJson> buildModernJson(
            @NotNull PackObfuscator.Pair obfuscator,
            @NotNull BlueprintLoadContext context
        ) {
            var scale = scale();
            var list = mapIndexed(
                CubeRotationGroups.group(
                    filterIsInstance(children, Cube.class)
                        .filter(cube -> cube.faces().textures().findAny().isPresent())
                        .toList()),
                (i, model) -> buildJson(i + 1, scale, obfuscator, context, model)
            ).toList();
            return list.isEmpty() ? null : list;
        }

        /**
         * Builds the JSON representation for a mesh-based item model.
         *
         * @param context the load context
         * @return the generated mesh JSON, or null if no meshes are present
         * @since 3.0.0
         */
        public @Nullable JsonObject buildMeshItemModel(
            @NotNull BlueprintLoadContext context
        ) {
            var scale = 1F / scale();
            var meshes = filterIsInstance(children, Mesh.class).toList();
            if (meshes.isEmpty()) return null;
            var builder = MeshBuilder.of(context.triangleName())
                .matrixModifier(mat -> mat.scale(scale))
                .image(context.imageByIndex());
            meshes.forEach(mesh -> builder.load(mesh.toShape(origin)));
            return builder.toJson();
        }

        private @NotNull BlueprintJson buildJson(
            int number,
            float scale,
            @NotNull PackObfuscator.Pair obfuscator,
            @NotNull BlueprintLoadContext context,
            @NotNull CubeRotationGroups.Model model
        ) {
            var selectedTextures = model.elements().stream()
                .flatMapToInt(element -> element.cube().faces().textures())
                .distinct()
                .sorted()
                .mapToObj(i -> Map.entry(Integer.toString(i), context.texture(i).packNamespace(obfuscator.textures())))
                .toList();
            var forceGlow = name().tagged(BoneTags.GLOW);
            var identifier = model.identifier();
            var inverse = identifier.invert(new Quaternionf());
            var displayRotation = identifier.x() == 0F && identifier.y() == 0F && identifier.z() == 0F
                ? null : MathUtil.toMinecraftDegree(identifier);
            return new BlueprintJson(obfuscator.models().obfuscate(jsonName(context) + "_" + number), () -> JsonObjectBuilder.builder()
                .jsonObject("textures", textures -> textures
                    .stringProperties(selectedTextures)
                    .property("particle", selectedTextures.getFirst().getValue()))
                .jsonArray("elements", model.elements().stream()
                    .flatMap(element -> element.cube().buildJson(context, scale, origin, inverse, element.rotation(), forceGlow))
                    .collect(toJsonArray()))
                .jsonObject("display", display -> display.jsonObject("fixed", fixed -> {
                    if (displayRotation != null) {
                        fixed.jsonArray("rotation", displayRotation.toJson());
                    }
                }))
                .build());
        }

        /**
         * Calculates the required scale for the cubes in this group.
         *
         * @return the scale factor
         * @since 1.15.2
         */
        public float scale() {
            return (float) Math.max(filterIsInstance(children, Cube.class)
                .mapToDouble(e -> e.max(origin) / 16F)
                .max()
                .orElse(1F), 1F);
        }

        /**
         * Calculates the bounding box for this group to be used as a hitbox.
         *
         * @return the named bounding box, or null if no cubes are present
         * @since 1.15.2
         */
        public @Nullable ModelBoundingBox hitbox() {
            return filterIsInstance(children, Cube.class)
                .map(element -> ModelBoundingBox.of(
                    element.from().minus(origin).toBlockScale(),
                    element.to().minus(origin).toBlockScale()
                ).invert())
                .max(Comparator.comparingDouble(ModelBoundingBox::length))
                .orElse(null);
        }
    }

    /**
     * Represents a locator element, used as a named attachment point.
     *
     * @param uuid the UUID of the locator
     * @param name the name of the locator
     * @param origin the position of the locator
     * @since 1.15.2
     */
    record Locator(
        @NotNull UUID uuid,
        @NotNull BoneName name,
        @NotNull Float3 origin
    ) implements Bone {
        /**
         * Returns the origin with inverted X and Z axes.
         *
         * @return the inverted origin
         * @since 1.15.2
         */
        @Override
        @NotNull
        public Float3 origin() {
            return origin.invertXZ();
        }
    }

    /**
     * Represents a camera element (currently a placeholder).
     *
     * @param uuid the UUID of the camera
     * @since 1.15.2
     */
    record Camera(
        @NotNull UUID uuid
    ) implements BlueprintElement {
    }

    /**
     * Represents a null object, often used for IK or as a simple bone.
     *
     * @param uuid the UUID of the null object
     * @param name the name of the null object
     * @param config the ik chain config of the null object
     * @param origin the position of the null object
     * @since 1.15.2
     */
    record NullObject(
        @NotNull UUID uuid,
        @NotNull BoneName name,
        @Nullable BoneIKChainConfig config,
        @NotNull Float3 origin
    ) implements Bone {
        /**
         * Returns the origin with inverted X and Z axes.
         *
         * @return the inverted origin
         * @since 1.15.2
         */
        @Override
        @NotNull
        public Float3 origin() {
            return origin.invertXZ();
        }
    }

    /**
     * Represents a cube element, the basic building block of a model.
     *
     * @param name the name of the cube
     * @param from the starting coordinate (min corner)
     * @param to the ending coordinate (max corner)
     * @param inflate the inflation value
     * @param rotation the rotation of the cube
     * @param origin the pivot point of the cube
     * @param faces the UV mapping for the faces
     * @param lightEmission the light emission level (1-15 or null if 0)
     * @param visibility whether the cube is visible
     * @since 1.15.2
     */
    record Cube(
        @NotNull String name,
        @NotNull Float3 from,
        @NotNull Float3 to,
        float inflate,
        @NotNull Float3 rotation,
        @NotNull Float3 origin,
        @NotNull BlueprintFace faces,
        @Nullable Integer lightEmission,
        boolean visibility
    ) implements BlueprintElement {

        private static @NotNull Float3 centralize(@NotNull Float3 target, @NotNull Float3 groupOrigin, float scale) {
            return target.minus(groupOrigin).div(scale);
        }

        private static @NotNull Float3 translatedOrigin(@NotNull Float3 origin, @NotNull Quaternionf inverseIdentifier) {
            return origin.rotate(inverseIdentifier).minus(origin).plus(Float3.CENTER);
        }

        private @NotNull Stream<JsonObject> buildJson(
            @NotNull BlueprintLoadContext context,
            float scale,
            @NotNull Float3 groupOrigin,
            @NotNull Quaternionf inverseIdentifier,
            @NotNull Float3 relativeRotation,
            boolean forceGlow
        ) {
            var centerOrigin = centralize(origin(), groupOrigin, scale);
            var relativeOrigin = translatedOrigin(centerOrigin, inverseIdentifier);
            var inflate = new Float3(inflate() / scale);

            var from = centralize(from(), groupOrigin, scale)
                .plus(relativeOrigin)
                .minus(inflate)
                .toJson();
            var to = centralize(to(), groupOrigin, scale)
                .plus(relativeOrigin)
                .plus(inflate)
                .toJson();
            var rotation = Float3.ZERO.equals(relativeRotation)
                ? null
                : parseToRotation(relativeRotation, centerOrigin.plus(relativeOrigin));

            return group(faces().uv(), uv -> forceGlow || context.texture(uv.texture()).isGlowingTexture())
                .entrySet()
                .stream()
                .map(entry -> {
                    var face = BlueprintUV.toJson(entry.getValue(), uv -> context.resolution(uv.texture()));
                    if (face == null) return null;
                    return JsonObjectBuilder.builder()
                        .property("light_emission", entry.getKey() ? Integer.valueOf(15) : lightEmission)
                        .jsonArray("from", from)
                        .jsonArray("to", to)
                        .jsonObject("faces", face)
                        .jsonObject("rotation", rotation)
                        .build();
                })
                .filter(Objects::nonNull);
        }

        /**
         * Calculates the maximum distance from the origin to any corner of the cube.
         *
         * @param origin the reference origin
         * @return the maximum length
         * @since 1.15.2
         */
        public float max(@NotNull Float3 origin) {
            var f = from().minus(origin);
            var t = to().minus(origin);
            var max = 0F;
            max = Math.max(max, Math.abs(f.x()));
            max = Math.max(max, Math.abs(f.y()));
            max = Math.max(max, Math.abs(f.z()));
            max = Math.max(max, Math.abs(t.x()));
            max = Math.max(max, Math.abs(t.y()));
            max = Math.max(max, Math.abs(t.z()));
            return max;
        }

        private static @NotNull JsonObject parseToRotation(@NotNull Float3 rot, @NotNull Float3 origin) {
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
            rotation.add("origin", origin.toJson());
            return rotation;
        }
    }

    /**
     * Represents a mesh element, allowing for complex geometry beyond simple cubes.
     *
     * @param origin the pivot point of the mesh
     * @param rotation the rotation of the mesh
     * @param faces the list of faces forming the mesh
     * @param visibility whether the mesh is visible
     * @since 3.0.0
     */
    record Mesh(
        @NotNull Float3 origin,
        @NotNull Float3 rotation,
        @NotNull List<Face> faces,
        boolean visibility
    ) implements BlueprintElement {

        /**
         * Converts this mesh into a list of {@link MeshShape} for rendering.
         *
         * @param parentOrigin the origin of the parent bone
         * @return an unmodifiable list of mesh shapes
         * @since 3.0.0
         */
        @NotNull
        @Unmodifiable
        public List<MeshShape> toShape(@NotNull Float3 parentOrigin) {
            var deltaOrigin = origin().minus(parentOrigin).toVector();
            var pointRotation = rotation().toQuaternionXYZ();
            return faces.stream()
                .map(face -> new MeshShape(
                    face.points.stream()
                        .map(p -> new MeshPoint(
                            p.vertices.toVector()
                                .rotate(pointRotation)
                                .add(deltaOrigin)
                                .mul(-1F, 1F, -1F)
                                .div(MathUtil.MODEL_TO_BLOCK_MULTIPLIER),
                            p.uv.toVector()
                                .div(MathUtil.MODEL_TO_BLOCK_MULTIPLIER)
                        ))
                        .toList(),
                    Integer.toString(face.texture)
                ))
                .toList();
        }

        /**
         * Represents a single face of a mesh.
         *
         * @param points the vertices and UV coordinates of the face
         * @param texture the index of the texture used by this face
         * @since 3.0.0
         */
        public record Face(@NotNull @Unmodifiable List<Point> points, int texture) {}

        /**
         * Represents a single point (vertex) in a mesh face.
         *
         * @param vertices the 3D coordinates of the vertex
         * @param uv the 2D UV coordinates for texture mapping
         * @since 3.0.0
         */
        public record Point(@NotNull Float3 vertices, @NotNull Float2 uv) {}
    }

    /**
     * Represents a bounding box.
     *
     * @param uuid the UUID of the bounding box
     * @param name the name of the bounding box
     * @param hitbox the hitbox of the bounding box
     * @since 3.6.0
     */
    record BoundingBox(
        @NotNull UUID uuid,
        @NotNull BoneName name,
        @NotNull ModelBoundingBox hitbox
    ) implements Bone {
    }
}

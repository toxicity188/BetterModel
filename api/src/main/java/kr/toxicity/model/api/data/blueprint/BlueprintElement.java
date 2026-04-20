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
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTags;
import kr.toxicity.model.api.data.Float2;
import kr.toxicity.model.api.data.Float3;
import kr.toxicity.model.api.data.raw.ModelFace;
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
        @NotNull Float3 origin();
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

        private @NotNull String jsonName(@NotNull BlueprintLoadContext context) {
            return PackUtil.toPackName(context.name() + "_" + name.rawName());
        }

        /**
         * Builds the JSON representation for legacy clients (1.21.3 or under).
         *
         * @param obfuscator the obfuscator for model and texture names
         * @param context the load context
         * @return the generated blueprint JSON, or null if not applicable
         * @since 1.15.2
         */
        public @Nullable BlueprintJson buildLegacyJson(
            @NotNull PackObfuscator.Pair obfuscator,
            @NotNull BlueprintLoadContext context
        ) {
            return buildJson(-2, 1, scale(), obfuscator, context, Float3.ZERO, filterIsInstance(children, Cube.class).filter(element -> MathUtil.checkValidDegree(element.identifierDegree())));
        }

        /**
         * Builds the JSON representation for modern clients.
         *
         * @param obfuscator the obfuscator for model and texture names
         * @param context the load context
         * @return a list of generated blueprint JSONs, or null if not applicable
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
                group(
                    filterIsInstance(children, Cube.class),
                    Cube::identifierDegree
                ),
                (i, entry) -> buildJson(0, i + 1, scale, obfuscator, context, entry.getKey(), entry.getValue().stream())
            ).filter(Objects::nonNull)
                .toList();
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

        private @Nullable BlueprintJson buildJson(
            int tint,
            int number,
            float scale,
            @NotNull PackObfuscator.Pair obfuscator,
            @NotNull BlueprintLoadContext context,
            @NotNull Float3 identifier,
            @NotNull Stream<Cube> cubes
        ) {
            var cubeElement = cubes
                .filter(Cube::hasTexture)
                .toList();
            var selectedTextures = cubeElement.stream()
                .flatMapToInt(tex -> tex.faces().textureIndex())
                .distinct()
                .sorted()
                .mapToObj(i -> Map.entry(Integer.toString(i), context.texture(i).packNamespace(obfuscator.textures())))
                .toList();
            if (selectedTextures.isEmpty()) return null;
            return new BlueprintJson(obfuscator.models().obfuscate(jsonName(context) + "_" + number), () -> JsonObjectBuilder.builder()
                .jsonObject("textures", textures -> textures
                    .stringProperties(selectedTextures)
                    .property("particle", selectedTextures.getFirst().getValue()))
                .jsonArray("elements", mapToJson(cubeElement, cube -> cube.buildJson(tint, scale, context, this, identifier)))
                .jsonObject("display", display -> display.jsonObject("fixed", fixed -> {
                    if (!identifier.equals(Float3.ZERO)) {
                        fixed.jsonArray("rotation", identifier.convertToMinecraftDegree().toJson());
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
        public @Nullable ModelBoundingBox hitBox() {
            return filterIsInstance(children, Cube.class)
                .map(element -> {
                    var from = element.from()
                        .minus(origin)
                        .toBlockScale();
                    var to = element.to()
                        .minus(origin)
                        .toBlockScale();
                    return ModelBoundingBox.of(
                        from.x(),
                        from.y(),
                        from.z(),
                        to.x(),
                        to.y(),
                        to.z()
                    ).invert();
                })
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
     * @param ikTarget the UUID of the IK target bone
     * @param ikSource the UUID of the IK source bone
     * @param origin the position of the null object
     * @since 1.15.2
     */
    record NullObject(
        @NotNull UUID uuid,
        @NotNull BoneName name,
        @Nullable UUID ikTarget,
        @Nullable UUID ikSource,
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
        @Nullable ModelFace faces,
        @Nullable Integer lightEmission,
        boolean visibility
    ) implements BlueprintElement {

        private @NotNull Float3 identifierDegree() {
            return MathUtil.identifier(rotation());
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
            @NotNull BlueprintLoadContext parent,
            @NotNull BlueprintElement.Group group,
            @NotNull Float3 identifier
        ) {
            var qua = identifier.toQuaternionZYX().invert();
            var centerOrigin = centralize(origin(), group.origin, scale);
            var groupDelta = deltaPosition(centerOrigin, qua);
            var inflate = new Float3(inflate() / scale);
            return JsonObjectBuilder.builder()
                .property("light_emission", group.name.tagged(BoneTags.GLOW) ? Integer.valueOf(15) : lightEmission)
                .jsonArray("from", centralize(from(), group.origin, scale)
                    .plus(groupDelta)
                    .plus(Float3.CENTER)
                    .minus(inflate)
                    .toJson())
                .jsonArray("to", centralize(to(), group.origin, scale)
                    .plus(groupDelta)
                    .plus(Float3.CENTER)
                    .plus(inflate)
                    .toJson())
                .jsonObject("faces", faces().toJson(parent, tint))
                .jsonObject("rotation", Optional.of(rotation().minus(identifier))
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

        @Override
        public @NotNull ModelFace faces() {
            return Objects.requireNonNull(faces);
        }

        /**
         * Checks if this cube has any textures defined.
         *
         * @return true if it has textures, false otherwise
         * @since 1.15.2
         */
        public boolean hasTexture() {
            return faces != null && faces.hasTexture();
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
}

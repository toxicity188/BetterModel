/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelConfig;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.data.Float3;
import kr.toxicity.model.api.data.Float4;
import kr.toxicity.model.api.data.blueprint.BlueprintElement.Cube;
import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.pack.PackObfuscator;
import kr.toxicity.model.api.platform.PlatformAdapter;
import kr.toxicity.model.api.platform.PlatformItemStack;
import kr.toxicity.model.api.util.MathUtil;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CubeRotationGroupsTest {

    private static final Float3 GROUP_ORIGIN = new Float3(1, 2, -3);
    private static final BlueprintFace FACE = new BlueprintFace(
        new BlueprintUV(BlueprintUV.NORTH, new Float4(0, 0, 16, 16), 0, 0), null, null, null, null, null
    );

    @BeforeAll
    static void registerPlatform() {
        var config = stub(BetterModelConfig.class, Map.of("namespace", "bettermodel"));
        var air = stub(PlatformItemStack.class, Map.of());
        var adapter = stub(PlatformAdapter.class, Map.of("air", air));
        BetterModel.register(stub(BetterModelPlatform.class, Map.of("config", config, "adapter", adapter)));
    }

    @Test
    void mergesFourRotationsAroundTheSameAxis() {
        var models = models(cubes(xRotations(22.5F, 45, 67.5F, 90)));
        assertEquals(1, models.size());
        var elements = models.getFirst().getAsJsonArray("elements");
        var angles = IntStream.range(0, elements.size())
            .mapToObj(i -> elementRotation(elements.get(i).getAsJsonObject()).x())
            .toList();
        assertEquals(List.of(-22.5F, 0F, 22.5F, 45F), angles);
    }

    @Test
    void findsMinimumEvenWhenTheLargestCandidateIsSuboptimal() {
        var rotations = cubes(xRotations(0, 45, 67.5F, 90, 112.5F, 135, 180));
        var cubes = Stream.concat(rotations.stream(), rotations.stream()).toList();
        var models = models(cubes);
        assertEquals(2, models.size());
        assertEquals(cubes.size(), models.stream().mapToInt(model -> model.getAsJsonArray("elements").size()).sum());
        assertEquals(models, models(cubes));
    }

    @Test
    void preservesMixedAxisRenderingAndOffCenterPivotsNearGimbalLock() {
        var identifier = new Float3(30, 89.9F, 17).toQuaternionXYZ();
        var cubes = cubes(List.of(
            zyxDegrees(identifier),
            zyxDegrees(identifier.mul(new Float3(0, 22.5F, 0).toQuaternionZYX(), new Quaternionf())),
            zyxDegrees(identifier.mul(new Float3(0, 0, -45).toQuaternionZYX(), new Quaternionf()))
        ));
        var models = models(cubes);
        assertEquals(1, models.size());
        var model = models.getFirst();
        var display = vector(model.getAsJsonObject("display").getAsJsonObject("fixed").getAsJsonArray("rotation"))
            .toQuaternionXYZ();
        var elements = model.getAsJsonArray("elements");
        assertEquals(cubes.size(), elements.size());
        for (var i = 0; i < elements.size(); i++) {
            var cube = cubes.get(i);
            var element = elements.get(i).getAsJsonObject();
            var relative = elementRotation(element);
            assertTrue(MathUtil.checkValidDegree(relative));
            var pivot = element.has("rotation")
                ? vector(element.getAsJsonObject("rotation").getAsJsonArray("origin")) : Float3.ZERO;
            for (var corner : List.of("from", "to")) {
                var actual = vector(element.getAsJsonArray(corner)).minus(pivot)
                    .rotate(relative.toQuaternionZYX()).plus(pivot).minus(Float3.CENTER).rotate(display);
                var point = corner.equals("from") ? cube.from().minus(new Float3(cube.inflate()))
                    : cube.to().plus(new Float3(cube.inflate()));
                var expected = point.minus(cube.origin()).rotate(cube.rotation().toQuaternionZYX())
                    .plus(cube.origin()).minus(GROUP_ORIGIN);
                assertTrue(actual.toVector().distance(expected.toVector()) < 1E-4F,
                    () -> "Expected " + expected + ", got " + actual);
            }
        }
    }

    @Test
    void matchesEquivalentQuaternionsAndWritesExactLegalAngles() {
        var models = models(cubes(xRotations(337.50003F, 360, 382.5F)));
        assertEquals(1, models.size());
        var elements = models.getFirst().getAsJsonArray("elements");
        assertEquals(new Float3(-22.5F, 0, 0), elementRotation(elements.get(0).getAsJsonObject()));
        assertEquals(Float3.ZERO, elementRotation(elements.get(1).getAsJsonObject()));
        assertEquals(new Float3(22.5F, 0, 0), elementRotation(elements.get(2).getAsJsonObject()));
    }

    @Test
    void keepsIncompatibleRotationsSeparate() {
        assertEquals(2, models(cubes(xRotations(0, 91))).size());
        assertEquals(2, models(cubes(xRotations(0, 89.99F))).size());
        assertEquals(2, models(cubes(List.of(Float3.ZERO, new Float3(23, 27, 31)))).size());
    }

    @Test
    void ignoresCubesWithNoTexturedFacesWhenGrouping() {
        var untextured = new Cube("untextured", GROUP_ORIGIN, GROUP_ORIGIN.plus(new Float3(1)), 0,
            new Float3(23, 27, 31), GROUP_ORIGIN, BlueprintFace.EMPTY, null, true);
        assertNull(group(List.of(untextured)).buildModernJson(obfuscator(), context()));
        assertEquals(1, models(List.of(untextured, cubes(xRotations(90)).getFirst())).size());
    }

    private static <T> T stub(Class<T> type, Map<String, ?> results) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (_, method, _) -> {
            if (results.containsKey(method.getName())) return results.get(method.getName());
            throw new UnsupportedOperationException(method.getName());
        }));
    }

    private static List<Float3> xRotations(float... angles) {
        return IntStream.range(0, angles.length).mapToObj(i -> new Float3(angles[i], 0, 0)).toList();
    }

    private static List<Cube> cubes(List<Float3> rotations) {
        return IntStream.range(0, rotations.size()).mapToObj(i -> new Cube("cube_" + i,
            new Float3(i - 2, 1, 3), new Float3(i + 1, 5, 8), 0.25F,
            rotations.get(i), new Float3(i, 4, 4), FACE, null, true)).toList();
    }

    private static List<JsonObject> models(List<Cube> cubes) {
        return group(cubes).buildModernJson(obfuscator(), context()).stream()
            .map(model -> model.buildJson().getAsJsonObject()).toList();
    }

    private static BlueprintElement.Group group(List<Cube> cubes) {
        return new BlueprintElement.Group(new UUID(0, 0), new BoneName(Set.of(), "group", "group"),
            GROUP_ORIGIN, Float3.ZERO, List.copyOf(cubes), true);
    }

    private static PackObfuscator.Pair obfuscator() {
        return PackObfuscator.pair(PackObfuscator.NONE, PackObfuscator.NONE);
    }

    private static BlueprintLoadContext context() {
        var texture = new BlueprintTexture("texture", BlueprintTexture.RenderMode.DEFAULT, new byte[0],
            16, 16, 16, 16, true, 1, false);
        return new BlueprintLoadContext("model", new ModelResolution(16, 16), List.of(texture));
    }

    private static Float3 elementRotation(JsonObject element) {
        if (!element.has("rotation")) return Float3.ZERO;
        var rotation = element.getAsJsonObject("rotation");
        var angle = rotation.get("angle").getAsFloat();
        return switch (rotation.get("axis").getAsString()) {
            case "x" -> new Float3(angle, 0, 0);
            case "y" -> new Float3(0, angle, 0);
            case "z" -> new Float3(0, 0, angle);
            default -> throw new AssertionError(rotation);
        };
    }

    private static Float3 zyxDegrees(Quaternionf rotation) {
        var euler = rotation.getEulerAnglesZYX(new Vector3f()).mul(MathUtil.RADIANS_TO_DEGREES);
        return new Float3(euler.x, euler.y, euler.z);
    }

    private static Float3 vector(JsonArray array) {
        return new Float3(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }
}

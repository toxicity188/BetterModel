/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.pack;

import com.google.gson.JsonObject;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.Float3;
import kr.toxicity.model.api.data.Float4;
import kr.toxicity.model.api.data.blueprint.BlueprintElement;
import kr.toxicity.model.api.util.json.JsonObjectBuilder;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

record PackBuiltInAssets(
    @NotNull String path,
    @NotNull Function<PackZipper, PackBuilder> builderFunction,
    @NotNull Supplier<byte[]> supplier
) {

    private static final byte[] MESH_PIXEL_IMAGE;

    static {
        var image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Arrays.fill(((DataBufferInt) image.getRaster().getDataBuffer()).getData(), 0xFFFFFF);
        try (var output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            MESH_PIXEL_IMAGE = output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final Set<PackBuiltInAssets> ASSETS = Set.of(
        new PackBuiltInAssets(
            BlueprintElement.MESH_TRIANGLE_SINGLE + ".json",
            zipper -> zipper.modern().bettermodel().models(),
            () -> PackMeta.GSON.toJson(meshTriangle(faces -> faces
                .jsonObject("north", north -> north
                    .jsonArray("uv", Float4.MAX_UV.toJson())
                    .property("texture", "#0")
                    .property("tintindex", 0))
            )).getBytes(StandardCharsets.UTF_8)
        ),
        new PackBuiltInAssets(
            BlueprintElement.MESH_TRIANGLE_DUPLEX + ".json",
            zipper -> zipper.modern().bettermodel().models(),
            () -> PackMeta.GSON.toJson(meshTriangle(faces -> faces
                .jsonObject("north", north -> north
                    .jsonArray("uv", Float4.MAX_UV.toJson())
                    .property("texture", "#0")
                    .property("tintindex", 0))
                .jsonObject("south", north -> north
                    .jsonArray("uv", Float4.MAX_UV.toJson())
                    .property("texture", "#0")
                    .property("tintindex", 0))
            )).getBytes(StandardCharsets.UTF_8)
        ),
        new PackBuiltInAssets(
            BlueprintElement.MESH_PIXEL + ".png",
            zipper -> zipper.modern().bettermodel().textures(),
            () -> MESH_PIXEL_IMAGE
        )
    );

    static void applyAs(@NotNull PackZipper zipper) {
        for (PackBuiltInAssets asset : ASSETS) {
            asset.builderFunction.apply(zipper).add(asset.path, asset.supplier);
        }
    }

    private static @NotNull JsonObject meshTriangle(@NotNull Function<JsonObjectBuilder, JsonObjectBuilder> faceBuilder) {
        var pixel = BetterModel.config().namespace() + ":item/" + BlueprintElement.MESH_PIXEL;
        return JsonObjectBuilder.builder()
            .jsonObject("textures", textures -> textures
                .property("0", pixel)
                .property("particle", pixel))
            .jsonArray("elements", elements -> elements
                .jsonObject(element -> element
                    .jsonArray("from", Float3.MESH_TRIANGLE_FROM.toJson())
                    .jsonArray("to", Float3.MESH_TRIANGLE_TO.toJson())
                    .jsonObject("faces", faceBuilder::apply)))
            .build();
    }
}

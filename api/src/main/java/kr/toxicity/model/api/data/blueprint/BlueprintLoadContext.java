/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import io.github.toxicity188.javamesh.MeshImage;
import io.github.toxicity188.javamesh.MeshTriangleName;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.raw.ModelResolution;
import kr.toxicity.model.api.pack.PackObfuscator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A context class for loading blueprints.
 *
 * @since 3.0.0
 */
@ApiStatus.Internal
public final class BlueprintLoadContext {

    private final String name;
    private final ModelResolution resolution;
    private final TextureRef[] textureRefs;
    private final boolean canBeRendered;

    private volatile Map<String, MeshImage> imageRefMap;

    private final MeshTriangleName triangleName = new MeshTriangleName(
        BetterModel.config().namespace() + ":" + BlueprintElement.MESH_TRIANGLE_SINGLE,
        BetterModel.config().namespace() + ":" + BlueprintElement.MESH_TRIANGLE_DUPLEX
    );

    BlueprintLoadContext(
        @NotNull String name,
        @NotNull ModelResolution resolution,
        @NotNull List<BlueprintTexture> textures
    ) {
        this.name = name;
        this.resolution = resolution;
        this.textureRefs = new TextureRef[textures.size()];
        var i = 0;
        var canBeRendered = false;
        for (BlueprintTexture texture : textures) {
            canBeRendered |= texture.canBeRendered();
            this.textureRefs[i++] = new TextureRef(texture);
        }
        this.canBeRendered = canBeRendered;
    }

    /**
     * Gets the name of the blueprint.
     *
     * @return the name
     * @since 3.0.0
     */
    public @NotNull String name() {
        return name;
    }

    /**
     * Gets the mesh triangle name.
     *
     * @return the triangle name
     * @since 3.0.0
     */
    public @NotNull MeshTriangleName triangleName() {
        return triangleName;
    }

    /**
     * Gets the model resolution.
     *
     * @return the resolution
     * @since 3.0.0
     */
    public @NotNull ModelResolution resolution() {
        return resolution;
    }

    /**
     * Gets a texture by its index.
     *
     * @param index the index
     * @return the texture
     * @since 3.0.0
     */
    public @NotNull BlueprintTexture texture(int index) {
        return Objects.requireNonNull(textureRefs[index]).texture();
    }

    @NotNull
    @Unmodifiable
    Map<String, MeshImage> imageByIndex() {
        Map<String, MeshImage> map;
        if ((map = imageRefMap) != null) return map;
        synchronized (this) {
            if ((map = imageRefMap) != null) return map;
            return imageRefMap = Collections.unmodifiableMap(new AbstractMap<>() {
                @Override
                public MeshImage get(Object key) {
                    var get = textureRefs[Integer.parseInt(key.toString())];
                    return get != null ? get.image() : null;
                }

                @Override
                @Unmodifiable
                public @NotNull Set<Entry<String, MeshImage>> entrySet() {
                    return IntStream.range(0, textureRefs.length)
                        .mapToObj(i -> Map.entry(Integer.toString(i), textureRefs[i].image()))
                        .collect(Collectors.toUnmodifiableSet());
                }
            });
        }
    }

    /**
     * Returns whether this context can be rendered.
     *
     * @return true if renderable
     * @since 3.0.0
     */
    public boolean canBeRendered() {
        return canBeRendered;
    }

    /**
     * Builds a stream of blueprint images using the provided obfuscator.
     *
     * @param obfuscator the obfuscator
     * @return a stream of images
     * @since 3.0.0
     */
    @NotNull
    public Stream<BlueprintImage> buildImage(@NotNull PackObfuscator obfuscator) {
        if (!canBeRendered()) return Stream.empty();
        return Arrays.stream(textureRefs)
            .filter(TextureRef::canBeRendered)
            .map(ref -> new BlueprintImage(
                ref.texture.packName(obfuscator),
                ref.texture.image(),
                ref.texture.isAnimatedTexture() ? ref.texture.toMcmeta() : null)
            );
    }

    private static final class TextureRef {

        private final BlueprintTexture texture;
        private final AtomicBoolean referenced = new AtomicBoolean();
        private volatile MeshImage image;

        private TextureRef(BlueprintTexture texture) {
            this.texture = texture;
        }

        public boolean canBeRendered() {
            return referenced.get() && texture.canBeRendered();
        }

        public @NotNull BlueprintTexture texture() {
            referenced.set(true);
            return texture;
        }

        public @NotNull MeshImage image() {
            MeshImage img;
            if ((img = image) != null) return img;
            synchronized (this) {
                if ((img = image) != null) return img;
                try (var input = new ByteArrayInputStream(texture.image())) {
                    return image = MeshImage.from(ImageIO.read(input));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

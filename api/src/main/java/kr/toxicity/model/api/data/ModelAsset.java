/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data;

import com.google.gson.JsonParseException;
import kr.toxicity.model.api.data.raw.ModelData;
import kr.toxicity.model.api.data.raw.ModelLoadResult;
import kr.toxicity.model.api.util.PackUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a raw model asset that can be loaded into the engine.
 * <p>
 * This record encapsulates the source of the model data (e.g., a file or stream), its name, and metadata.
 * It provides methods to load and parse the model data into a usable format.
 * </p>
 *
 * @param rawName the original raw name or path of the asset
 * @param name the sanitized, pack-compliant name of the asset
 * @param sizeAssume the estimated size of the asset in bytes (0 if unknown)
 * @param supplier a supplier for the input stream containing the model data
 * @since 2.0.0
 */
public record ModelAsset(
    @NotNull String rawName,
    @NotNull String name,
    long sizeAssume,
    @NotNull StreamSupplier supplier
) implements Comparable<ModelAsset> {

    /**
     * Internal constructor for ModelAsset.
     */
    @ApiStatus.Internal
    public ModelAsset {
    }

    /**
     * Creates a new ModelAsset from a name and byte array.
     *
     * @param name the name of the asset
     * @param bytes the byte array containing the model data
     * @return the created asset
     * @since 2.0.0
     */
    public static @NotNull ModelAsset of(@NotNull String name, byte[] bytes) {
        return of(name, bytes.length, () -> new ByteArrayInputStream(bytes));
    }

    /**
     * Creates a new ModelAsset from a name and stream supplier.
     *
     * @param name the name of the asset
     * @param supplier the stream supplier
     * @return the created asset
     * @since 2.0.0
     */
    public static @NotNull ModelAsset of(@NotNull String name, @NotNull StreamSupplier supplier) {
        return of(name, 0, supplier); // Unknown size
    }

    /**
     * Creates a new ModelAsset from a name, stream supplier, and estimated size.
     *
     * @param name the name of the asset
     * @param sizeAssume the estimated size in bytes
     * @param supplier the stream supplier
     * @return the created asset
     * @since 2.0.0
     */
    public static @NotNull ModelAsset of(@NotNull String name, long sizeAssume, @NotNull StreamSupplier supplier) {
        PackUtil.assertPackName(name);
        return new ModelAsset(name, name, sizeAssume, supplier);
    }

    /**
     * Creates a new ModelAsset from a file.
     *
     * @param file the source file
     * @return the created asset
     * @since 2.0.0
     */
    public static @NotNull ModelAsset of(@NotNull File file) {
        return new ModelAsset(file.getPath(), nameWithoutExtension(file.getName()), file.length(), () -> new FileInputStream(file));
    }

    /**
     * Creates a new ModelAsset from a path.
     *
     * @param path the source path
     * @return the created asset
     * @throws RuntimeException if an I/O error occurs
     * @since 2.0.0
     */
    public static @NotNull ModelAsset of(@NotNull Path path) {
        try {
            return new ModelAsset(path.toString(), nameWithoutExtension(path.getFileName().toString()), Files.size(path), () -> Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static @NotNull String nameWithoutExtension(@NotNull String name) {
        var index = name.lastIndexOf('.');
        return PackUtil.toPackName(index > 0 ? name.substring(0, index) : name);
    }

    /**
     * Loads and parses the model data from this asset.
     *
     * @return the result of the load operation
     * @throws RuntimeException if an I/O or parsing error occurs
     * @since 2.0.0
     */
    public @NotNull ModelLoadResult toResult() {
        try (
            var stream = supplier.get();
            var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
        ) {
            var result = ModelData.GSON.fromJson(reader, ModelData.class);
            result.assertSupported();
            return result.loadBlueprint(name);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load this asset: " + this, e);
        } catch (JsonParseException e) {
            throw new RuntimeException("Unable to parse this json asset: " + this, e);
        }
    }

    @Override
    public int compareTo(@NonNull ModelAsset o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ModelAsset that)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public @NonNull String toString() {
        return rawName;
    }

    /**
     * A functional interface for supplying an input stream.
     *
     * @since 2.0.0
     */
    public interface StreamSupplier {
        /**
         * Gets the input stream.
         *
         * @return the input stream
         * @throws IOException if an I/O error occurs
         * @since 2.0.0
         */
        @NotNull InputStream get() throws IOException;
    }
}

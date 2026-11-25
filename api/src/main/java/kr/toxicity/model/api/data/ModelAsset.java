/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.raw.ModelLoadResult;
import kr.toxicity.model.api.manager.ModelManager;
import kr.toxicity.model.api.util.PackUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Represents a raw model asset that can be loaded into the engine.
 * <p>
 * This record encapsulates the source of the model data (e.g., a file or stream), its name, and metadata.
 * It provides methods to load and parse the model data into a usable format.
 * </p>
 * <p>Example: {@code ModelAsset.of("model", "custom", bytes).toResult()}.</p>
 *
 * @param rawName the original raw name or path of the asset
 * @param name the sanitized, pack-compliant name of the asset
 * @param extension the model extension without a leading dot, matched without case sensitivity
 * @param sizeAssume the estimated size of the asset in bytes (0 if unknown)
 * @param supplier a supplier for the input stream containing the model data
 * @since 2.0.0
 */
public record ModelAsset(
    @NotNull String rawName,
    @NotNull String name,
    @NotNull String extension,
    long sizeAssume,
    @NotNull StreamSupplier supplier
) implements Comparable<ModelAsset> {

    /**
     * Internal constructor for ModelAsset.
     */
    @ApiStatus.Internal
    public ModelAsset {
        extension = extension.toLowerCase(Locale.ROOT);
    }

    /**
     * Creates an asset using the default {@code bbmodel} extension.
     * <p>Example: {@code new ModelAsset("model", "model", bytes.length, supplier)}.</p>
     *
     * @param rawName the original raw name or path
     * @param name the pack-compliant model name
     * @param sizeAssume the estimated size in bytes
     * @param supplier the stream supplier
     * @since 3.7.0
     */
    @ApiStatus.Internal
    public ModelAsset(@NotNull String rawName, @NotNull String name, long sizeAssume, @NotNull StreamSupplier supplier) {
        this(rawName, name, "bbmodel", sizeAssume, supplier);
    }

    /**
     * Creates a new ModelAsset from a name and byte array using the default {@code bbmodel} extension.
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
     * Creates a new ModelAsset from a name and stream supplier using the default {@code bbmodel} extension.
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
     * Creates a new ModelAsset from a name, stream supplier, and estimated size using the default {@code bbmodel} extension.
     *
     * @param name the name of the asset
     * @param sizeAssume the estimated size in bytes
     * @param supplier the stream supplier
     * @return the created asset
     * @since 2.0.0
     */
    public static @NotNull ModelAsset of(@NotNull String name, long sizeAssume, @NotNull StreamSupplier supplier) {
        return of(name, "bbmodel", sizeAssume, supplier);
    }

    /**
     * Creates an asset from bytes with an explicit extension.
     * <p>Example: {@code ModelAsset.of("model", "custom", bytes)}.</p>
     *
     * @param name the pack-compliant model name
     * @param extension the extension without a leading dot
     * @param bytes the model data
     * @return the created asset
     * @throws IllegalArgumentException if the name is not pack-compliant
     * @since 3.7.0
     */
    public static @NotNull ModelAsset of(@NotNull String name, @NotNull String extension, byte[] bytes) {
        return of(name, extension, bytes.length, () -> new ByteArrayInputStream(bytes));
    }

    /**
     * Creates an asset from a stream supplier with an explicit extension.
     * <p>Example: {@code ModelAsset.of("model", "custom", supplier)}.</p>
     *
     * @param name the pack-compliant model name
     * @param extension the extension without a leading dot
     * @param supplier the stream supplier
     * @return the created asset
     * @throws IllegalArgumentException if the name is not pack-compliant
     * @since 3.7.0
     */
    public static @NotNull ModelAsset of(@NotNull String name, @NotNull String extension, @NotNull StreamSupplier supplier) {
        return of(name, extension, 0, supplier);
    }

    /**
     * Creates an asset with an explicit extension and estimated size.
     * <p>Example: {@code ModelAsset.of("model", "custom", size, supplier)}.</p>
     *
     * @param name the pack-compliant model name
     * @param extension the extension without a leading dot
     * @param sizeAssume the estimated size in bytes
     * @param supplier the stream supplier
     * @return the created asset
     * @throws IllegalArgumentException if the name is not pack-compliant
     * @since 3.7.0
     */
    public static @NotNull ModelAsset of(@NotNull String name, @NotNull String extension, long sizeAssume, @NotNull StreamSupplier supplier) {
        PackUtil.assertPackName(name);
        return new ModelAsset(name, name, extension, sizeAssume, supplier);
    }

    /**
     * Creates a new ModelAsset from a file, inferring its extension or using {@code bbmodel} when absent.
     *
     * @param file the source file
     * @return the created asset
     * @since 2.0.0
     */
    public static @NotNull ModelAsset of(@NotNull File file) {
        return new ModelAsset(file.getPath(), nameWithoutExtension(file.getName()), extension(file.getName()), file.length(), () -> new FileInputStream(file));
    }

    /**
     * Creates a new ModelAsset from a path, inferring its extension or using {@code bbmodel} when absent.
     *
     * @param path the source path
     * @return the created asset
     * @throws RuntimeException if an I/O error occurs
     * @since 2.0.0
     */
    public static @NotNull ModelAsset of(@NotNull Path path) {
        try {
            return new ModelAsset(path.toString(), nameWithoutExtension(path.getFileName().toString()), extension(path.getFileName().toString()), Files.size(path), () -> Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static @NotNull String nameWithoutExtension(@NotNull String name) {
        var index = name.lastIndexOf('.');
        return PackUtil.toPackName(index > 0 ? name.substring(0, index) : name);
    }

    private static @NotNull String extension(@NotNull String name) {
        var index = name.lastIndexOf('.');
        return index > 0 && index < name.length() - 1 ? name.substring(index + 1) : "bbmodel";
    }

    /**
     * Loads this asset using its registered extension importer, or the built-in bbmodel importer if none is registered.
     * <p>Example: {@code ModelAsset.of(path).toResult()}.</p>
     *
     * @return the result of the load operation
     * @throws RuntimeException if an I/O or parsing error occurs
     * @since 2.0.0
     */
    public @NotNull ModelLoadResult toResult() {
        var importer = BetterModel.platform().manager(ModelManager.class).importer(extension);
        return (importer != null ? importer : ModelImporter.BBMODEL).load(this);
    }

    @Override
    public int compareTo(@NotNull ModelAsset o) {
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
    public @NotNull String toString() {
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

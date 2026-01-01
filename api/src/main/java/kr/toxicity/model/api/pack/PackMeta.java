/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.nms.NMSVersion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the metadata (pack.mcmeta) of a resource pack.
 * <p>
 * This record handles the serialization and deserialization of the pack metadata, including
 * pack format, description, and supported formats. It also supports overlays for multi-version support.
 * </p>
 *
 * @param pack the main pack configuration
 * @param overlays the optional overlays configuration
 * @since 1.15.2
 */
public record PackMeta(
    @NotNull Pack pack,
    @Nullable Overlay overlays
) {

    /**
     * The standard path for the pack metadata file.
     * @since 1.15.2
     */
    public static final PackPath PATH = new PackPath("pack.mcmeta");
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(PackVersion.class, (JsonDeserializer<PackVersion>) (json, typeOfT, context) -> {
            if (json.isJsonPrimitive()) return new PackVersion(json.getAsInt());
            else if (json.isJsonArray()) {
                var array = json.getAsJsonArray();
                return new PackVersion(array.get(0).getAsInt(), array.size() < 2 ? 0 : array.get(1).getAsInt());
            } else return null;
        })
        .registerTypeAdapter(PackVersion.class, (JsonSerializer<PackVersion>) (src, typeOfSrc, context) -> src.toJson())
        .registerTypeAdapter(VersionRange.class, (JsonDeserializer<VersionRange>) (json, typeOfT, context) -> {
            if (json.isJsonPrimitive()) return new VersionRange(json.getAsInt());
            else if (json.isJsonArray()) {
                var array = json.getAsJsonArray();
                return new VersionRange(
                    array.get(0).getAsInt(),
                    array.get(1).getAsInt()
                );
            } else if (json.isJsonObject()) {
                return context.deserialize(json, VersionRange.class);
            } else return null;
        })
        .registerTypeAdapter(VersionRange.class, (JsonSerializer<VersionRange>) (src, typeOfSrc, context) -> src.toJson())
        .create();

    /**
     * Creates a new builder for {@link PackMeta}.
     *
     * @return a new builder instance
     * @since 1.15.2
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Converts this metadata to a JSON element.
     *
     * @return the JSON representation
     * @since 1.15.2
     */
    public @NotNull JsonElement toJson() {
        return GSON.toJsonTree(this);
    }

    /**
     * Converts this metadata to a pack resource.
     *
     * @return the pack resource containing the JSON data
     * @since 1.15.2
     */
    public @NotNull PackResource toResource() {
        var json = GSON.toJson(this);
        return PackResource.of(PATH, 2L * json.length(), () -> json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Represents the 'pack' section of the metadata.
     *
     * @param packFormat the pack format version
     * @param description the pack description
     * @param supportedFormats the range of supported formats
     * @param minFormat the minimum supported format (>= 1.21.9)
     * @param maxFormat the maximum supported format
     * @since 1.15.2
     */
    public record Pack(
        @SerializedName("pack_format") int packFormat,
        @SerializedName("description") @NotNull String description,
        @SerializedName("supported_formats") @NotNull VersionRange supportedFormats,
        @SerializedName("min_format") @NotNull PackVersion minFormat, //>=1.21.9
        @SerializedName("max_format") @NotNull PackVersion maxFormat
    ) {
    }

    /**
     * Represents a pack version, consisting of major and minor numbers.
     *
     * @param major the major version
     * @param minor the minor version
     * @since 1.15.2
     */
    public record PackVersion(
        int major,
        int minor
    ) {
        /**
         * Creates a pack version with only a major number.
         *
         * @param major the major version
         * @since 1.15.2
         */
        public PackVersion(int major) {
            this(major, 0);
        }

        /**
         * Converts this version to a JSON element.
         *
         * @return the JSON representation (primitive int or array)
         * @since 1.15.2
         */
        public @NotNull JsonElement toJson() {
            if (minor <= 0) {
                return new JsonPrimitive(major);
            } else {
                var array = new JsonArray(2);
                array.add(major);
                array.add(minor);
                return array;
            }
        }
    }

    /**
     * Represents the 'overlays' section of the metadata.
     *
     * @param entries the list of overlay entries
     * @since 1.15.2
     */
    public record Overlay(@Nullable List<OverlayEntry> entries) {
    }

    /**
     * Represents a single entry in the overlays list.
     *
     * @param formats the range of formats this overlay applies to (removed in 1.21.9)
     * @param directory the directory name for the overlay
     * @param minFormat the minimum format (>= 1.21.9)
     * @param maxFormat the maximum format
     * @since 1.15.2
     */
    public record OverlayEntry(
        @NotNull VersionRange formats, //Removed in 1.21.9
        @NotNull String directory,
        @SerializedName("min_format") @NotNull PackVersion minFormat, //>=1.21.9
        @SerializedName("max_format") @NotNull PackVersion maxFormat
    ) {
        /**
         * Creates an overlay entry with a format range and directory.
         *
         * @param formats the format range
         * @param directory the directory
         * @since 1.15.2
         */
        public OverlayEntry(
            @NotNull VersionRange formats, //Removed in 1.21.9
            @NotNull String directory
        ) {
            this(
                formats,
                directory,
                new PackVersion(formats.minInclusive),
                new PackVersion(formats.maxInclusive)
            );
        }
    }

    /**
     * Represents a range of versions.
     *
     * @param minInclusive the minimum version (inclusive)
     * @param maxInclusive the maximum version (inclusive)
     * @since 1.15.2
     */
    public record VersionRange(
        @SerializedName("min_inclusive") int minInclusive,
        @SerializedName("max_inclusive") int maxInclusive
    ) {

        /**
         * Creates a version range for a single value.
         *
         * @param value the version value
         * @since 1.15.2
         */
        public VersionRange(int value) {
            this(value, value);
        }

        /**
         * Converts this range to a JSON element.
         *
         * @return the JSON representation (primitive int or array)
         * @since 1.15.2
         */
        public @NotNull JsonElement toJson() {
            if (minInclusive == maxInclusive) return new JsonPrimitive(minInclusive);
            var arr = new JsonArray(2);
            arr.add(minInclusive);
            arr.add(maxInclusive);
            return arr;
        }
    }

    /**
     * Builder for {@link PackMeta}.
     *
     * @since 1.15.2
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder {
        private int format = BetterModel.nms().version().getMetaVersion();
        private String description = "BetterModel's default pack.";
        private final List<OverlayEntry> entries = new ArrayList<>();
        private VersionRange supportedFormats = new VersionRange(
            NMSVersion.first().getMetaVersion(),
            NMSVersion.latest().getMetaVersion() //<=1.21.8
        );
        private PackVersion minFormat = new PackVersion(NMSVersion.first().getMetaVersion());
        private PackVersion maxFormat = new PackVersion(NMSVersion.latest().getMetaVersion());

        /**
         * Sets the pack description.
         *
         * @param description the description
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder description(@NotNull String description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        /**
         * Sets the pack format.
         *
         * @param format the format version
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder format(int format) {
            this.format = format;
            return this;
        }

        /**
         * Adds an overlay entry.
         *
         * @param overlayEntry the overlay entry to add
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder overlayEntry(@NotNull OverlayEntry overlayEntry) {
            entries.add(overlayEntry);
            return this;
        }

        /**
         * Sets the supported formats range.
         *
         * @param range the version range
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder supportedFormats(@NotNull VersionRange range) {
            supportedFormats = Objects.requireNonNull(range);
            return this;
        }

        /**
         * Sets the minimum supported format.
         *
         * @param version the minimum version
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder minFormat(@NotNull PackVersion version) {
            minFormat = Objects.requireNonNull(version);
            return this;
        }

        /**
         * Sets the maximum supported format.
         *
         * @param version the maximum version
         * @return this builder
         * @since 1.15.2
         */
        public @NotNull Builder maxFormat(@NotNull PackVersion version) {
            maxFormat = Objects.requireNonNull(version);
            return this;
        }

        /**
         * Builds the {@link PackMeta} instance.
         *
         * @return the created pack meta
         * @since 1.15.2
         */
        public @NotNull PackMeta build() {
            return new PackMeta(
                new Pack(
                    format,
                    description,
                    supportedFormats,
                    minFormat,
                    maxFormat
                ),
                entries.isEmpty() ? null : new Overlay(entries)
            );
        }
    }
}

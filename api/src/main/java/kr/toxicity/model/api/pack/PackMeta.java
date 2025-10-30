/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
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

public record PackMeta(
        @NotNull Pack pack,
        @Nullable Overlay overlays
) {

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

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public @NotNull JsonElement toJson() {
        return GSON.toJsonTree(this);
    }

    public @NotNull PackResource toResource() {
        var json = GSON.toJson(this);
        return PackResource.of(PATH, 2L * json.length(), () -> json.getBytes(StandardCharsets.UTF_8));
    }

    public record Pack(
            @SerializedName("pack_format") int packFormat,
            @SerializedName("description") @NotNull String description,
            @SerializedName("supported_formats") @NotNull VersionRange supportedFormats,
            @SerializedName("min_format") @NotNull PackVersion minFormat, //>=1.21.9
            @SerializedName("max_format") @NotNull PackVersion maxFormat
    ) {
    }

    public record PackVersion(
            int major,
            int minor
    ) {
        public PackVersion(int major) {
            this(major, 0);
        }

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

    public record Overlay(@Nullable List<OverlayEntry> entries) {
    }

    public record OverlayEntry(
            @NotNull VersionRange formats, //Removed in 1.21.9
            @NotNull String directory,
            @SerializedName("min_format") @NotNull PackVersion minFormat, //>=1.21.9
            @SerializedName("max_format") @NotNull PackVersion maxFormat
    ) {
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

    public record VersionRange(
            @SerializedName("min_inclusive") int minInclusive,
            @SerializedName("max_inclusive") int maxInclusive
    ) {
        public static final VersionRange LEGACY = new VersionRange(
                NMSVersion.first().getMetaVersion(),
                64 //<=1.21.8
        );

       public VersionRange(int value) {
           this(value, value);
       }

       public @NotNull JsonElement toJson() {
           if (minInclusive == maxInclusive) return new JsonPrimitive(minInclusive);
           var arr = new JsonArray(2);
           arr.add(minInclusive);
           arr.add(maxInclusive);
           return arr;
       }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder {
        private int format = BetterModel.nms().version().getMetaVersion();
        private String description = "BetterModel's default pack.";
        private final List<OverlayEntry> entries = new ArrayList<>();
        private VersionRange supportedFormats = VersionRange.LEGACY;
        private PackVersion minFormat = new PackVersion(NMSVersion.first().getMetaVersion());
        private PackVersion maxFormat = new PackVersion(NMSVersion.latest().getMetaVersion());

        public @NotNull Builder description(@NotNull String description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public @NotNull Builder format(int format) {
            this.format = format;
            return this;
        }

        public @NotNull Builder overlayEntry(@NotNull OverlayEntry overlayEntry) {
            entries.add(overlayEntry);
            return this;
        }

        public @NotNull Builder supportedFormats(@NotNull VersionRange range) {
            supportedFormats = Objects.requireNonNull(range);
            return this;
        }

        public @NotNull Builder minFormat(@NotNull PackVersion version) {
            minFormat = Objects.requireNonNull(version);
            return this;
        }

        public @NotNull Builder maxFormat(@NotNull PackVersion version) {
            maxFormat = Objects.requireNonNull(version);
            return this;
        }

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

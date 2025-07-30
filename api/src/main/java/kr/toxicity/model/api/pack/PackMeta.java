package kr.toxicity.model.api.pack;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import kr.toxicity.model.api.BetterModel;
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
        return PackResource.of(PATH, () -> GSON.toJson(this).getBytes(StandardCharsets.UTF_8));
    }

    public record Pack(
            @SerializedName("pack_format") int packFormat,
            @SerializedName("description") @NotNull String description
    ) {
    }

    public record Overlay(@Nullable List<OverlayEntry> entries) {
    }

    public record OverlayEntry(
            @NotNull VersionRange formats,
            @NotNull String directory
    ) {
    }

    public record VersionRange(
            @SerializedName("min_inclusive") int minInclusive,
            @SerializedName("max_inclusive") int maxInclusive
    ) {
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
        private int format = BetterModel.plugin().nms().version().getMetaVersion();
        private String description = "BetterModel's default pack.";
        private final List<OverlayEntry> entries = new ArrayList<>();

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

        public @NotNull PackMeta build() {
            return new PackMeta(
                    new Pack(
                            format,
                            description
                    ),
                    entries.isEmpty() ? null : new Overlay(entries)
            );
        }
    }
}

/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.util.LogUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PackZipper {

    private static final PackPath PACK_ICON = new PackPath("pack.png");


    public static @NotNull PackZipper zipper() {
        return new PackZipper();
    }

    private final PackMeta.Builder metaBuilder = PackMeta.builder();
    private final Map<PackOverlay, PackAssets> overlayMap = new ConcurrentHashMap<>();

    public @NotNull PackAssets assets() {
        return overlay(PackOverlay.DEFAULT);
    }
    public @NotNull PackAssets legacy() {
        return overlay(PackOverlay.LEGACY);
    }
    public @NotNull PackAssets modern() {
        return overlay(PackOverlay.MODERN);
    }

    public @NotNull PackAssets overlay(@NotNull PackOverlay overlay) {
        return overlayMap.computeIfAbsent(overlay, PackAssets::new);
    }

    public @NotNull PackMeta.Builder metaBuilder() {
        return metaBuilder;
    }

    @ApiStatus.Internal
    public @NotNull BuildData build() {
        var resources = new ArrayList<PackResource>(size());
        for (Map.Entry<PackOverlay, PackAssets> entry : overlayMap.entrySet()) {
            var overlay = entry.getKey();
            var value = entry.getValue();
            if (overlay.test() && value.dirty()) {
                resources.addAll(value.resourceMap.values());
                overlay.range().ifPresent(range -> metaBuilder.overlayEntry(new PackMeta.OverlayEntry(range, value.path.path())));
            }
            value.resourceMap.clear();
        }
        var meta = metaBuilder.build();
        resources.add(meta.toResource());
        var icon = loadIcon();
        if (icon != null) resources.add(icon);
        return new BuildData(meta, resources);
    }

    public int size() {
        return overlayMap.values().stream().mapToInt(PackAssets::size).sum() + 2;
    }

    private static @Nullable PackResource loadIcon() {
        try (
            var icon = BetterModel.plugin().getResource("icon.png")
        ) {
            if (icon == null) return null;
            var read = icon.readAllBytes();
            return PackResource.of(PACK_ICON, read.length, () -> read);
        } catch (IOException e) {
            LogUtil.handleException("Unable to get icon.png", e);
            return null;
        }
    }

    public record BuildData(@NotNull PackMeta meta, @NotNull List<PackResource> resources) {

    }
}

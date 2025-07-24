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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PackZipper {

    private static final PackPath OVERLAY_LEGACY = new PackPath("bettermodel_legacy");
    private static final PackPath OVERLAY_MODERN = new PackPath("bettermodel_modern");
    private static final PackPath PACK_ICON = new PackPath("pack.png");

    private static final PackMeta.VersionRange LEGACY_FORMATS = new PackMeta.VersionRange(22, 45);
    private static final PackMeta.VersionRange MODERN_FORMATS = new PackMeta.VersionRange(46, 99);

    public static @NotNull PackZipper zipper() {
        return new PackZipper();
    }

    private final PackMeta.Builder metaBuilder = PackMeta.builder();
    private final PackAssets assets = new PackAssets(PackPath.EMPTY);
    private final PackAssets legacy = new PackAssets(OVERLAY_LEGACY);
    private final PackAssets modern = new PackAssets(OVERLAY_MODERN);

    public @NotNull PackAssets assets() {
        return assets;
    }
    public @NotNull PackAssets legacy() {
        return legacy;
    }
    public @NotNull PackAssets modern() {
        return modern;
    }

    public @NotNull PackMeta.Builder metaBuilder() {
        return metaBuilder;
    }

    @ApiStatus.Internal
    public @NotNull List<PackResource> build() {
        var config = BetterModel.config().pack();
        var resources = new ArrayList<PackResource>(assets.size() + legacy.size() + modern.size() + 2);
        resources.addAll(assets.resourceMap.values());
        if (config.generateLegacyModel() && legacy.dirty()) {
            resources.addAll(legacy.resourceMap.values());
            metaBuilder.overlayEntry(new PackMeta.OverlayEntry(LEGACY_FORMATS, OVERLAY_LEGACY.path()));
        }
        if (config.generateModernModel() && modern.dirty()) {
            resources.addAll(modern.resourceMap.values());
            metaBuilder.overlayEntry(new PackMeta.OverlayEntry(MODERN_FORMATS, OVERLAY_MODERN.path()));
        }

        resources.add(metaBuilder.build().toResource());
        var icon = loadIcon();
        if (icon != null) resources.add(icon);

        assets.resourceMap.clear();
        legacy.resourceMap.clear();
        modern.resourceMap.clear();

        return resources;
    }

    private static @Nullable PackResource loadIcon() {
        try (
                var icon = BetterModel.plugin().getResource("icon.png")
        ) {
            if (icon == null) return null;
            var read = icon.readAllBytes();
            return PackResource.of(PACK_ICON, () -> read);
        } catch (IOException e) {
            LogUtil.handleException("Unable to get icon.png", e);
            return null;
        }
    }
}

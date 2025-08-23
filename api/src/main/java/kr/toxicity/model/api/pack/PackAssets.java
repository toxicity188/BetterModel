package kr.toxicity.model.api.pack;

import kr.toxicity.model.api.BetterModel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class PackAssets {
    final PackPath path;
    final PackOverlay overlay;
    final Map<PackPath, PackResource> resourceMap = new ConcurrentHashMap<>();

    private final PackNamespace bettermodel, minecraft;

    PackAssets(@NotNull PackPath path, @NotNull PackOverlay overlay) {
        this.path = path;
        this.overlay = overlay;
        bettermodel = new PackNamespace(this, BetterModel.config().namespace());
        minecraft = new PackNamespace(this, "minecraft");
    }

    public @NotNull PackNamespace bettermodel() {
        return bettermodel;
    }

    public @NotNull PackNamespace minecraft() {
        return minecraft;
    }

    int size() {
        return resourceMap.size();
    }

    boolean dirty() {
        return size() > 0;
    }

    public void add(@NotNull String path, long size, @NotNull Supplier<byte[]> supplier) {
        add(new String[] { path }, size, supplier);
    }

    public void add(@NotNull String[] paths, long size, @NotNull Supplier<byte[]> supplier) {
        var resolve = path.resolve(paths);
        resourceMap.putIfAbsent(resolve, PackResource.of(overlay, resolve, size, supplier));
    }

    public void add(@NotNull String path, @NotNull Supplier<byte[]> supplier) {
        add(path, -1, supplier);
    }

    public void add(@NotNull String[] paths, @NotNull Supplier<byte[]> supplier) {
        add(paths, -1, supplier);
    }
}

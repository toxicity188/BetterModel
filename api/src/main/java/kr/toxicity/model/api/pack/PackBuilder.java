package kr.toxicity.model.api.pack;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@RequiredArgsConstructor(access = AccessLevel.MODULE)
public final class PackBuilder {
    private final PackAssets assets;
    private final PackPath path;

    public @NotNull PackBuilder resolve(@NotNull String... paths) {
        return new PackBuilder(assets, path.resolve(paths));
    }

    public void add(@NotNull String path, long estimatedSize, @NotNull Supplier<byte[]> supplier) {
        add(new String[] { path }, estimatedSize, supplier);
    }

    public void add(@NotNull String[] paths, long size, @NotNull Supplier<byte[]> supplier) {
        var resolve = path.resolve(paths);
        assets.resourceMap.putIfAbsent(resolve, PackResource.of(assets.overlay, resolve, size, supplier));
    }

    public void add(@NotNull String path, @NotNull Supplier<byte[]> supplier) {
        add(path, -1, supplier);
    }

    public void add(@NotNull String[] paths, @NotNull Supplier<byte[]> supplier) {
        add(paths, -1, supplier);
    }
}

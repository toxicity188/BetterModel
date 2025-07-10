package kr.toxicity.model.api.pack;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@RequiredArgsConstructor
public final class PackBuilder {
    private final PackAssets assets;
    private final PackPath path;

    public @NotNull PackBuilder resolve(@NotNull String... paths) {
        return new PackBuilder(assets, path.resolve(paths));
    }

    public void add(@NotNull String path, @NotNull Supplier<byte[]> supplier) {
        add(new String[] { path }, supplier);
    }

    public void add(@NotNull String[] paths, @NotNull Supplier<byte[]> supplier) {
        var resolve = path.resolve(paths);
        assets.resourceMap.putIfAbsent(resolve, PackResource.of(resolve, supplier));
    }
}

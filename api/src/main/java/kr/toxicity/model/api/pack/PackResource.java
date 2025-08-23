package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public interface PackResource extends Supplier<byte[]> {

    @Nullable
    PackOverlay overlay();

    @NotNull
    PackPath path();

    long estimatedSize();

    static @NotNull PackResource of(@NotNull PackPath path, long size, @NotNull Supplier<byte[]> supplier) {
        return of(null, path, size, supplier);
    }

    static @NotNull PackResource of(@Nullable PackOverlay overlay, @NotNull PackPath path, long size, @NotNull Supplier<byte[]> supplier) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(supplier, "supplier");
        return new Packed(overlay, path, size, supplier);
    }

    record Packed(
            @Nullable PackOverlay overlay,
            @NotNull PackPath path,
            long estimatedSize,
            @NotNull Supplier<byte[]> supplier
    ) implements PackResource {
        @Override
        public byte[] get() {
            return supplier.get();
        }
    }
}

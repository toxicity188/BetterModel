package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

public interface PackResource extends Supplier<byte[]> {
    @NotNull
    PackPath path();
    long estimatedSize();

    static @NotNull PackResource of(@NotNull PackPath path, long size, @NotNull Supplier<byte[]> supplier) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(supplier, "supplier");
        return new PackResource() {
            @Override
            public @NotNull PackPath path() {
                return path;
            }

            @Override
            public long estimatedSize() {
                return size;
            }

            @Override
            public byte[] get() {
                return supplier.get();
            }
        };
    }
}

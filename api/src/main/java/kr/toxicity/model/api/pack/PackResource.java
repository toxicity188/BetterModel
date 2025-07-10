package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

public interface PackResource extends Supplier<byte[]> {
    @NotNull
    PackPath path();

    static @NotNull PackResource of(@NotNull PackPath path, @NotNull Supplier<byte[]> supplier) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(supplier, "supplier");
        return new PackResource() {
            @Override
            public @NotNull PackPath path() {
                return path;
            }

            @Override
            public byte[] get() {
                return supplier.get();
            }
        };
    }
}

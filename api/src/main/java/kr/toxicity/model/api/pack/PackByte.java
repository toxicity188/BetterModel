package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record PackByte(@NotNull PackPath path, byte[] bytes) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PackByte packByte)) return false;
        return Objects.equals(path, packByte.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}

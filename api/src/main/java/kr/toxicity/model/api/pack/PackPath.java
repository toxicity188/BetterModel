package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

import static java.lang.String.join;

public record PackPath(@NotNull String path) implements Comparable<PackPath> {

    public static final String DELIMITER = "/";
    public static final PackPath EMPTY = new PackPath("");

    public @NotNull PackPath resolve(@NotNull String... subPaths) {
        if (subPaths.length == 0) return this;
        return new PackPath(path.isEmpty() ? join(DELIMITER, subPaths) : path + DELIMITER + join(DELIMITER, subPaths));
    }

    @Override
    public @NotNull String toString() {
        return path;
    }

    @Override
    public int compareTo(@NotNull PackPath o) {
        return path.compareTo(o.path);
    }
}

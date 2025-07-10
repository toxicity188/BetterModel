package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

import static java.lang.String.join;

public record PackPath(@NotNull String path) {

    public static final String DELIMITER = "/";
    public static final PackPath EMPTY = new PackPath("");

    public PackPath(@NotNull String... paths) {
        this(join(DELIMITER, paths));
    }

    public @NotNull PackPath resolve(@NotNull String... subPaths) {
        if (subPaths.length == 0) return this;
        return new PackPath(path.isEmpty() ? join(DELIMITER, subPaths) : path + DELIMITER + join(DELIMITER, subPaths));
    }

    public @NotNull PackPath resolve(@NotNull PackPath packPath) {
        return resolve(packPath.path);
    }

    @Override
    public @NotNull String toString() {
        return path;
    }
}

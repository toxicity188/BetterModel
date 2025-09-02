package kr.toxicity.model.api.pack;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.util.function.BooleanConstantSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Pack overlay
 * @param packName name
 * @param range range
 * @param tester tester
 */
public record PackOverlay(
        @NotNull String packName,
        @NotNull Optional<PackMeta.VersionRange> range,
        @NotNull BooleanSupplier tester
) implements Comparable<PackOverlay> {
    /**
     * Default
     */
    public static final PackOverlay DEFAULT = new PackOverlay(
            "",
            Optional.empty(),
            BooleanConstantSupplier.TRUE
    );

    /**
     * Legacy
     */
    public static final PackOverlay LEGACY = new PackOverlay(
            "legacy",
            Optional.of(new PackMeta.VersionRange(22, 45)),
            BetterModel.config().pack()::generateLegacyModel
    );

    /**
     * Modern
     */
    public static final PackOverlay MODERN = new PackOverlay(
            "modern",
            Optional.of(new PackMeta.VersionRange(46, 99)),
            BetterModel.config().pack()::generateModernModel
    );


    /**
     * Gets path
     * @param namespace namespace
     * @return path
     */
    public @NotNull PackPath path(@NotNull String namespace) {
        return packName.isEmpty() ? PackPath.EMPTY : new PackPath(namespace + "_" + packName);
    }

    /**
     * Tests this overlay
     * @return test value
     */
    public boolean test() {
        return tester.getAsBoolean();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PackOverlay that)) return false;
        return packName.equals(that.packName);
    }

    @Override
    public int hashCode() {
        return packName.hashCode();
    }

    @Override
    public int compareTo(@NotNull PackOverlay o) {
        return packName.compareTo(o.packName);
    }
}

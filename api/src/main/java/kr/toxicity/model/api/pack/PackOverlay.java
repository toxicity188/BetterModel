package kr.toxicity.model.api.pack;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.util.function.BooleanConstantSupplier;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.BooleanSupplier;

@RequiredArgsConstructor
public enum PackOverlay {
    /**
     * Default
     */
    DEFAULT("default", null, BooleanConstantSupplier.TRUE) {
        @Override
        public @NotNull PackPath path(@NotNull String namespace) {
            return PackPath.EMPTY;
        }
    },
    /**
     * Legacy
     */
    LEGACY("legacy", new PackMeta.VersionRange(22, 45), BetterModel.config().pack()::generateLegacyModel),
    /**
     * Modern
     */
    MODERN("modern", new PackMeta.VersionRange(46, 99), BetterModel.config().pack()::generateModernModel)
    ;
    private final String packName;
    private final PackMeta.VersionRange range;
    private final BooleanSupplier predicate;

    /**
     * Gets path
     * @param namespace namespace
     * @return path
     */
    public @NotNull PackPath path(@NotNull String namespace) {
        return new PackPath(namespace + "_" + packName);
    }

    /**
     * Gets version range
     * @return optional version range
     */
    public @NotNull Optional<PackMeta.VersionRange> range() {
        return Optional.ofNullable(range);
    }

    /**
     * Tests this overlay
     * @return test value
     */
    public boolean test() {
        return predicate.getAsBoolean();
    }
}

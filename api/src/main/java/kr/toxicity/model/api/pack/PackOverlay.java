/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.util.function.BooleanConstantSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Represents a resource pack overlay, allowing for version-specific resources.
 * <p>
 * Overlays are used to support multiple Minecraft versions within a single resource pack.
 * </p>
 *
 * @param packName the name of the overlay (e.g., "legacy", "modern")
 * @param range the version range this overlay applies to
 * @param tester a supplier to determine if this overlay should be active
 * @since 1.15.2
 */
public record PackOverlay(
    @NotNull String packName,
    @NotNull Optional<PackMeta.VersionRange> range,
    @NotNull BooleanSupplier tester
) implements Comparable<PackOverlay> {
    /**
     * The default overlay (base pack).
     * @since 1.15.2
     */
    public static final PackOverlay DEFAULT = new PackOverlay(
        "",
        Optional.empty(),
        BooleanConstantSupplier.TRUE
    );

    /**
     * The legacy overlay (for older versions).
     * @since 1.15.2
     */
    public static final PackOverlay LEGACY = new PackOverlay(
        "legacy",
        Optional.of(new PackMeta.VersionRange(22, 45)),
        BetterModel.config().pack()::generateLegacyModel
    );

    /**
     * The modern overlay (for newer versions).
     * @since 1.15.2
     */
    public static final PackOverlay MODERN = new PackOverlay(
        "modern",
        Optional.of(new PackMeta.VersionRange(46, 99)),
        BetterModel.config().pack()::generateModernModel
    );


    /**
     * Generates the root path for this overlay.
     *
     * @param namespace the namespace prefix
     * @return the pack path
     * @since 1.15.2
     */
    public @NotNull PackPath path(@NotNull String namespace) {
        return packName.isEmpty() ? PackPath.EMPTY : new PackPath(namespace + "_" + packName);
    }

    /**
     * Checks if this overlay is active.
     *
     * @return true if active, false otherwise
     * @since 1.15.2
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

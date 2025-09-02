package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Pack overlay
 */
public interface PackOverlay {
    /**
     * Gets path
     * @param namespace namespace
     * @return path
     */
    @NotNull PackPath path(@NotNull String namespace);

    /**
     * Gets version range
     * @return optional version range
     */
    @NotNull Optional<PackMeta.VersionRange> range();

    /**
     * Tests this overlay
     * @return test value
     */
    boolean test();
}

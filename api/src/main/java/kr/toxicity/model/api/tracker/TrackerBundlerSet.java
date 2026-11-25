/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.nms.PacketBundler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A pair of {@link PacketBundler} instances used during tracker tick and frame updates.
 * <p>
 * Separates packet-based rendering updates into tick packets (movement and position synchronization
 * dispatched to all tracking players) and data packets (dirty entity metadata and display properties
 * dispatched only to non-hidden viewing players).
 * </p>
 * <p>Example:</p>
 * <pre>{@code
 * tracker.tick(bundlerSet -> {
 *     PacketBundler dataBundler = bundlerSet.dataBundler();
 *     PacketBundler tickBundler = bundlerSet.tickBundler();
 *     // append packet operations
 * });
 * }</pre>
 *
 * @since 3.7.0
 */
public final class TrackerBundlerSet {

    private final RenderPipeline pipeline;

    private PacketBundler tickBundler;
    private PacketBundler dataBundler;

    /**
     * Constructs a new bundler set.
     *
     * @param pipeline the render pipeline
     * @since 3.7.0
     */
    TrackerBundlerSet(@NotNull RenderPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline);
        this.tickBundler = pipeline.createBundler();
        this.dataBundler = pipeline.createBundler();
    }

    /**
     * Flushes non-empty packet bundlers to tracking players and reinitializes bundlers.
     *
     * @since 3.7.0
     */
    void send() {
        if (tickBundler.isNotEmpty()) {
            pipeline.forEachAllPlayer(tickBundler::send);
            tickBundler = pipeline.createBundler();
        }
        if (dataBundler.isNotEmpty()) {
            pipeline.forEachNonHidePlayer(dataBundler::send);
            dataBundler = pipeline.createBundler();
        }
    }

    /**
     * Returns the packet bundler for entity data updates.
     * <p>
     * Packets added to this bundler are dispatched to non-hidden tracking players.
     * </p>
     *
     * @return the data packet bundler
     * @since 3.7.0
     */
    @NotNull
    public PacketBundler dataBundler() {
        return dataBundler;
    }

    /**
     * Returns the packet bundler for movement and tick updates.
     * <p>
     * Packets added to this bundler are dispatched to all tracking players.
     * </p>
     *
     * @return the tick packet bundler
     * @since 3.7.0
     */
    @NotNull
    public PacketBundler tickBundler() {
        return tickBundler;
    }
}

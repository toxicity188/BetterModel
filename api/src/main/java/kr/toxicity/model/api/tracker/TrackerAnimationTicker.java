/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.tracker;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.nms.AnimationBundler;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Coordinates animation ticking and packet dispatching for a tracker.
 * <p>
 * Manages animation frame updates via the {@link RenderPipeline}, broadcasting animation packets
 * to viewing players and handling isolated per-player animation overrides with reference counting.
 * </p>
 * <p>Example:</p>
 * <pre>{@code
 * TrackerAnimationTicker ticker = new TrackerAnimationTicker(pipeline);
 * ticker.tick();
 * }</pre>
 *
 * @since 3.7.0
 */
final class TrackerAnimationTicker {

    private final RenderPipeline pipeline;

    private final List<PlayerChannelHandler> viewedPlayers = new ObjectArrayList<>();
    private final Map<UUID, PerPlayerCache> perPlayerAnimationCache = new ConcurrentHashMap<>();
    private AnimationBundler animationBundler;

    /**
     * Constructs a new animation ticker.
     *
     * @param pipeline the render pipeline
     * @since 3.7.0
     */
    TrackerAnimationTicker(@NotNull RenderPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        this.animationBundler = pipeline.createAnimationBundler();
    }

    /**
     * Advances animation playback in the render pipeline and dispatches animation packets to viewers.
     *
     * @since 3.7.0
     */
    void tick() {
        if (!pipeline.tick()) return;
        recalculateViewedPlayers();
        if (viewedPlayers.isEmpty()) return;
        sendAnimation();
    }

    /**
     * Increments the reference count of active per-player animations for the specified player.
     * <p>
     * If this is the first animation registered for the player, {@code startConsumer} is invoked.
     * </p>
     *
     * @param uuid the unique identifier of the player
     * @param startConsumer the callback invoked when the per-player animation starts
     * @since 3.7.0
     */
    void countAdd(@NotNull UUID uuid, @NotNull Consumer<PlayerChannelHandler> startConsumer) {
        perPlayerAnimationCache
            .computeIfAbsent(uuid, PerPlayerCache::new)
            .add(startConsumer);
    }

    /**
     * Decrements the reference count of active per-player animations for the specified player.
     * <p>
     * If no active animations remain for the player, cached entries are removed,
     * bone transformations are forcibly synchronized, and {@code endConsumer} is invoked.
     * </p>
     *
     * @param uuid the unique identifier of the player
     * @param endConsumer the callback invoked when the per-player animation ends
     * @since 3.7.0
     */
    void countRemove(@NotNull UUID uuid, @NotNull Consumer<PlayerChannelHandler> endConsumer) {
        var get = perPlayerAnimationCache.get(uuid);
        if (get != null) get.remove(endConsumer);
    }

    private void recalculateViewedPlayers() {
        viewedPlayers.clear();
        pipeline.forEachViewedPlayer(channel -> {
            if (!perPlayerAnimationCache.containsKey(channel.uuid())) viewedPlayers.add(channel);
        });
    }

    private void sendAnimation() {
        pipeline.send(animationBundler);
        if (animationBundler.isNotEmpty()) {
            viewedPlayers.forEach(animationBundler::send);
            animationBundler = pipeline.createAnimationBundler();
        }
        if (perPlayerAnimationCache.isEmpty()) return;
        perPlayerAnimationCache.values().forEach(PerPlayerCache::send);
    }

    @RequiredArgsConstructor
    private final class PerPlayerCache {
        private final UUID uuid;
        private final AtomicInteger counter = new AtomicInteger();
        private AnimationBundler bundler = pipeline.createAnimationBundler();

        private @NotNull Optional<PlayerChannelHandler> channel() {
            return Optional.ofNullable(pipeline.channel(uuid));
        }

        public void add(@NotNull Consumer<PlayerChannelHandler> consumer) {
            if (counter.getAndIncrement() == 0) {
                channel().ifPresent(consumer);
            }
        }

        public void remove(@NotNull Consumer<PlayerChannelHandler> consumer) {
            if (counter.decrementAndGet() == 0) {
                perPlayerAnimationCache.remove(uuid);
                channel().ifPresent(channel -> {
                    var bundler = pipeline.createBundler();
                    pipeline.forEach(bone -> bone.forceTransformation(bundler));
                    bundler.send(channel);
                    consumer.accept(channel);
                });
            }
        }

        private void send() {
            if (pipeline.tick(uuid, bundler) && bundler.isNotEmpty()) {
                channel().ifPresent(bundler::send);
                bundler = pipeline.createAnimationBundler();
            }
        }
    }
}

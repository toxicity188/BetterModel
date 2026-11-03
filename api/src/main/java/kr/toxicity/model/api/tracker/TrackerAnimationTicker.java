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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@RequiredArgsConstructor
final class TrackerAnimationTicker {

    private final RenderPipeline pipeline;

    private final List<PlayerChannelHandler> viewedPlayers = new ObjectArrayList<>();
    private final Map<UUID, PerPlayerCache> perPlayerAnimationCache = new ConcurrentHashMap<>();
    private AnimationBundler animationBundler = pipeline.createAnimationBundler();

    void tick() {
        if (!pipeline.tick()) return;
        recalculateViewedPlayers();
        if (viewedPlayers.isEmpty()) return;
        sendAnimation();
    }

    void countAdd(@NotNull UUID uuid, @NotNull Consumer<PlayerChannelHandler> startConsumer) {
        perPlayerAnimationCache
            .computeIfAbsent(uuid, PerPlayerCache::new)
            .add(startConsumer);
    }

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


/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.nms.HitBoxListener;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class BoneEventDispatcher {
    private final EventFunction builder = new EventFunction();
    private EventFunction applier = builder;

    public synchronized void extend(@NotNull BoneEventDispatcher dispatcher) {
        if (dispatcher == this) throw new UnsupportedOperationException("cannot extend self");
        applier = EventFunction.concat(dispatcher.applier, builder);
    }

    public synchronized void handleCreateHitBox(@NotNull BiFunction<RenderedBone, HitBoxListener.Builder, HitBoxListener.Builder> function) {
        var before = builder.createHitBox;
        builder.createHitBox = (b, l) -> function.apply(b, before.apply(b, l));
    }

    public synchronized void handleStateCreate(@NotNull BiConsumer<RenderedBone, UUID> function) {
        builder.stateCreate = builder.stateCreate.andThen(function);
    }

    public synchronized void handleStateRemove(@NotNull BiConsumer<RenderedBone, UUID> function) {
        builder.stateRemove = builder.stateRemove.andThen(function);
    }

    @NotNull HitBoxListener.Builder onCreateHitBox(@NotNull RenderedBone bone, @NotNull HitBoxListener.Builder builder) {
        return applier.createHitBox.apply(bone, builder);
    }

    void onStateCreated(@NotNull RenderedBone bone, @NotNull UUID uuid) {
        applier.stateCreate.accept(bone, uuid);
    }

    void onStateRemoved(@NotNull RenderedBone bone, @NotNull UUID uuid) {
        applier.stateRemove.accept(bone, uuid);
    }

    @AllArgsConstructor
    private static class EventFunction {
        private BiFunction<RenderedBone, HitBoxListener.Builder, HitBoxListener.Builder> createHitBox;
        private BiConsumer<RenderedBone, UUID> stateCreate;
        private BiConsumer<RenderedBone, UUID> stateRemove;

        EventFunction() {
            this(
                    (b, l) -> l,
                    (b, u) -> {},
                    (b, u) -> {}
            );
        }

        static @NotNull EventFunction concat(@NotNull EventFunction first, @NotNull EventFunction second) {
            return new EventFunction(
                    (b, l) -> second.createHitBox.apply(b, first.createHitBox.apply(b, l)),
                    (b, u) -> {
                        first.stateCreate.accept(b, u);
                        second.stateCreate.accept(b, u);
                    },
                    (b, u) -> {
                        first.stateRemove.accept(b, u);
                        second.stateRemove.accept(b, u);
                    }
            );
        }
    }
}

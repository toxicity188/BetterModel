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

/**
 * Dispatches events related to bone lifecycle and interaction.
 * <p>
 * This class manages handlers for hitbox creation, state creation, and state removal.
 * It allows for extending behavior by chaining dispatchers.
 * </p>
 *
 * @since 1.15.2
 */
public final class BoneEventDispatcher {
    private final EventFunction builder = new EventFunction();
    private EventFunction applier = builder;

    /**
     * Extends this dispatcher with another dispatcher's handlers.
     * <p>
     * The handlers from the provided dispatcher will be executed before the handlers in this dispatcher.
     * </p>
     *
     * @param dispatcher the dispatcher to extend
     * @throws UnsupportedOperationException if attempting to extend self
     * @since 1.15.2
     */
    public synchronized void extend(@NotNull BoneEventDispatcher dispatcher) {
        if (dispatcher == this) throw new UnsupportedOperationException("cannot extend self");
        applier = EventFunction.concat(dispatcher.applier, builder);
    }

    /**
     * Registers a handler for hitbox creation.
     *
     * @param function the function to modify the hitbox listener builder
     * @since 1.15.2
     */
    public synchronized void handleCreateHitBox(@NotNull BiFunction<RenderedBone, HitBoxListener.Builder, HitBoxListener.Builder> function) {
        var before = builder.createHitBox;
        builder.createHitBox = (b, l) -> function.apply(b, before.apply(b, l));
    }

    /**
     * Registers a handler for state creation (e.g., when a bone is initialized for a player).
     *
     * @param function the consumer to handle state creation
     * @since 1.15.2
     */
    public synchronized void handleStateCreate(@NotNull BiConsumer<RenderedBone, UUID> function) {
        builder.stateCreate = builder.stateCreate.andThen(function);
    }

    /**
     * Registers a handler for state removal (e.g., when a bone is removed for a player).
     *
     * @param function the consumer to handle state removal
     * @since 1.15.2
     */
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

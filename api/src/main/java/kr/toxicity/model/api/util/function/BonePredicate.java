/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.util.function;

import kr.toxicity.model.api.bone.BoneTag;
import kr.toxicity.model.api.bone.RenderedBone;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Bone predicate
 */
public interface BonePredicate extends Predicate<RenderedBone> {

    /**
     * True
     */
    BonePredicate TRUE = of(State.TRUE, b -> true);

    /**
     * False
     */
    BonePredicate FALSE = of(State.FALSE, b -> false);

    /**
     * Gets builder by name
     * @param name name
     * @return builder
     */
    static @NotNull Builder name(@NotNull String name) {
        return b -> b.name().name().equalsIgnoreCase(name);
    }

    /**
     * Gets builder by tags
     * @param tags tags
     * @return builder
     */
    static @NotNull Builder tag(@NotNull BoneTag... tags) {
        if (tags.length == 0) throw new RuntimeException("tags cannot be empty.");
        return b -> b.name().tagged(tags);
    }

    @Override
    boolean test(@NotNull RenderedBone bone);

    @Override
    @NotNull
    BonePredicate and(@NotNull Predicate<? super RenderedBone> other);

    @Override
    @NotNull
    BonePredicate or(@NotNull Predicate<? super RenderedBone> other);

    @Override
    @NotNull
    BonePredicate negate();

    /**
     * Should apply at children bone too
     * @return apply at children
     */
    @NotNull State applyAtChildren();

    /**
     * Gets bone predicate
     * @param predicate predicate
     * @return bone predicate
     */
    static @NotNull BonePredicate from(@NotNull Predicate<RenderedBone> predicate) {
        return of(State.NOT_SET, predicate);
    }

    /**
     * Gets bone predicate
     * @param applyAtChildren apply at children
     * @param predicate predicate
     * @return bone predicate
     */
    static @NotNull BonePredicate of(@NotNull State applyAtChildren, @NotNull Predicate<RenderedBone> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null.");
        return new Packed(applyAtChildren, predicate);
    }

    /**
     * Packed value
     * @param applyAtChildren apply at children
     * @param predicate predicate
     */
    record Packed(@NotNull State applyAtChildren, @NotNull Predicate<RenderedBone> predicate) implements BonePredicate {
        @Override
        public boolean test(@NotNull RenderedBone bone) {
            return predicate.test(bone);
        }

        @Override
        @NotNull
        public BonePredicate and(@NotNull Predicate<? super RenderedBone> other) {
            Objects.requireNonNull(other);
            return of(applyAtChildren, t -> predicate.test(t) && other.test(t));
        }

        @Override
        @NotNull
        public BonePredicate or(@NotNull Predicate<? super RenderedBone> other) {
            Objects.requireNonNull(other);
            return of(applyAtChildren, t -> predicate.test(t) || other.test(t));
        }

        @Override
        @NotNull
        public BonePredicate negate() {
            return of(applyAtChildren, t -> !predicate.test(t));
        }
    }

    /**
     * children bone state
     */
    enum State {
        /**
         * Apply with children too
         */
        TRUE,
        /**
         * Doesn't apply children
         */
        FALSE,
        /**
         * Ignore parent's result
         */
        NOT_SET
    }

    /**
     * Gets children predicate
     * @param parentSuccess result at parent bone
     * @return bone predicate
     */
    @ApiStatus.Internal
    default @NotNull BonePredicate children(boolean parentSuccess) {
        return parentSuccess ? switch (applyAtChildren()) {
            case TRUE -> BonePredicate.TRUE;
            case FALSE -> BonePredicate.FALSE;
            case NOT_SET -> this;
        } : this;
    }

    /**
     * Builder
     */
    @FunctionalInterface
    interface Builder extends Predicate<RenderedBone> {

        /**
         * Builds with child state
         * @return bone predicate
         */
        default @NotNull BonePredicate notSet() {
            return build(State.NOT_SET);
        }

        /**
         * Builds with applying children bone
         * @return bone predicate
         */
        default @NotNull BonePredicate withChildren() {
            return build(State.TRUE);
        }

        /**
         * Builds without applying children bone
         * @return bone predicate
         */
        default @NotNull BonePredicate withoutChildren() {
            return build(State.FALSE);
        }

        /**
         * Builds with child state
         * @param state state
         * @return bone predicate
         */
        default @NotNull BonePredicate build(@NotNull State state) {
            return of(state, this);
        }

        @Override
        @NotNull
        default Builder and(@NotNull Predicate<? super RenderedBone> other) {
            return bone -> test(bone) && other.test(bone);
        }

        @Override
        @NotNull
        default Builder or(@NotNull Predicate<? super RenderedBone> other) {
            return bone -> test(bone) || other.test(bone);
        }

        @Override
        @NotNull
        default Builder negate() {
            return bone -> !test(bone);
        }
    }
}

/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.bone.RenderedBone;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Animation predicate
 */
public interface AnimationPredicate extends Predicate<RenderedBone> {
    /**
     * Checks current bone is children bone
     * @return is children
     */
    @ApiStatus.Internal
    boolean isChildren();

    /**
     * Creates predicate
     * @param bonePredicate predicate
     * @return animation predicate
     */
    static @NotNull AnimationPredicate of(@NotNull Predicate<RenderedBone> bonePredicate) {
        return of(false, bonePredicate);
    }

    /**
     * Creates predicate
     * @param children children
     * @param bonePredicate predicate
     * @return animation predicate
     */
    static @NotNull AnimationPredicate of(boolean children, @NotNull Predicate<RenderedBone> bonePredicate) {
        Objects.requireNonNull(bonePredicate, "bonePredicate cannot be null.");
        return new AnimationPredicate() {
            @Override
            public boolean isChildren() {
                return children;
            }

            @Override
            public boolean test(RenderedBone renderedBone) {
                return bonePredicate.test(renderedBone);
            }
        };
    }

    /**
     * Creates children predicate
     * @return children predicate
     */
    default @NotNull AnimationPredicate children() {
        return isChildren() ? this : of(true, this);
    }
}

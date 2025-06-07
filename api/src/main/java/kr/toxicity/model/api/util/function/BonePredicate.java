package kr.toxicity.model.api.util.function;

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
     * Should apply at children bone too
     * @return apply at children
     */
    @NotNull State applyAtChildren();

    /**
     * Gets bone predicate
     * @param predicate predicate
     * @return bone predicate
     */
    static @NotNull BonePredicate of(@NotNull Predicate<RenderedBone> predicate) {
        return of(State.FALSE, predicate);
    }

    /**
     * Gets bone predicate
     * @param applyAtChildren apply at children
     * @param predicate predicate
     * @return bone predicate
     */
    static @NotNull BonePredicate of(@NotNull State applyAtChildren, @NotNull Predicate<RenderedBone> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null.");
        return new BonePredicate() {
            @Override
            public @NotNull State applyAtChildren() {
                return applyAtChildren;
            }

            @Override
            public boolean test(RenderedBone renderedBone) {
                return predicate.test(renderedBone);
            }
        };
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
}

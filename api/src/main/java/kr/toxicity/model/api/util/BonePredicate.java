package kr.toxicity.model.api.util;

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
    BonePredicate TRUE = of(true, b -> true);

    /**
     * False
     */
    BonePredicate FALSE = of(false, b -> false);

    /**
     * Should apply at children bone too
     * @return apply at children
     */
    boolean applyAtChildren();

    /**
     * Gets bone predicate
     * @param predicate predicate
     * @return bone predicate
     */
    static @NotNull BonePredicate of(@NotNull Predicate<RenderedBone> predicate) {
        return of(false, predicate);
    }

    /**
     * Gets bone predicate
     * @param applyAtChildren apply at children
     * @param predicate predicate
     * @return bone predicate
     */
    static @NotNull BonePredicate of(boolean applyAtChildren, @NotNull Predicate<RenderedBone> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null.");
        return new BonePredicate() {
            @Override
            public boolean applyAtChildren() {
                return applyAtChildren;
            }

            @Override
            public boolean test(RenderedBone renderedBone) {
                return predicate.test(renderedBone);
            }
        };
    }

    /**
     * Gets children predicate
     * @param parentSuccess result at parent bone
     * @return bone predicate
     */
    @ApiStatus.Internal
    default @NotNull BonePredicate children(boolean parentSuccess) {
        return parentSuccess ? applyAtChildren() ? TRUE : FALSE : this;
    }
}

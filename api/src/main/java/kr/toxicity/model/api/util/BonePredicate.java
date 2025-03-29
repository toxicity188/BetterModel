package kr.toxicity.model.api.util;

import kr.toxicity.model.api.bone.RenderedBone;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

public interface BonePredicate extends Predicate<RenderedBone> {

    BonePredicate TRUE = of(true, b -> true);
    BonePredicate FALSE = of(false, b -> false);

    boolean applyAtChildren();

    static @NotNull BonePredicate of(@NotNull Predicate<RenderedBone> predicate) {
        return of(false, predicate);
    }

    static @NotNull BonePredicate of(boolean applyAtChildren, @NotNull Predicate<RenderedBone> predicate) {
        Objects.requireNonNull(predicate);
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

    default @NotNull BonePredicate children(boolean parentSuccess) {
        return parentSuccess ? applyAtChildren() ? TRUE : FALSE : this;
    }
}

package kr.toxicity.model.api.animation;

import kr.toxicity.model.api.bone.RenderedBone;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

public interface AnimationPredicate extends Predicate<RenderedBone> {
    boolean isChildren();

    static @NotNull AnimationPredicate of(@NotNull Predicate<RenderedBone> bonePredicate) {
        return of(false, bonePredicate);
    }

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

    default @NotNull AnimationPredicate children() {
        return isChildren() ? this : of(true, this);
    }
}

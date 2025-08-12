package kr.toxicity.model.api.script;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.data.raw.ModelAnimation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A script data of blueprint.
 * @param name script name
 * @param type type
 * @param length playtime
 * @param scripts scripts
 */
@ApiStatus.Internal
public record BlueprintScript(@NotNull String name, @NotNull AnimationIterator.Type type, float length, @NotNull List<TimeScript> scripts) {

    /**
     * Creates empty script
     * @param animation animation
     * @return empty script
     */
    public static @NotNull BlueprintScript fromEmpty(@NotNull ModelAnimation animation) {
        return new BlueprintScript(
                animation.name(),
                animation.loop(),
                animation.length(),
                List.of(TimeScript.EMPTY, AnimationScript.EMPTY.time(animation.length()))
        );
    }

    /**
     * Creates animation iterator of this script
     * @return animation iterator
     */
    public @NotNull AnimationIterator<TimeScript> iterator() {
        return type.create(scripts);
    }
}

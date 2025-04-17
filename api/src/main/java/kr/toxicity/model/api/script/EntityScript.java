package kr.toxicity.model.api.script;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * Entity script
 */
@FunctionalInterface
public interface EntityScript extends Consumer<Entity> {

    /**
     * Empty script
     */
    EntityScript EMPTY = e -> {};

    @NotNull
    @Override
    default EntityScript andThen(@NotNull Consumer<? super Entity> after) {
        return e -> {
            accept(e);
            after.accept(e);
        };
    }

    /**
     * Creates a timed script
     * @param time time
     * @return timed script
     */
    default @NotNull TimeScript time(int time) {
        return new TimeScript(time, this);
    }

    /**
     * Sums a script list to one
     * @param scriptList list of a script
     * @return merged script
     */
    static @NotNull EntityScript of(@NotNull List<EntityScript> scriptList) {
        return switch (scriptList.size()) {
            case 0 -> EMPTY;
            case 1 -> scriptList.getFirst();
            default -> e -> {
                for (EntityScript entityScript : scriptList) {
                    entityScript.accept(e);
                }
            };
        };
    }
}

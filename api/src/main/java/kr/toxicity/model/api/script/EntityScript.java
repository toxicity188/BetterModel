package kr.toxicity.model.api.script;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

@FunctionalInterface
public interface EntityScript extends Consumer<Entity> {
    @NotNull
    @Override
    default EntityScript andThen(@NotNull Consumer<? super Entity> after) {
        return e -> {
            accept(e);
            after.accept(e);
        };
    }

    default @NotNull TimeScript time(int time) {
        return new TimeScript(time, this);
    }

    static @NotNull EntityScript of(@NotNull List<EntityScript> scriptList) {
        return e -> {
            for (EntityScript entityScript : scriptList) {
                entityScript.accept(e);
            }
        };
    }
}

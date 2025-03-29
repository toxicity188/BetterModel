package kr.toxicity.model.api.script;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Script with time
 * @param time time
 * @param script source script
 */
public record TimeScript(int time, @NotNull EntityScript script) implements EntityScript {
    @Override
    public void accept(Entity entity) {
        script.accept(entity);
    }
}

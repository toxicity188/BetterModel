package kr.toxicity.model.api.script;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public record TimeScript(int time, @NotNull EntityScript script) implements EntityScript {
    @Override
    public void accept(Entity entity) {
        script.accept(entity);
    }
}

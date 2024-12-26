package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.data.renderer.BlueprintRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ModelManager extends GlobalManager {
    @Nullable BlueprintRenderer renderer(@NotNull String name);
    @NotNull @Unmodifiable
    List<BlueprintRenderer> renderers();
    @NotNull @Unmodifiable
    Set<String> keys();

    default @Nullable EntityTracker tracker(@NotNull Entity entity) {
        return EntityTracker.tracker(entity);
    }
    default @Nullable EntityTracker tracker(@NotNull UUID uuid) {
        return EntityTracker.tracker(uuid);
    }
}

package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.data.renderer.BlueprintRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Set;

public interface ModelManager extends GlobalManager {
    @Nullable BlueprintRenderer renderer(@NotNull String name);
    @NotNull @Unmodifiable
    List<BlueprintRenderer> renderers();
    @NotNull @Unmodifiable
    Set<String> keys();
}

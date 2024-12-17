package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.data.renderer.BlueprintRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ModelManager extends GlobalManager {
    @Nullable BlueprintRenderer renderer(@NotNull String name);
}

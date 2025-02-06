package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.script.EntityScript;
import kr.toxicity.model.api.script.EntityScriptBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ScriptManager extends GlobalManager {
    @Nullable EntityScript build(@NotNull String script);
    void addBuilder(@NotNull String name, @NotNull EntityScriptBuilder script);
}

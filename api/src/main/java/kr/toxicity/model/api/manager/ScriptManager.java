package kr.toxicity.model.api.manager;

import kr.toxicity.model.api.script.EntityScript;
import kr.toxicity.model.api.script.EntityScriptBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Script manager
 */
public interface ScriptManager extends GlobalManager {
    /**
     * Creates a script for line
     * @param script raw script
     * @return entity script
     */
    @Nullable EntityScript build(@NotNull String script);

    /**
     * Adds script parser to registry
     * @param name parser name
     * @param script script builder
     */
    void addBuilder(@NotNull String name, @NotNull EntityScriptBuilder script);
}

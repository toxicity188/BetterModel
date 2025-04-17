package kr.toxicity.model.api.script;

import org.jetbrains.annotations.NotNull;

/**
 * Script parser
 */
@FunctionalInterface
public interface EntityScriptBuilder {
    /**
     * Build an entity script by line
     * @param arg raw line
     * @return script
     */
    @NotNull EntityScript build(@NotNull String arg);
}

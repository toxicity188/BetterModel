package kr.toxicity.model.api.script;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface EntityScriptBuilder {
    @NotNull EntityScript build(@NotNull String arg);
}

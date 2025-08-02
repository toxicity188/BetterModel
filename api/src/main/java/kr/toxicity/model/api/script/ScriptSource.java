package kr.toxicity.model.api.script;

import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.data.renderer.RenderSource;
import org.jetbrains.annotations.NotNull;

/**
 * Script source
 * @param pipeline pipeline
 * @param source source
 */
public record ScriptSource(@NotNull RenderPipeline pipeline, @NotNull RenderSource<?> source) {
}
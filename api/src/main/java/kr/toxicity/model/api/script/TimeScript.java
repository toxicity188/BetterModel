package kr.toxicity.model.api.script;

import kr.toxicity.model.api.animation.Timed;
import kr.toxicity.model.api.data.renderer.RenderSource;
import org.jetbrains.annotations.NotNull;

/**
 * Script with time
 * @param time time
 * @param script source script
 */
public record TimeScript(float time, @NotNull AnimationScript script) implements AnimationScript, Timed {

    public static final TimeScript EMPTY = AnimationScript.EMPTY.time(0);

    @Override
    public boolean isSync() {
        return script.isSync();
    }

    @Override
    public void accept(@NotNull RenderSource<?> renderSource) {
        script.accept(renderSource);
    }

    public @NotNull TimeScript time(float time) {
        return new TimeScript(time, script);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeScript that)) return false;
        return Float.compare(time, that.time) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(time);
    }
}

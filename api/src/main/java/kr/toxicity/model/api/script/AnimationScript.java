package kr.toxicity.model.api.script;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Animation script
 */
public interface AnimationScript extends Consumer<ScriptSource> {

    boolean isSync();
    @Override
    void accept(@NotNull ScriptSource renderSource);

    /**
     * Empty script
     */
    AnimationScript EMPTY = of(s -> {});

    static @NotNull AnimationScript of(@NotNull Consumer<ScriptSource> source) {
        return of(false, source);
    }

    static @NotNull AnimationScript of(boolean isSync, @NotNull Consumer<ScriptSource> source) {
        Objects.requireNonNull(source);
        return new AnimationScript() {
            @Override
            public boolean isSync() {
                return isSync;
            }

            @Override
            public void accept(@NotNull ScriptSource renderSource) {
                source.accept(renderSource);
            }
        };
    }

    /**
     * Creates a timed script
     * @param time time
     * @return timed script
     */
    default @NotNull TimeScript time(float time) {
        return new TimeScript(time, this);
    }

    /**
     * Sums a script list to one
     * @param scriptList list of a script
     * @return merged script
     */
    static @NotNull AnimationScript of(@NotNull List<AnimationScript> scriptList) {
        return switch (scriptList.size()) {
            case 0 -> EMPTY;
            case 1 -> scriptList.getFirst();
            default -> {
                var sync = false;
                Consumer<ScriptSource> consumer = trigger -> {};
                for (AnimationScript entityScript : scriptList) {
                    sync = sync || entityScript.isSync();
                    consumer = consumer.andThen(entityScript);
                }
                yield of(sync, consumer);
            }
        };
    }
}

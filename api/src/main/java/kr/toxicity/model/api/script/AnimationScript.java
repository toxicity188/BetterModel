/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.script;

import kr.toxicity.model.api.tracker.Tracker;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Animation script
 */
public interface AnimationScript extends Consumer<Tracker> {

    /**
     * Empty script
     */
    AnimationScript EMPTY = of(s -> {});

    @Override
    void accept(@NotNull Tracker tracker);

    /**
     * Checks this script should be called in tick thread
     * @return requires tick thread
     */
    boolean isSync();

    /**
     * Creates a timed script
     * @param time time
     * @return timed script
     */
    default @NotNull TimeScript time(float time) {
        return new TimeScript(time, this);
    }

    /**
     * Creates script
     * @param source consumer
     * @return script
     */
    static @NotNull AnimationScript of(@NotNull Consumer<Tracker> source) {
        return of(false, source);
    }

    /**
     * Creates script
     * @param isSync should be called in tick thread
     * @param source consumer
     * @return script
     */
    static @NotNull AnimationScript of(boolean isSync, @NotNull Consumer<Tracker> source) {
        Objects.requireNonNull(source);
        return new AnimationScript() {
            @Override
            public boolean isSync() {
                return isSync;
            }

            @Override
            public void accept(@NotNull Tracker tracker) {
                source.accept(tracker);
            }
        };
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
                Consumer<Tracker> consumer = trigger -> {};
                for (AnimationScript entityScript : scriptList) {
                    sync = sync || entityScript.isSync();
                    consumer = consumer.andThen(entityScript);
                }
                yield of(sync, consumer);
            }
        };
    }
}

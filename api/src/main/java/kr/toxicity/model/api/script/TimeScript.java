/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.script;

import kr.toxicity.model.api.animation.Timed;
import kr.toxicity.model.api.tracker.Tracker;
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
    public void accept(@NotNull Tracker tracker) {
        script.accept(tracker);
    }

    public @NotNull TimeScript time(float newTime) {
        if (time == newTime) return this;
        return new TimeScript(newTime, script);
    }
}

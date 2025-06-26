package kr.toxicity.model.api.script;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A script data of blueprint.
 * @param name script name
 * @param length playtime
 * @param scripts scripts
 */
@ApiStatus.Internal
public record BlueprintScript(@NotNull String name, int length, @NotNull List<TimeScript> scripts) {

    /**
     * Creates an empty script.
     * @param animation animation
     * @return blueprint script
     */
    public static @NotNull BlueprintScript emptyOf(@NotNull ModelAnimation animation) {
        return new BlueprintScript(animation.name(), 0, Collections.emptyList());
    }

    /**
     * Creates animation's script
     * @param animation animation
     * @param animator animator
     * @return blueprint script
     */
    public static @NotNull BlueprintScript from(@NotNull ModelAnimation animation, @NotNull ModelAnimator animator) {
        return new BlueprintScript(
                animation.name(),
                Math.round((animation.length() - (float) animator.keyframes().stream().mapToDouble(ModelKeyframe::time).max().orElse(0)) * 20),
                processFrame(animator.keyframes())
                        .stream()
                        .map(d -> EntityScript.of(d.dataPoints()
                                .stream()
                                .map(p -> {
                                    var raw = p.script();
                                    if (raw == null) return null;
                                    return BetterModel.plugin().scriptManager().build(raw);
                                })
                                .filter(Objects::nonNull)
                                .toList()).time(Math.round(d.time() * 20)))
                        .toList());
    }

    /**
     * Script reader
     */
    public interface ScriptReader {
        /**
         * Ticks this reader
         * @return end
         */
        boolean tick();

        /**
         * Clears this reader
         */
        void clear();

        /**
         * Gets current entity script
         * @return script or null
         */
        @Nullable EntityScript script();
    }

    /**
     * Gets script reader by once
     * @param delay delay
     * @param speed speed
     * @return reader
     */
    public @NotNull ScriptReader single(int delay, float speed) {
        return new SingleScriptReader(delay, speed);
    }

    /**
     * Gets script reader by loop
     * @param delay delay
     * @param speed speed
     * @return reader
     */
    public @NotNull ScriptReader loop(int delay, float speed) {
        return new LoopScriptReader(delay, speed);
    }

    /**
     * Gets script reader by hold on last
     * @param delay delay
     * @param speed speed
     * @return reader
     */
    public @NotNull ScriptReader holdOn(int delay, float speed) {
        return new HoldOnLastScriptReader(delay, speed);
    }

    private class SingleScriptReader implements ScriptReader {

        private final int initialDelay;
        private int index, delay;
        private final float speed;
        private @Nullable EntityScript script;

        private SingleScriptReader(int initialDelay, float speed) {
            this.initialDelay = delay = initialDelay;
            this.speed = speed;
        }

        @Override
        public void clear() {
            index = 0;
            delay = initialDelay;
        }

        @Override
        public boolean tick() {
            if (--delay <= 0) {
                if (index >= scripts.size()) {
                    return true;
                }
                var next = scripts.get(index++);
                delay = Math.round((float) next.time() / speed);
                script = next.script();
            } else {
                script = null;
            }
            return false;
        }

        @Nullable
        @Override
        public EntityScript script() {
            return script;
        }
    }

    private class LoopScriptReader implements ScriptReader {

        private final int initialDelay;
        private int index, delay;
        private final float speed;
        private @Nullable EntityScript script;

        private LoopScriptReader(int initialDelay, float speed) {
            this.initialDelay = delay = initialDelay;
            this.speed = speed;
        }

        @Override
        public void clear() {
            index = 0;
            delay = initialDelay;
        }

        @Override
        public boolean tick() {
            if (--delay <= 0) {
                if (index >= scripts.size()) {
                    delay = Math.round((float) length / speed);
                    index = 0;
                    script = null;
                    return false;
                }
                var next = scripts.get(index++);
                delay = Math.round(next.time() * 20 / speed);
                script = next.script();
            } else {
                script = null;
            }
            return false;
        }

        @Nullable
        @Override
        public EntityScript script() {
            return script;
        }
    }

    private class HoldOnLastScriptReader implements ScriptReader {

        private final int initialDelay;
        private int index, delay;
        private final float speed;
        private @Nullable EntityScript script;

        private HoldOnLastScriptReader(int initialDelay, float speed) {
            this.initialDelay = delay = initialDelay;
            this.speed = speed;
        }

        @Override
        public void clear() {
            index = 0;
            delay = initialDelay;
        }

        @Override
        public boolean tick() {
            if (--delay <= 0) {
                if (index >= scripts.size()) {
                    script = null;
                    return false;
                }
                var next = scripts.get(index++);
                delay = Math.round(next.time() * 20 / speed);
                script = next.script();
            } else {
                script = null;
            }
            return false;
        }

        @Nullable
        @Override
        public EntityScript script() {
            return script;
        }
    }

    private static @NotNull List<ModelKeyframe> processFrame(@NotNull List<ModelKeyframe> target) {
        if (target.size() <= 1) return target;
        return IntStream.range(0, target.size()).mapToObj(i -> {
            if (i == 0) return target.getFirst();
            else {
                var get = target.get(i);
                return get.time(get.time() - target.get(i - 1).time());
            }
        }).toList();
    }
}

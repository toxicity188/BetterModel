package kr.toxicity.model.api.script;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.raw.ModelAnimation;
import kr.toxicity.model.api.data.raw.ModelAnimator;
import kr.toxicity.model.api.data.raw.ModelKeyframe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record BlueprintScript(@NotNull String name, int length, @NotNull List<TimeScript> scripts) {

    public static @NotNull BlueprintScript emptyOf(@NotNull ModelAnimation animation) {
        return new BlueprintScript(animation.name(), 0, Collections.emptyList());
    }

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
                                    return BetterModel.inst().scriptManager().build(raw);
                                })
                                .filter(Objects::nonNull)
                                .toList()).time(Math.round(d.time() * 20)))
                        .toList());
    }

    public interface ScriptReader {
        boolean tick();
        void clear();
        @Nullable EntityScript script();
    }

    public @NotNull ScriptReader single(int delay, float speed) {
        return new SingleScriptReader(delay, speed);
    }

    public @NotNull ScriptReader loop(int delay, float speed) {
        return new LoopScriptReader(delay, speed);
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

    private static @NotNull List<ModelKeyframe> processFrame(@NotNull List<ModelKeyframe> target) {
        if (target.size() <= 1) return target;
        var list = new ArrayList<ModelKeyframe>();
        for (int i = 1; i < target.size(); i++) {
            var get = target.get(i);
            list.add(get.time(get.time() - target.get(i - 1).time()));
        }
        return list;
    }
}

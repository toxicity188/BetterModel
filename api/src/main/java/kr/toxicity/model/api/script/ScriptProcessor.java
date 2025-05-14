package kr.toxicity.model.api.script;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.function.Supplier;

public final class ScriptProcessor {
    private final SequencedMap<String, PredicatedScript> scriptMap = new LinkedHashMap<>();
    private final Collection<PredicatedScript> scriptView = scriptMap.sequencedValues().reversed();
    @Getter
    private @Nullable BlueprintScript.ScriptReader currentReader;

    public void animate(@NotNull BlueprintScript script, @NotNull AnimationIterator.Type defaultType, @NotNull AnimationModifier modifier) {
        synchronized (scriptMap) {
            var type = modifier.type() != null ? modifier.type() : defaultType;
            scriptMap.putLast(script.name(), new PredicatedScript(script.name(), modifier.predicate(), switch (type) {
                case PLAY_ONCE -> script.single(modifier.start(), modifier.speedValue());
                case LOOP -> script.loop(modifier.start(), modifier.speedValue());
                case HOLD_ON_LAST -> script.holdOn(modifier.start(), modifier.speedValue());
            }));
        }
    }

    public void replace(@NotNull BlueprintScript script,@ NotNull AnimationIterator.Type defaultType, @NotNull AnimationModifier modifier) {
        synchronized (scriptMap) {
            var type = modifier.type() != null ? modifier.type() : defaultType;
            scriptMap.replace(script.name(), new PredicatedScript(script.name(), modifier.predicate(), switch (type) {
                case PLAY_ONCE -> script.single(modifier.start(), modifier.speedValue());
                case LOOP -> script.loop(modifier.start(), modifier.speedValue());
                case HOLD_ON_LAST -> script.holdOn(modifier.start(), modifier.speedValue());
            }));
        }
    }

    public void stopAnimation(@NotNull String animation) {
        synchronized (scriptMap) {
            scriptMap.remove(animation);
        }
    }

    public void tick() {
        synchronized (scriptMap) {
            var check = true;
            var iterator = scriptView.iterator();
            while (iterator.hasNext()) {
                var predicatedScript = iterator.next();
                if (predicatedScript.supplier.get() && check) {
                    check = false;
                    if (predicatedScript.reader.tick()) iterator.remove();
                    else currentReader = predicatedScript.reader;
                } else {
                    predicatedScript.reader.clear();
                }
            }
            if (check) currentReader = null;
        }
    }

    private record PredicatedScript(@NotNull String name, @NotNull Supplier<Boolean> supplier, @NotNull BlueprintScript.ScriptReader reader) {}
}

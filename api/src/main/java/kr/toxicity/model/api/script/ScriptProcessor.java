package kr.toxicity.model.api.script;

import kr.toxicity.model.api.animation.AnimationModifier;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ScriptProcessor {
    private final Map<String, PredicatedScript> scriptMap = new LinkedHashMap<>();
    @Getter
    private @Nullable BlueprintScript.ScriptReader currentReader;

    public void animateSingle(@NotNull BlueprintScript script, @NotNull AnimationModifier modifier) {
        synchronized (scriptMap) {
            scriptMap.put(script.name(), new PredicatedScript(script.name(), modifier.predicate(), script.single(modifier.start(), modifier.speedValue())));
        }
    }
    public void animateLoop(@NotNull BlueprintScript script, @NotNull AnimationModifier modifier) {
        synchronized (scriptMap) {
            scriptMap.put(script.name(), new PredicatedScript(script.name(), modifier.predicate(), script.loop(modifier.start(), modifier.speedValue())));
        }
    }

    public void replaceSingle(@NotNull BlueprintScript script, @NotNull AnimationModifier modifier) {
        synchronized (scriptMap) {
            scriptMap.replace(script.name(), new PredicatedScript(script.name(), modifier.predicate(), script.single(modifier.start(), modifier.speedValue())));
        }
    }
    public void replaceLoop(@NotNull BlueprintScript script, @NotNull AnimationModifier modifier) {
        synchronized (scriptMap) {
            scriptMap.replace(script.name(), new PredicatedScript(script.name(), modifier.predicate(), script.loop(modifier.start(), modifier.speedValue())));
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
            for (PredicatedScript predicatedScript : new ArrayList<>(scriptMap.values()).reversed()) {
                if (predicatedScript.supplier.get() && check) {
                    check = false;
                    if (predicatedScript.reader.tick()) scriptMap.remove(predicatedScript.name);
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

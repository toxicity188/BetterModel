package kr.toxicity.model.api.pack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;

public record PackData(@NotNull @Unmodifiable Map<PackPath, byte[]> bytes, long time) {
}

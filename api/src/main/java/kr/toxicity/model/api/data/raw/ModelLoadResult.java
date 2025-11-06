/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Model load result
 * @param blueprint blueprint
 * @param errors error messages
 */
public record ModelLoadResult(@NotNull ModelBlueprint blueprint, @NotNull @Unmodifiable List<String> errors) {
}

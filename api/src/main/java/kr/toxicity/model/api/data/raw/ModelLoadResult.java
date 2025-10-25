/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ModelLoadResult(@NotNull ModelBlueprint blueprint, @NotNull List<String> errors) {
}

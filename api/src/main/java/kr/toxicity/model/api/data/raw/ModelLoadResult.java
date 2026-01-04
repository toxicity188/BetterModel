/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.raw;

import kr.toxicity.model.api.data.blueprint.ModelBlueprint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Represents the result of loading and processing a raw model file.
 * <p>
 * This record contains the successfully created {@link ModelBlueprint} and a list of any errors
 * or warnings that occurred during the loading process.
 * </p>
 *
 * @param blueprint the processed model blueprint
 * @param errors a list of error or warning messages generated during loading
 * @since 1.15.2
 */
public record ModelLoadResult(@NotNull ModelBlueprint blueprint, @NotNull @Unmodifiable List<String> errors) {
}

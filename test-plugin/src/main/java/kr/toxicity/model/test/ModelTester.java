/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.test;

import org.jetbrains.annotations.NotNull;

public interface ModelTester {
    void start(@NotNull BetterModelTest test);
    void end(@NotNull BetterModelTest test);
}

/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.platform;

public enum PlatformBillboard {
    /**
     * No rotation (default).
     */
    FIXED,
    /**
     * Can pivot around vertical axis.
     */
    VERTICAL,
    /**
     * Can pivot around horizontal axis.
     */
    HORIZONTAL,
    /**
     * Can pivot around center point.
     */
    CENTER
}

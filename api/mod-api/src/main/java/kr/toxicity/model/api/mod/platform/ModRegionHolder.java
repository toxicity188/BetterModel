/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.mod.platform;

import kr.toxicity.model.api.platform.PlatformRegionHolder;

/**
 * Represents a Mod-specific region holder for managing thread-safe operations.
 * <p>
 * This interface extends {@link PlatformRegionHolder} to provide Mod-specific functionality
 * for scheduling tasks within specific regions or contexts.
 * </p>
 *
 * @since 2.0.0
 */
public interface ModRegionHolder extends PlatformRegionHolder {
}

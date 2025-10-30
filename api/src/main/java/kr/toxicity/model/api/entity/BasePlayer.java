/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.nms.Profiled;

/**
 * An adapter of player
 */
public interface BasePlayer extends BaseEntity, Profiled {

    /**
     * Updates current inventory
     */
    void updateInventory();
}

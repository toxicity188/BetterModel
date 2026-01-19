/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.fabric.entity;

import kr.toxicity.model.api.entity.BaseEntity;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public interface BaseFabricEntity extends BaseEntity {

    default @NotNull Entity entity() {
        return (Entity) handle();
    }

    void entity(@NotNull Entity entity);
}

/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
@file:Suppress("UnstableApiUsage")

package kr.toxicity.model.impl.fabric.world

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.fabricmc.fabric.mixin.networking.accessor.ChunkMapAccessor
import net.fabricmc.fabric.mixin.networking.accessor.EntityTrackerAccessor
import net.minecraft.server.level.ChunkMap

val ChunkMap.entityMap: Int2ObjectMap<EntityTrackerAccessor>
    get() {
        return (this as ChunkMapAccessor).entityMap
    }

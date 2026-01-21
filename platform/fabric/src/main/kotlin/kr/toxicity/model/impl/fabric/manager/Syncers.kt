/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.impl.fabric.manager

import kr.toxicity.model.mixin.SynchedEntityDataAccessor
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData

fun <T : Any> SynchedEntityData.markDirty(accessor: EntityDataAccessor<T>) {
    (this as SynchedEntityDataAccessor).`bettermodel$getItem`(accessor).isDirty = true
    `bettermodel$setDirty`(true)
}

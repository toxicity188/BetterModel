/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.fabric

import kr.toxicity.model.api.fabric.platform.*
import kr.toxicity.model.api.platform.*

val PlatformEntity.asFabric get() = this as FabricEntity

val PlatformItemStack.asFabric get() = this as FabricItemStack

val PlatformLivingEntity.asFabric get() = this as FabricLivingEntity

val PlatformLocation.asFabric get() = this as FabricLocation

val PlatformOfflinePlayer.asFabric get() = this as FabricOfflinePlayer

val PlatformPlayer.asFabric get() = this as FabricPlayer

val PlatformWorld.asFabric get() = this as FabricWorld

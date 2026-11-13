/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines platform-neutral wrappers for server, entity, item, and world types.
 * <p>
 * Platform contracts allow the API and core logic to describe locations,
 * worlds, entities, players, item stacks, namespaces, transforms, and billboards
 * without accepting Bukkit, Fabric, or other runtime classes directly.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * PlatformEntity entity = adapter.entity(nativeEntity);
 * PlatformLocation location = entity.location();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.platform;

/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Describes normalized model blueprint data used after raw import.
 * <p>
 * Blueprint types represent model elements, textures, animations, bounding
 * boxes, generated animation points, and load context data in the shape used by
 * renderers. This layer is still an API data contract and does not own runtime
 * rendering or platform resource delivery.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ModelBlueprint blueprint = result.blueprint();
 * var animations = blueprint.animations();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.data.blueprint;

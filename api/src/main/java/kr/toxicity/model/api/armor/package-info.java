/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Describes player armor model data exposed by the API.
 * <p>
 * The package contains value contracts for armor items and the grouped armor
 * state associated with a player model. It keeps armor metadata independent
 * from Bukkit, Fabric, or other platform inventory classes.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * PlayerArmor armor = PlayerArmor.EMPTY;
 * ArmorItem helmet = armor.helmet();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.armor;

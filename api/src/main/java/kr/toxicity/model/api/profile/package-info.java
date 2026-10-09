/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines model profile and player skin lookup contracts.
 * <p>
 * Profiles describe player identity, texture information, profile suppliers,
 * and skin payloads used by player model rendering. Implementations can resolve
 * or cache profile data while consumers depend only on these API records and
 * interfaces.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ProfileManager profiles = BetterModel.platform().profileManager();
 * ModelProfile.Uncompleted profile = profiles.supplier().supply(player);
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.profile;

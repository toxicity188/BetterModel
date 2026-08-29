/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Contains small JSON builder helpers for API data serialization.
 * <p>
 * The builders wrap common Gson object and array construction patterns used by
 * resource-pack, model, and metadata generation code while keeping JSON creation
 * readable in Java.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * var json = JsonObjectBuilder.builder()
 *         .property("pack_format", 26)
 *         .build();
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.util.json;

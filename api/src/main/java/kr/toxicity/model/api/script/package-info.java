/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

/**
 * Defines animation and blueprint scripting contracts.
 * <p>
 * Script types represent executable animation scripts, time-bound script
 * entries, blueprint script definitions, and builders that turn script text into
 * runtime actions. The package keeps script contracts independent from parser or
 * command implementation details.
 * </p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ScriptManager scripts = BetterModel.platform().scriptManager();
 * AnimationScript script = scripts.build("variable.foo = 1");
 * }</pre>
 *
 * @since 3.2.0
 */
package kr.toxicity.model.api.script;

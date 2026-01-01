/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import kr.toxicity.model.api.BetterModel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines a strategy for obfuscating resource names in the pack.
 * <p>
 * Obfuscation can help reduce file path lengths and protect asset names.
 * </p>
 *
 * @since 1.15.2
 */
public interface PackObfuscator {

    /**
     * A no-op obfuscator that returns the name as-is.
     * @since 1.15.2
     */
    PackObfuscator NONE = name -> name;

    /**
     * Obfuscates the given raw name.
     *
     * @param rawName the original name
     * @return the obfuscated name
     * @since 1.15.2
     */
    @NotNull String obfuscate(@NotNull String rawName);

    /**
     * Creates an order-based obfuscator if obfuscation is enabled in the configuration.
     *
     * @return the obfuscator
     * @since 1.15.2
     */
    static @NotNull PackObfuscator order() {
        return BetterModel.config().pack().useObfuscation() ? new Order() : NONE;
    }

    /**
     * Creates a pair obfuscator, combining this obfuscator (as textures) with another (as models).
     *
     * @param models the models obfuscator
     * @return the pair obfuscator
     * @since 1.15.2
     */
    default @NotNull Pair withModels(@NotNull PackObfuscator models) {
        return pair(models, this);
    }

    /**
     * Creates a pair obfuscator from two separate obfuscators.
     *
     * @param models the models obfuscator
     * @param textures the textures obfuscator
     * @return the pair obfuscator
     * @since 1.15.2
     */
    static @NotNull Pair pair(@NotNull PackObfuscator models, @NotNull PackObfuscator textures) {
        return new Pair(models, textures);
    }

    /**
     * An obfuscator that generates short names based on the order of appearance.
     *
     * @since 1.15.2
     */
    final class Order implements PackObfuscator {

        private static final char[] AVAILABLE_NAME = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g',
            'h', 'i', 'j', 'k', 'm', 'n', 'l', 'o', 'p',
            'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6' ,'7', '8', '9'
        };
        private static final int NAME_LENGTH = AVAILABLE_NAME.length;

        private final Map<String, String> nameMap = new ConcurrentHashMap<>();
        private final StringBuilder builder = new StringBuilder();

        /**
         * Private initializer
         */
        private Order() {
        }

        public @NotNull String obfuscate(@NotNull String rawName) {
            return nameMap.computeIfAbsent(rawName, n -> {
                var size = nameMap.size();
                builder.setLength(0);
                while (size >= NAME_LENGTH) {
                    builder.append(AVAILABLE_NAME[size % NAME_LENGTH]);
                    size /= NAME_LENGTH;
                }
                builder.append(AVAILABLE_NAME[size % NAME_LENGTH]);
                return builder.toString();
            });
        }
    }

    /**
     * A pair of obfuscators for models and textures.
     *
     * @param models the models obfuscator
     * @param textures the textures obfuscator
     * @since 1.15.2
     */
    record Pair(@NotNull PackObfuscator models, @NotNull PackObfuscator textures) {}
}

/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.pack;

import kr.toxicity.model.api.BetterModel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pack obfuscator
 */
public interface PackObfuscator {

    /**
     * None
     */
    PackObfuscator NONE = name -> name;

    /**
     * Obfuscate this name
     * @param rawName raw name
     * @return obfuscated name
     */
    @NotNull String obfuscate(@NotNull String rawName);

    /**
     * Creates order obfuscator
     * @return order obfuscator
     */
    static @NotNull PackObfuscator order() {
        return BetterModel.config().pack().useObfuscation() ? new Order() : NONE;
    }

    /**
     * Creates pair with models
     * @param models models obfuscator
     * @return obfuscator pair
     */
    default @NotNull Pair withModels(@NotNull PackObfuscator models) {
        return pair(models, this);
    }

    /**
     * Creates pair obfuscator
     * @param models models obfuscator
     * @param textures textures obfuscator
     * @return pair obfuscator
     */
    static @NotNull Pair pair(@NotNull PackObfuscator models, @NotNull PackObfuscator textures) {
        return new Pair(models, textures);
    }

    /**
     * Obfuscate by order
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
     * Pair obfuscator
     * @param models models obfuscator
     * @param textures textures obfuscator
     */
    record Pair(@NotNull PackObfuscator models, @NotNull PackObfuscator textures) {}
}

/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.impl.fabric

inline fun <H, T> dirtyChecked(crossinline hash: () -> H, crossinline function: (H) -> T): () -> T {
    val lock = Any()
    var h = hash()
    var value = function(h)
    return {
        val newH = hash()
        when {
            h === newH -> value
            h == newH -> value
            else -> synchronized(lock) {
                h = newH
                value = function(h)
                value
            }
        }
    }
}

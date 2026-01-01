/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
import kr.toxicity.model.api.util.HttpUtil
import kr.toxicity.model.api.version.MinecraftVersion
import org.junit.jupiter.api.Test

class ModrinthTest {
    @Test
    fun testModrinth() {
        println(HttpUtil.versionList(MinecraftVersion(1, 21, 8)))
    }
}

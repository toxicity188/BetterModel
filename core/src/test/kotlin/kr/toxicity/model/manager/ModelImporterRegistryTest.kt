/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.manager

import kr.toxicity.model.api.data.ModelImporter
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class ModelImporterRegistryTest {

    @Test
    fun registersBuiltInAndCustomImportersWithoutCaseSensitivity() {
        assertSame(ModelImporter.BBMODEL, ModelManagerImpl.importer("BBMODEL"))
        assertSame(ModelImporter.BBMODEL, ModelManagerImpl.importer("AJMODEL"))
        assertNull(ModelManagerImpl.importer("unregistered_test_extension"))

        val importer = ModelImporter { error("Not called during registration") }
        ModelManagerImpl.addImporter("CUSTOM_TEST_EXTENSION", importer)
        assertSame(importer, ModelManagerImpl.importer("custom_test_extension"))
        assertSame(importer, ModelManagerImpl.importer("CUSTOM_TEST_EXTENSION"))

        ModelManagerImpl.addImporter("custom_test_extension", ModelImporter.BBMODEL)
        assertSame(ModelImporter.BBMODEL, ModelManagerImpl.importer("custom_test_extension"))
    }
}

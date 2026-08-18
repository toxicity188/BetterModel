/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.nms.v26_R2

import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.entity.Display
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EntityDataTest {

    @Test
    fun `display accessors are ordered by metadata id`() {
        val ids = Display::class.java.accessors().map { it.id }

        assertTrue(ids.zipWithNext().all { (first, second) -> first < second })
    }

    @Test
    fun `transformation data uses the expected metadata types`() {
        val translation = Vector3f(1F, 2F, 3F)
        val scale = Vector3f(4F, 5F, 6F)
        val rotation = Quaternionf().rotationXYZ(0.1F, 0.2F, 0.3F)
        val packed = TransformationData().apply {
            transform(20, translation, scale, rotation)
        }.pack()

        assertEquals(listOf(8, 9, 11, 12, 13), packed.map { it.id })
        assertEquals(packed.size, packed.map { it.id }.distinct().size)

        assertSame(EntityDataSerializers.INT, packed[0].serializer)
        assertIs<Int>(packed[0].value)
        assertEquals(0, packed[0].value)

        assertSame(EntityDataSerializers.INT, packed[1].serializer)
        assertIs<Int>(packed[1].value)
        assertEquals(20, packed[1].value)

        assertSame(EntityDataSerializers.VECTOR3, packed[2].serializer)
        assertIs<Vector3f>(packed[2].value)
        assertEquals(translation, packed[2].value)

        assertSame(EntityDataSerializers.VECTOR3, packed[3].serializer)
        assertIs<Vector3f>(packed[3].value)
        assertEquals(scale, packed[3].value)

        assertSame(EntityDataSerializers.QUATERNION, packed[4].serializer)
        assertIs<Quaternionf>(packed[4].value)
        assertEquals(rotation, packed[4].value)
    }
}

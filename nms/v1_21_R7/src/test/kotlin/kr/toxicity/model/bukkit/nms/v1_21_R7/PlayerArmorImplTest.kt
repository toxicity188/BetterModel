/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.nms.v1_21_R7

import kr.toxicity.model.api.bukkit.platform.BukkitItemStack
import kr.toxicity.model.api.util.TransformedItemStack
import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomModelData
import org.bukkit.craftbukkit.inventory.CraftItemStack
import sun.misc.Unsafe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Acceptance criteria from BetterModel issue #410:
 * 1. A head item with an item-model component is preserved as a separate custom head item when
 *    BetterModel converts it for player-emote rendering.
 * 2. A head item with custom-model-data is preserved as a separate custom head item when BetterModel
 *    converts it for player-emote rendering.
 */
class PlayerArmorImplTest {

    @BeforeTest
    fun bootstrapRegistries() {
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()
    }

    @Test
    fun `preserves item model when converting head equipment`() {
        val plain = Items.IRON_HELMET.defaultInstance
        val itemModel = Identifier.parse("bettermodel:issue_410_hat")
        val customized = plain.copy().apply {
            set(DataComponents.ITEM_MODEL, itemModel)
        }

        assertEquals(itemModel, customized.get(DataComponents.ITEM_MODEL))
        assertNull(convertCustomItem(plain))
        val converted = assertNotNull(convertCustomItem(customized))
        assertEquals(itemModel, unwrap(converted).get(DataComponents.ITEM_MODEL))
    }

    @Test
    fun `preserves custom model data when converting head equipment`() {
        val plain = Items.IRON_HELMET.defaultInstance
        val customModelData = CustomModelData(
            listOf(410F),
            emptyList<Boolean>(),
            emptyList<String>(),
            emptyList<Int>()
        )
        val customized = plain.copy().apply {
            set(DataComponents.CUSTOM_MODEL_DATA, customModelData)
        }

        assertEquals(customModelData, customized.get(DataComponents.CUSTOM_MODEL_DATA))
        assertNull(convertCustomItem(plain))
        val converted = assertNotNull(convertCustomItem(customized))
        assertEquals(customModelData, unwrap(converted).get(DataComponents.CUSTOM_MODEL_DATA))
    }

    private fun convertCustomItem(itemStack: ItemStack): TransformedItemStack? {
        val converter = PlayerArmorImpl::class.java.declaredMethods.singleOrNull {
            it.parameterTypes.contentEquals(arrayOf(ItemStack::class.java)) &&
                it.returnType == TransformedItemStack::class.java
        }
        assertNotNull(converter, "PlayerArmorImpl must preserve custom head-item model data")
        converter.isAccessible = true
        return converter.invoke(converterOwner, itemStack) as TransformedItemStack?
    }

    private fun unwrap(itemStack: TransformedItemStack): ItemStack = CraftItemStack.asNMSCopy(
        (itemStack.itemStack as BukkitItemStack).source()
    )

    private companion object {
        private val unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").run {
            isAccessible = true
            get(null) as Unsafe
        }
        private val converterOwner = unsafe.allocateInstance(PlayerArmorImpl::class.java)
    }
}

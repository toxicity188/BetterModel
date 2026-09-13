/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.manager

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.BetterModelPlatform
import kr.toxicity.model.api.armor.ArmorItem
import kr.toxicity.model.api.armor.PlayerArmor
import kr.toxicity.model.api.bone.BoneName
import kr.toxicity.model.api.bone.BoneRenderContext
import kr.toxicity.model.api.bone.BoneTag
import kr.toxicity.model.api.data.Float3
import kr.toxicity.model.api.data.blueprint.BlueprintElement
import kr.toxicity.model.api.data.blueprint.ModelBlueprint
import kr.toxicity.model.api.data.raw.ModelResolution
import kr.toxicity.model.api.data.renderer.ModelRenderer
import kr.toxicity.model.api.data.renderer.RenderSource
import kr.toxicity.model.api.data.renderer.RendererGroup
import kr.toxicity.model.api.entity.BasePlayer
import kr.toxicity.model.api.platform.PlatformAdapter
import kr.toxicity.model.api.platform.PlatformItemStack
import kr.toxicity.model.api.platform.PlatformItemTransform
import kr.toxicity.model.api.platform.PlatformNamespace
import kr.toxicity.model.api.skin.SkinData
import kr.toxicity.model.api.util.TransformedItemStack
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.UUID
import sun.misc.Unsafe
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Player-renderer regression coverage for BetterModel issue #410.
 */
class PlayerModelRendererTest {

    private var previousPlatform: Any? = null

    @BeforeTest
    fun installPlatform() {
        platformField().run {
            previousPlatform = get(null)
            set(null, platform())
        }
    }

    @AfterTest
    fun restorePlatform() {
        platformField().set(null, previousPlatform)
    }

    @Test
    fun `player head receives an internal custom-item child`() {
        val first = playerRenderer()
        val second = playerRenderer()
        val firstHead = assertNotNull(first.groupByTree(headName()))
        val secondHead = assertNotNull(second.groupByTree(headName()))
        val firstHeadItem = assertNotNull(firstHead.customHeadItem())
        val secondHeadItem = assertNotNull(secondHead.customHeadItem())

        assertNotNull(firstHead.children[authoredChildName()])
        assertNotNull(firstHead.children[collidingChildName()])
        assertEquals(firstHead.position, firstHeadItem.position)
        assertTrue(firstHeadItem.parent.visibility())
        assertEquals(firstHeadItem.uuid(), secondHeadItem.uuid())
        assertEquals(firstHeadItem.name(), secondHeadItem.name())
        assertTrue(firstHeadItem.name() != collidingChildName())
        assertTrue(BoneName.of(firstHeadItem.name().rawName()).tags().isEmpty())
        assertNull(BoneTag.REGISTRY.byTagNameOrNull("phi"))
    }

    @Test
    fun `internal custom-item child renders the preserved helmet with head transform`() {
        val customItem = TransformedItemStack.of(TestItem(false))
        val mapper = assertNotNull(
            assertNotNull(playerRenderer().groupByTree(headName())).customHeadItem()
        ).itemMapper

        val mapped = mapper.apply(context(armor(customItem)), TransformedItemStack.empty())

        assertSame(customItem, mapped)
        assertEquals(PlatformItemTransform.HEAD, mapper.transform())
    }

    @Test
    fun `internal custom-item child renders air when helmet is not customized`() {
        val mapper = assertNotNull(
            assertNotNull(playerRenderer().groupByTree(headName())).customHeadItem()
        ).itemMapper

        val mapped = mapper.apply(context(armor(null)), TransformedItemStack.empty())

        assertTrue(mapped.isAir)
    }

    @Test
    fun `general models do not receive the internal player child`() {
        val head = assertNotNull(toRenderer(ModelRenderer.Type.GENERAL).groupByTree(headName()))

        assertNull(head.customHeadItem())
    }

    private fun playerRenderer() = toRenderer(ModelRenderer.Type.PLAYER)

    private fun toRenderer(type: ModelRenderer.Type): ModelRenderer {
        val pipelineClass = ModelManagerImpl::class.java.declaredClasses.single {
            it.simpleName == "ModelPipeline"
        }
        val converter = pipelineClass.declaredMethods.single {
            it.name == "toRenderer" &&
                it.parameterTypes.firstOrNull() == ModelBlueprint::class.java
        }
        converter.isAccessible = true
        val itemBuilder: (BlueprintElement.Group) -> String? = { null }
        val pipeline = UNSAFE.allocateInstance(pipelineClass)
        return converter.invoke(pipeline, blueprint(), type, itemBuilder) as ModelRenderer
    }

    private fun blueprint(): ModelBlueprint {
        val authoredChild = BlueprintElement.Locator(
            AUTHORED_CHILD_UUID,
            authoredChildName(),
            Float3(4F, 24F, -2F)
        )
        val collidingChild = BlueprintElement.Locator(
            COLLIDING_CHILD_UUID,
            collidingChildName(),
            Float3(4F, 24F, -2F)
        )
        val head = BlueprintElement.Group(
            HEAD_UUID,
            headName(),
            Float3(4F, 24F, -2F),
            Float3.ZERO,
            listOf(authoredChild, collidingChild),
            true
        )
        return ModelBlueprint(
            "existing_player_rig",
            ModelResolution(64, 64),
            emptyList(),
            listOf(head),
            emptyMap()
        )
    }

    private fun RendererGroup.customHeadItem(): RendererGroup? = children.values.singleOrNull {
        it.itemMapper.transform() == PlatformItemTransform.HEAD
    }

    private fun headName() = BoneName.of("ph_head")

    private fun authoredChildName() = BoneName.of("authored_child")

    private fun collidingChildName(): BoneName {
        val rawName = "bettermodel:internal_player_head_item:$HEAD_UUID"
        return BoneName(emptySet(), rawName, rawName)
    }

    private fun context(armor: PlayerArmor): BoneRenderContext {
        val player = proxy<BasePlayer> { method, _ ->
            if (method.name == "armors") armor else defaultValue(method.returnType)
        }
        val skin = proxy<SkinData> { method, _ -> defaultValue(method.returnType) }
        return BoneRenderContext(RenderSource.of(player), skin)
    }

    private fun armor(helmetItem: TransformedItemStack?) = object : PlayerArmor {
        override fun helmet(): ArmorItem? = null
        override fun helmetItem(): TransformedItemStack? = helmetItem
        override fun chestplate(): ArmorItem? = null
        override fun leggings(): ArmorItem? = null
        override fun boots(): ArmorItem? = null
    }

    private fun platform(): BetterModelPlatform {
        val adapter = proxy<PlatformAdapter> { method, _ ->
            if (method.name == "air") AIR else defaultValue(method.returnType)
        }
        return proxy { method, _ ->
            if (method.name == "adapter") adapter else defaultValue(method.returnType)
        }
    }

    private fun platformField() = BetterModel::class.java.getDeclaredField("instance").apply {
        isAccessible = true
    }

    private inline fun <reified T> proxy(
        noinline invocation: (Method, Array<out Any?>?) -> Any?
    ): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java)
    ) { _, method, arguments -> invocation(method, arguments) } as T

    private fun defaultValue(type: Class<*>): Any? = when {
        !type.isPrimitive -> null
        type == Boolean::class.javaPrimitiveType -> false
        type == Char::class.javaPrimitiveType -> '\u0000'
        type == Byte::class.javaPrimitiveType -> 0.toByte()
        type == Short::class.javaPrimitiveType -> 0.toShort()
        type == Int::class.javaPrimitiveType -> 0
        type == Long::class.javaPrimitiveType -> 0L
        type == Float::class.javaPrimitiveType -> 0F
        type == Double::class.javaPrimitiveType -> 0.0
        else -> error("Unknown primitive type: $type")
    }

    private data class TestItem(private val air: Boolean) : PlatformItemStack {
        override fun isAir(): Boolean = air
        override fun enchant(enchant: Boolean): PlatformItemStack = this
        override fun itemModel(namespace: PlatformNamespace?): PlatformItemStack = this
        public override fun clone(): PlatformItemStack = this
    }

    private companion object {
        private val AIR = TestItem(true)
        private val UNSAFE = Unsafe::class.java.getDeclaredField("theUnsafe").run {
            isAccessible = true
            get(null) as Unsafe
        }
        private val HEAD_UUID = UUID.fromString("a3172126-8b18-4bee-bfae-410000000001")
        private val AUTHORED_CHILD_UUID = UUID.fromString("a3172126-8b18-4bee-bfae-410000000002")
        private val COLLIDING_CHILD_UUID = UUID.fromString("a3172126-8b18-4bee-bfae-410000000003")
    }
}

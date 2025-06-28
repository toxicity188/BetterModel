package kr.toxicity.model.nms.v1_21_R4

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup
import com.mojang.authlib.GameProfile
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.*
import kr.toxicity.model.api.player.PlayerLimb
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.tracker.ModelRotation
import kr.toxicity.model.api.util.TransformedItemStack
import kr.toxicity.model.api.util.function.BonePredicate
import net.minecraft.core.component.DataComponents
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.util.ARGB
import net.minecraft.util.Brightness
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.level.entity.LevelEntityGetterAdapter
import net.minecraft.world.level.entity.PersistentEntitySectionManager
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f
import java.lang.reflect.Field
import java.util.function.BooleanSupplier

class NMSImpl : NMS {

    companion object {
        private const val INJECT_NAME = "bettermodel_channel_handler"

        //Spigot
        private val getGameProfile: (net.minecraft.world.entity.player.Player) -> GameProfile = createAdaptedFieldGetter { it.gameProfile }
        private val getConnection: (ServerCommonPacketListenerImpl) -> Connection = createAdaptedFieldGetter { it.connection }
        private val spigotChunkAccess = ServerLevel::class.java.fields.firstOrNull {
            it.type == PersistentEntitySectionManager::class.java
        }?.apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        private val ServerLevel.levelGetter
            get(): LevelEntityGetter<Entity> {
                return if (BetterModel.IS_PAPER) {
                    `moonrise$getEntityLookup`()
                } else {
                    spigotChunkAccess?.get(this)?.let {
                        (it as PersistentEntitySectionManager<*>).entityGetter as LevelEntityGetter<Entity>
                    } ?: throw RuntimeException("LevelEntityGetter")
                }
            }
        private val getEntityById: (LevelEntityGetter<Entity>, Int) -> Entity? = if (BetterModel.IS_PAPER)  { g, i ->
            (g as EntityLookup)[i]
        } else LevelEntityGetterAdapter::class.java.declaredFields.first {
            net.minecraft.world.level.entity.EntityLookup::class.java.isAssignableFrom(it.type)
        }.let {
            it.isAccessible = true
            { e, i ->
                (it[e] as net.minecraft.world.level.entity.EntityLookup<*>).getEntity(i) as? Entity
            }
        }
        private fun Int.toEntity(level: ServerLevel) = getEntityById(level.levelGetter, this)
        //Spigot

        private fun Class<*>.serializers() = declaredFields.filter { f ->
            EntityDataAccessor::class.java.isAssignableFrom(f.type)
        }

        private fun Field.toSerializerId() = run {
            isAccessible = true
            (get(null) as EntityDataAccessor<*>).id
        }

        private val sharedFlag = Entity::class.java.serializers().first().toSerializerId()
        private val itemId = ItemDisplay::class.java.serializers().map {
            it.toSerializerId()
        }
        private val itemSerializer = itemId.first()
        private val displaySet = Display::class.java.serializers().map { e ->
            e.toSerializerId()
        }
        private val transformSet = displaySet.subList(0, 6).toIntSet()
        private val entityDataSet = (mutableListOf(sharedFlag) + itemId + displaySet.subList(transformSet.size, displaySet.size)).toIntSet()
    }

    override fun hide(player: Player, registry: EntityTrackerRegistry, condition: BooleanSupplier) {
        val entity = registry.entity()
        val target = (entity as CraftEntity).vanillaEntity
        val task = {
            val list = mutableListOf<Packet<ClientGamePacketListener>>(
                ClientboundSetEntityDataPacket(target.id, target.entityData.pack()).toRegistryDataPacket(registry)
            )
            if (target is LivingEntity) target.toEmptyEquipmentPacket()?.let {
                list += it
            }
            PacketBundlerImpl(list).send(player)
        }
        if (entity is Player) BetterModel.plugin().scheduler().asyncTaskLater(CONFIG.playerHideDelay()) {
            if (condition.asBoolean) task()
        } else task()
    }
    
    private fun ClientboundSetEntityDataPacket.toRegistryDataPacket(registry: EntityTrackerRegistry) = ClientboundSetEntityDataPacket(id, packedItems().map {
        if (it.id == sharedFlag) SynchedEntityData.DataValue(
            it.id,
            EntityDataSerializers.BYTE,
            registry.entityFlag(it.value() as Byte)
        ) else it
    })

    inner class PlayerChannelHandlerImpl(
        private val player: Player
    ) : PlayerChannelHandler, ChannelDuplexHandler() {
        private val connection = (player as CraftPlayer).handle.connection
        private val slim = BetterModel.plugin().skinManager().isSlim(profile())

        init {
            val pipeLine = getConnection(connection).channel.pipeline()
            pipeLine.toMap().forEach {
                if (it.value is Connection) pipeLine.addBefore(it.key, INJECT_NAME, this)
            }
        }

        override fun isSlim(): Boolean = slim

        override fun close() {
            val channel = getConnection(connection).channel
            channel.eventLoop().submit {
                channel.pipeline().remove(INJECT_NAME)
            }
        }

        override fun player(): Player = player
        private fun send(packet: Packet<*>) = connection.send(packet)

        private fun Int.toPlayerEntity() = toEntity(connection.player.serverLevel())
        private fun Int.toRegistry(filter: EntityTrackerRegistry.() -> Boolean = { isSpawned(player ) }) = (EntityTrackerRegistry.registry(this) ?: toPlayerEntity()?.let {
            EntityTrackerRegistry.registry(it.uuid)
        })?.takeIf(filter)

        override fun sendEntityData(registry: EntityTrackerRegistry) {
            val handle = registry.adapter().handle() as Entity
            val list = mutableListOf(
                ClientboundSetEntityDataPacket(handle.id, handle.entityData.pack()),
                ClientboundSetPassengersPacket(handle)
            )
            if (handle is LivingEntity) handle.toEquipmentPacket()?.let {
                list += it
            }
            PacketBundlerImpl(list).send(player)
        }

        private fun <T : ClientGamePacketListener> Packet<in T>.handle(): Packet<in T> {
            when (this) {
                is ClientboundBundlePacket -> return if (subPackets() is PacketBundler) this else ClientboundBundlePacket(subPackets().map {
                    it.handle()
                })
                is ClientboundAddEntityPacket -> {
                    EntityTrackerRegistry.registry(id)?.spawnIfMatched(player) ?: id.toPlayerEntity()?.bukkitEntity?.let { e ->
                        if (!EntityTrackerRegistry.hasModelData(e)) return this
                        BetterModel.plugin().scheduler().taskLater(1, e) {
                            EntityTrackerRegistry.registry(e).spawnIfMatched(player)
                        }
                    }
                }
                is ClientboundRemoveEntitiesPacket -> {
                    entityIds
                        .asSequence()
                        .mapNotNull {
                            it.toRegistry()
                        }
                        .forEach {
                            it.remove()
                        }
                }
                is ClientboundSetPassengersPacket -> {
                    vehicle.toRegistry()?.let {
                        return it.mountPacket(array = useByteBuf { buffer ->
                            ClientboundSetPassengersPacket.STREAM_CODEC.encode(buffer, this)
                            buffer.readVarInt()
                            buffer.readVarIntArray()
                        })
                    }
                }
                is ClientboundSetEntityDataPacket -> id.toRegistry()?.let { registry ->
                    return toRegistryDataPacket(registry)
                }
                is ClientboundSetEquipmentPacket -> entity.toRegistry()?.let {
                    if (it.hideOption().equipment()) (it.adapter().handle() as? LivingEntity)?.toEmptyEquipmentPacket()?.let { packet ->
                        return packet
                    }
                } 
                is ClientboundRespawnPacket -> EntityTrackerRegistry.registry(player.uniqueId)?.let {
                    send(it.mountPacket())
                }
            }
            return this
        }

        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            super.write(ctx, if (msg is Packet<*>) msg.handle() else msg, promise)
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            fun EntityTrackerRegistry.updatePlayerLimb() {
                player.updateInventory()
                connection.player.toEmptyEquipmentPacket()?.let {
                    connection.send(it)
                }
                BetterModel.plugin().scheduler().asyncTaskLater(1) {
                    trackers().forEach { tracker ->
                        if (tracker.updateItem(BonePredicate.of(BonePredicate.State.NOT_SET) {
                                it.itemMapper is PlayerLimb.LimbItemMapper
                            })) tracker.forceUpdate(true)
                    }
                }
            }
            when (msg) {
                is ServerboundSetCarriedItemPacket -> {
                    connection.player.id.toRegistry()?.let { registry ->
                        if (!registry.hideOption().equipment()) return super.channelRead(ctx, msg)
                        if (CONFIG.cancelPlayerModelInventory()) {
                            connection.send(ClientboundSetHeldSlotPacket(player.inventory.heldItemSlot))
                            return
                        } else {
                            registry.updatePlayerLimb()
                        }
                    }
                }
                is ServerboundPlayerActionPacket -> {
                    connection.player.id.toRegistry()?.let { registry ->
                        if (!registry.hideOption().equipment()) return super.channelRead(ctx, msg)
                        if (CONFIG.cancelPlayerModelInventory()) return
                        else registry.updatePlayerLimb()
                    }
                }
            }
            super.channelRead(ctx, msg)
        }

        private fun EntityTrackerRegistry.remove() {
            remove(player)
        }
    }

    override fun mount(registry: EntityTrackerRegistry, bundler: PacketBundler) {
        bundler.unwrap() += registry.mountPacket()
    }

    private fun EntityTrackerRegistry.mountPacket(entity: Entity = adapter().handle() as Entity, array: IntArray = entity.passengers.filter {
        EntityTrackerRegistry.registry(it.uuid) == null
    }.map {
        it.id
    }.toIntArray()): ClientboundSetPassengersPacket {
        return useByteBuf { buffer ->
            buffer.writeVarInt(entity.id)
            buffer.writeVarIntArray(displays()
                .mapToInt { 
                    (it as ModelDisplayImpl).display.id 
                }.toArray() + array)
            ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer)
        }
    }

    override fun inject(player: Player): PlayerChannelHandlerImpl = PlayerChannelHandlerImpl(player)

    override fun createBundler(initialCapacity: Int): PacketBundler = PacketBundlerImpl(ArrayList(initialCapacity))

    override fun create(location: Location): ModelDisplay = ModelDisplayImpl(ItemDisplay(EntityType.ITEM_DISPLAY, (location.world as CraftWorld).handle).apply {
        billboardConstraints = Display.BillboardConstraints.FIXED
        valid = true
        moveTo(
            location.x,
            location.y,
            location.z,
            location.yaw,
            0F
        )
        itemTransform = ItemDisplayContext.FIXED
        entityData[Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID] = 3
    })

    private inner class ModelDisplayImpl(
        val display: ItemDisplay
    ) : ModelDisplay {

        private var forceGlow = false
        private var forceInvisibility = false

        override fun rotate(rotation: ModelRotation, bundler: PacketBundler) {
            if (!display.valid) return
            display.xRot = rotation.x
            display.yRot = rotation.y
            bundler.unwrap() += ClientboundMoveEntityPacket.Rot(
                display.id,
                rotation.packedY(),
                rotation.packedX(),
                display.onGround
            )
        }

        override fun invisible(invisible: Boolean) {
            forceInvisibility = invisible
        }

        override fun sync(entity: EntityAdapter) {
            display.valid = !entity.dead()
            display.onGround = entity.ground()
            display.setGlowingTag(entity.glow() || forceGlow)
            if (CONFIG.followMobInvisibility()) display.isInvisible = entity.invisible()
        }

        override fun display(transform: org.bukkit.entity.ItemDisplay.ItemDisplayTransform) {
            display.itemTransform = ItemDisplayContext.BY_ID.apply(transform.ordinal)
        }

        override fun spawn(showItem: Boolean, bundler: PacketBundler) {
            bundler.unwrap() += display.addPacket
            val f = display.transformationInterpolationDuration
            frame(0)
            bundler.unwrap() += ClientboundSetEntityDataPacket(display.id, display.entityData.nonDefaultValues!!.map {
                if (it.id == itemSerializer) SynchedEntityData.DataValue(
                    it.id,
                    EntityDataSerializers.ITEM_STACK,
                    if (showItem) display.itemStack else net.minecraft.world.item.ItemStack.EMPTY
                ) else it
            })
            frame(f)
        }

        override fun frame(frame: Int) {
            display.transformationInterpolationDuration = frame
        }

        override fun moveDuration(duration: Int) {
            display.entityData[Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID] = duration
        }

        override fun remove(bundler: PacketBundler) {
            bundler.unwrap() += removePacket
        }

        override fun teleport(location: Location, bundler: PacketBundler) {
            display.moveTo(
                location.x,
                location.y,
                location.z,
                location.yaw,
                0F
            )
            bundler.unwrap() += ClientboundTeleportEntityPacket.teleport(display.id, PositionMoveRotation.of(display), emptySet(), display.onGround)
        }

        override fun item(itemStack: ItemStack) {
            display.itemStack = CraftItemStack.asNMSCopy(itemStack)
        }

        override fun brightness(block: Int, sky: Int) {
            display.brightnessOverride = if (block < 0 && sky < 0) null else Brightness(
                block,
                sky
            )
        }

        override fun viewRange(range: Float) {
            display.viewRange = range
        }

        override fun shadowRadius(radius: Float) {
            display.shadowRadius = radius
        }

        override fun glow(glow: Boolean) {
            forceGlow = glow
            display.setGlowingTag(display.isCurrentlyGlowing || forceGlow)
        }

        override fun glowColor(glowColor: Int) {
            display.glowColorOverride = glowColor
        }

        override fun syncPosition(adapter: EntityAdapter, bundler: PacketBundler) {
            val handle = adapter.handle() as Entity
            display.setPos(handle.position())
            display.onGround = handle.onGround
            bundler.unwrap() += ClientboundEntityPositionSyncPacket(
                display.id,
                PositionMoveRotation.of(handle),
                handle.onGround
            )
        }

        override fun transform(position: Vector3f, scale: Vector3f, rotation: Quaternionf) {
            display.setTransformation(com.mojang.math.Transformation(
                position,
                rotation,
                scale,
                EMPTY_QUATERNION
            ))
        }

        override fun sendTransformation(bundler: PacketBundler) {
            bundler.unwrap() += ClientboundSetEntityDataPacket(display.id, display.entityData.pack().filter {
                transformSet.contains(it.id)
            })
        }

        override fun invisible(): Boolean = display.isInvisible || forceInvisibility || display.itemStack.`is`(Items.AIR)
        override fun sendEntityData(showItem: Boolean, bundler: PacketBundler) {
            bundler.unwrap() += ClientboundSetEntityDataPacket(display.id, display.entityData.pack()
                .asSequence()
                .filter {
                    entityDataSet.contains(it.id)
                }
                .map {
                    if (it.id == itemSerializer) SynchedEntityData.DataValue(
                        it.id,
                        EntityDataSerializers.ITEM_STACK,
                        if (showItem) display.itemStack else net.minecraft.world.item.ItemStack.EMPTY
                    ) else it
                }.toList())
        }

        private val removePacket
            get() = ClientboundRemoveEntitiesPacket(display.id)

    }

    private val Entity.addPacket
        get() = ClientboundAddEntityPacket(
            id,
            uuid,
            x,
            y,
            z,
            xRot,
            yRot,
            type,
            0,
            deltaMovement,
            yHeadRot.toDouble()
        )

    override fun tint(itemStack: ItemStack, rgb: Int): ItemStack {
        return CraftItemStack.asBukkitCopy(CraftItemStack.asNMSCopy(itemStack).apply {
            //set(DataComponents.DYED_COLOR, DyedItemColor(rgb))
            set(DataComponents.CUSTOM_MODEL_DATA, get(DataComponents.CUSTOM_MODEL_DATA)?.let {
                CustomModelData(it.floats, it.flags, it.strings, it.colors
                    .run {
                        if (rgb == 0xFFFFFF) this else map { color ->
                            ARGB.multiply(color, rgb) and 0xFFFFFF
                        }
                    }
                    .ifEmpty { listOf(rgb) })
            })
        })
    }

    override fun createHitBox(entity: EntityAdapter, supplier: HitBoxSource, namedBoundingBox: NamedBoundingBox, mountController: MountController, listener: HitBoxListener): HitBox? {
        val handle = entity.handle() as? Entity ?: return null
        val newBox = namedBoundingBox.center()
        return HitBoxImpl(
            namedBoundingBox.name,
            newBox,
            supplier,
            listener,
            handle,
            mountController
        ).craftEntity
    }
    override fun version(): NMSVersion = NMSVersion.V1_21_R4

    override fun adapt(entity: org.bukkit.entity.Entity): EntityAdapter {
        entity as CraftEntity
        return object : EntityAdapter {
            
            override fun entity(): org.bukkit.entity.Entity = entity
            override fun handle(): Entity = entity.vanillaEntity
            override fun dead(): Boolean = (handle() as? LivingEntity)?.isDeadOrDying == true || handle().removalReason?.shouldSave() == false
            override fun invisible(): Boolean = handle().isInvisible || (handle() as? LivingEntity)?.hasEffect(MobEffects.INVISIBILITY) == true
            override fun glow(): Boolean = handle().isCurrentlyGlowing

            override fun onWalk(): Boolean {
                return handle().isWalking()
            }

            override fun scale(): Double {
                val handle = handle()
                return if (handle is LivingEntity) handle.scale.toDouble() else 1.0
            }

            override fun pitch(): Float {
                return handle().xRot
            }

            override fun ground(): Boolean {
                return handle().onGround()
            }

            override fun bodyYaw(): Float {
                val handle = handle()
                return if (handle is ServerPlayer) handle.yRot else handle.visualRotationYInDegrees
            }

            override fun yaw(): Float {
                return handle().yHeadRot
            }

            override fun fly(): Boolean {
                return handle().isFlying
            }

            override fun damageTick(): Float {
                val handle = handle()
                if (handle !is LivingEntity) return 0F
                val duration = handle.invulnerableDuration.toFloat()
                if (duration <= 0F) return 0F
                val knockBack = 1 - (handle.getAttribute(Attributes.KNOCKBACK_RESISTANCE)?.value?.toFloat() ?: 0F)
                return handle.invulnerableTime.toFloat() / duration * knockBack * (1F - 1F / (handle.deltaMovement.length().toFloat() * 20 + 1F))
            }

            override fun walkSpeed(): Float {
                val handle = handle()
                if (handle !is LivingEntity) return 0F
                if (!handle.onGround) return 1F
                val speed = handle.getEffect(MobEffects.SPEED)?.amplifier ?: 0
                val slow = handle.getEffect(MobEffects.SLOWNESS)?.amplifier ?: 0
                return (1F + (speed - slow) * 0.2F)
                    .coerceAtLeast(0.2F)
                    .coerceAtMost(2F)
            }

            override fun passengerPosition(): Vector3f {
                return handle().passengerPosition()
            }
        }
    }

    override fun profile(player: Player): GameProfile = getGameProfile((player as CraftPlayer).handle)

    override fun createPlayerHead(profile: GameProfile): ItemStack = net.minecraft.world.item.ItemStack(Items.PLAYER_HEAD).run {
        set(DataComponents.PROFILE, ResolvableProfile(profile))
        CraftItemStack.asBukkitCopy(this)
    }

    override fun createSkinItem(model: String, flags: List<Boolean>, colors: List<Int>): TransformedItemStack {
        return net.minecraft.world.item.ItemStack(Items.PLAYER_HEAD).run {
            set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(emptyList(), flags, emptyList(), colors))
            set(DataComponents.ITEM_MODEL, ResourceLocation.parse(model))
            TransformedItemStack.of(CraftItemStack.asBukkitCopy(this))
        }
    }

    override fun isProxyOnlineMode(): Boolean = ONLINE_MODE
}
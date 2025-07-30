package kr.toxicity.model.nms.v1_21_R2

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup
import com.mojang.authlib.GameProfile
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bone.RenderedBone
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.*
import kr.toxicity.model.api.tracker.EntityTrackerRegistry
import kr.toxicity.model.api.tracker.ModelRotation
import kr.toxicity.model.api.util.lock.SingleLock
import net.kyori.adventure.key.Keyed
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.util.Brightness
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.level.entity.LevelEntityGetterAdapter
import net.minecraft.world.level.entity.PersistentEntitySectionManager
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.joml.Quaternionf
import org.joml.Vector3f
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

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
        }.map { 
            it.toSerializerId()
        }
        private fun Field.toSerializerId() = run {
            isAccessible = true
            get(null) as EntityDataAccessor<*>
        }

        private val sharedFlag = Entity::class.java.serializers().first().id
        private val itemId = ItemDisplay::class.java.serializers().map {
            it.id
        }
        private val itemSerializer = ItemDisplay::class.java.serializers().first()
        private val displaySet = Display::class.java.serializers().map { e ->
            e.id
        }
        private val interpolationDelay = displaySet.first()
        private val animationSet = displaySet.subList(3, 6).toIntSet()
        private val transformSet = displaySet.subList(0, 6).toIntSet()
        private val entityDataSet = (mutableListOf(sharedFlag) + itemId + displaySet.subList(transformSet.size, displaySet.size)).toIntSet()
        private val hitBoxData = ItemDisplay(EntityType.ITEM_DISPLAY, MinecraftServer.getServer().overworld()).run {
            entityData[Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID] = 3
            entityData.nonDefaultValues!!
        }
    }

    override fun hide(channel: PlayerChannelHandler, registry: EntityTrackerRegistry) {
        val target = (registry.entity() as CraftEntity).vanillaEntity
        val list = bundlerOf()
        target.entityData.pack(
            valueFilter = { it.id == sharedFlag }
        )?.let {
            list += ClientboundSetEntityDataPacket(target.id, it).toRegistryDataPacket(channel.uuid(), registry)
        }
        if (target is LivingEntity) {
            val packet = if (registry.hideOption(channel.uuid()).equipment) target.toEmptyEquipmentPacket() else target.toEquipmentPacket()
            packet?.let { list += it }
        }
        list.send(channel.player())
    }
    
    private fun ClientboundSetEntityDataPacket.toRegistryDataPacket(uuid: UUID, registry: EntityTrackerRegistry) = ClientboundSetEntityDataPacket(id, packedItems().map {
        if (it.id == sharedFlag) SynchedEntityData.DataValue(
            it.id,
            EntityDataSerializers.BYTE,
            registry.entityFlag(uuid, it.value() as Byte)
        ) else it
    })

    inner class PlayerChannelHandlerImpl(
        private val player: Player
    ) : PlayerChannelHandler, ChannelDuplexHandler() {
        private val connection = (player as CraftPlayer).handle.connection
        private val uuid = player.uniqueId
        private val slim = BetterModel.plugin().skinManager().isSlim(profile())

        init {
            val pipeLine = getConnection(connection).channel.pipeline()
            pipeLine.toMap().forEach {
                if (it.value is Connection) pipeLine.addBefore(it.key, INJECT_NAME, this)
            }
        }

        override fun isSlim(): Boolean = slim
        override fun uuid(): UUID = uuid
        override fun close() {
            val channel = getConnection(connection).channel
            channel.eventLoop().submit {
                channel.pipeline().remove(INJECT_NAME)
            }
        }

        override fun player(): Player = player
        private val playerModel get() = connection.player.id.toRegistry()

        private fun Int.toPlayerEntity() = toEntity(connection.player.serverLevel())
        private fun Entity.toRegistry() = EntityTrackerRegistry.registry(uuid)
        private inline fun Int.toRegistry(
            ifHitBox: (Entity) -> Unit = {}
        ) = (EntityTrackerRegistry.registry(this) ?: toPlayerEntity()?.let {
            if (it is HitBox) ifHitBox(it)
            it.toRegistry()
        })?.takeIf {
            it.isSpawned(player)
        }

        override fun sendEntityData(registry: EntityTrackerRegistry) {
            val handle = registry.adapter().handle() as Entity
            val list = bundlerOf(
                ClientboundSetPassengersPacket(handle)
            )
            handle.entityData.pack(
                valueFilter = { it.id == sharedFlag }
            )?.let {
                list += ClientboundSetEntityDataPacket(handle.id, it)
            }
            if (handle is LivingEntity) handle.toEquipmentPacket()?.let {
                list += it
            }
            list.send(player)
        }

        private fun <T : ClientGamePacketListener> Packet<in T>.handle(): Packet<in T>? {
            when (this) {
                is ClientboundBundlePacket -> return if (subPackets() is Keyed) this else ClientboundBundlePacket(subPackets().mapNotNull {
                    it.handle()
                })
                is ClientboundAddEntityPacket -> {
                    val entity = id.toPlayerEntity() ?: return this
                    if (entity is HitBox) return entity.toFakeAddPacket()
                    BetterModel.registry(entity.bukkitEntity).ifPresent {
                        BetterModel.plugin().scheduler().taskLater(entity.bukkitEntity, 1) {
                            it.spawn(player)
                        }
                    }
                }
                is ClientboundRemoveEntitiesPacket -> {
                    entityIds
                        .asSequence()
                        .mapNotNull map@ {
                            it.toRegistry {
                                return@map null
                            }
                        }
                        .forEach {
                            it.remove()
                        }
                }
                is ClientboundSetPassengersPacket -> {
                    vehicle.toRegistry()?.let {
                        return it.mountPacket(array = passengers)
                    }
                }
                is ClientboundUpdateAttributesPacket if entityId.toPlayerEntity() is HitBox -> return null
                is ClientboundSetEntityDataPacket -> id.toRegistry {
                    return ClientboundSetEntityDataPacket(id, hitBoxData)
                }?.let { registry ->
                    return toRegistryDataPacket(uuid, registry)
                }
                is ClientboundSetEquipmentPacket -> entity.toRegistry {
                    return null
                }?.let {
                    if (it.hideOption(uuid).equipment()) (it.adapter().handle() as? LivingEntity)?.toEmptyEquipmentPacket()?.let { packet ->
                        return packet
                    }
                } 
                is ClientboundRespawnPacket -> playerModel?.let {
                    bundlerOf(it.mountPacket()).send(player)
                }
                is ClientboundContainerSetSlotPacket if isInHand(connection.player) && playerModel?.hideOption(uuid)?.equipment() == true -> {
                    return ClientboundContainerSetSlotPacket(containerId, stateId, slot, EMPTY_ITEM)
                }
                is ClientboundContainerSetContentPacket if containerId == 0 && playerModel?.hideOption(uuid)?.equipment() == true -> {
                    return ClientboundContainerSetContentPacket(
                        containerId,
                        stateId,
                        (items as NonNullList<net.minecraft.world.item.ItemStack>).apply {
                            set(45, EMPTY_ITEM)
                            set(connection.player.hotbarSlot, EMPTY_ITEM)
                        },
                        carriedItem
                    )
                }
            }
            return this
        }

        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            super.write(ctx, if (msg is Packet<*>) msg.handle() ?: return else msg, promise)
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            fun EntityTrackerRegistry.updatePlayerLimb() = BetterModel.plugin().scheduler().asyncTaskLater(1) {
                if (isClosed) return@asyncTaskLater
                player.updateInventory()
                trackers().forEach { tracker ->
                    tracker.updateDisplay { bone ->
                        !bone.itemMapper.fixed()
                    }
                }
            }
            when (msg) {
                is ServerboundSetCarriedItemPacket -> {
                    playerModel?.let { registry ->
                        if (!registry.hideOption(uuid).equipment()) return super.channelRead(ctx, msg)
                        if (CONFIG.cancelPlayerModelInventory()) {
                            connection.send(ClientboundSetHeldSlotPacket(player.inventory.heldItemSlot))
                            return
                        }
                        registry.updatePlayerLimb()
                    }
                }
                is ServerboundPlayerActionPacket -> {
                    playerModel?.let { registry ->
                        if (!registry.hideOption(uuid).equipment()) return super.channelRead(ctx, msg)
                        if (CONFIG.cancelPlayerModelInventory()) return
                        registry.updatePlayerLimb()
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
        bundler += registry.mountPacket()
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

    override fun createBundler(initialCapacity: Int): PacketBundler = bundlerOf(initialCapacity)
    override fun createParallelBundler(threshold: Int): PacketBundler = parallelBundlerOf(threshold)

    override fun create(location: Location, yOffset: Double, initialConsumer: Consumer<ModelDisplay>): ModelDisplay = ModelDisplayImpl(ItemDisplay(EntityType.ITEM_DISPLAY, (location.world as CraftWorld).handle).apply {
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
    }, yOffset).apply {
        initialConsumer.accept(this)
        display.entityData.packDirty()
    }

    private inner class ModelDisplayImpl(
        val display: ItemDisplay,
        val yOffset: Double
    ) : ModelDisplay {

        private val entityData = display.entityData
        private val entityDataLock = SingleLock()
        private val forceGlow = AtomicBoolean()
        private val forceInvisibility = AtomicBoolean()

        override fun rotate(rotation: ModelRotation, bundler: PacketBundler) {
            if (!display.valid) return
            display.xRot = rotation.x
            display.yRot = rotation.y
            bundler += ClientboundMoveEntityPacket.Rot(
                display.id,
                rotation.packedY(),
                rotation.packedX(),
                display.onGround
            )
        }

        override fun invisible(invisible: Boolean) {
            if (forceInvisibility.compareAndSet(!invisible, invisible)) {
                entityDataLock.accessToLock {
                    entityData.markDirty(itemSerializer)
                }
            }
        }

        override fun sync(entity: EntityAdapter) {
            display.valid = !entity.dead()
            display.onGround = entity.ground()
            display.setOldPosAndRot()
            display.setPos((entity.handle() as Entity).position())
            val beforeInvisible = display.isInvisible
            val afterInvisible = entity.invisible()
            entityDataLock.accessToLock {
                display.setGlowingTag(entity.glow() || forceGlow.get())
                if (CONFIG.followMobInvisibility() && beforeInvisible != afterInvisible) {
                    display.isInvisible = afterInvisible
                    entityData.markDirty(itemSerializer)
                }
            }
        }

        override fun spawn(showItem: Boolean, bundler: PacketBundler) {
            bundler += addPacket
            bundler += entityDataLock.accessToLock {
                ClientboundSetEntityDataPacket(display.id, entityData.nonDefaultValues!!.markVisible(showItem))
            }
        }
        
        override fun remove(bundler: PacketBundler) {
            bundler += removePacket
        }
        
        override fun teleport(location: Location, bundler: PacketBundler) {
            display.moveTo(
                location.x,
                location.y,
                location.z,
                location.yaw,
                0F
            )
            bundler += ClientboundTeleportEntityPacket.teleport(display.id, PositionMoveRotation.of(display), emptySet(), display.onGround)
        }

        override fun syncPosition(adapter: EntityAdapter, bundler: PacketBundler) {
            val handle = adapter.handle() as Entity
            if (display.position() == display.oldPosition()) return
            bundler += ClientboundEntityPositionSyncPacket(
                display.id,
                PositionMoveRotation.of(handle),
                handle.onGround
            )
        }
        
        override fun display(transform: org.bukkit.entity.ItemDisplay.ItemDisplayTransform) {
            entityDataLock.accessToLock {
                display.itemTransform = ItemDisplayContext.BY_ID.apply(transform.ordinal)
            }
        }

        override fun frame(frame: Int) {
            entityDataLock.accessToLock {
                display.transformationInterpolationDuration = frame
            }
        }

        override fun moveDuration(duration: Int) {
            entityDataLock.accessToLock {
                entityData[Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID] = duration
            }
        }

        override fun item(itemStack: ItemStack) {
            entityDataLock.accessToLock {
                display.itemStack = itemStack.asVanilla()
            }
        }

        override fun brightness(block: Int, sky: Int) {
            entityDataLock.accessToLock {
                display.brightnessOverride = if (block < 0 && sky < 0) null else Brightness(
                    block,
                    sky
                )
            }
        }

        override fun viewRange(range: Float) {
            entityDataLock.accessToLock {
                display.viewRange = range
            }
        }

        override fun shadowRadius(radius: Float) {
            entityDataLock.accessToLock {
                display.shadowRadius = radius
            }
        }

        override fun glow(glow: Boolean) {
            if (!forceGlow.compareAndSet(!glow, glow)) return
            entityDataLock.accessToLock {
                display.setGlowingTag(display.isCurrentlyGlowing || glow)
            }
        }

        override fun glowColor(glowColor: Int) {
            entityDataLock.accessToLock {
                display.glowColorOverride = glowColor
            }
        }

        override fun billboard(billboard: org.bukkit.entity.Display.Billboard) {
            entityDataLock.accessToLock {
                display.billboardConstraints = Display.BillboardConstraints.BY_ID.apply(billboard.ordinal)
            }
        }

        override fun transform(position: Vector3f, scale: Vector3f, rotation: Quaternionf) {
            entityDataLock.accessToLock {
                display.setTransformation(
                    com.mojang.math.Transformation(
                        position,
                        rotation,
                        scale,
                        EMPTY_QUATERNION
                    )
                )
            }
        }

        override fun sendTransformation(bundler: PacketBundler) {
            entityDataLock.accessToLock {
                entityData.pack(
                    clean = true,
                    itemFilter = { interpolationDelay == it.accessor.id || it.isDirty },
                    valueFilter = { transformSet.contains(it.id) },
                    required = { it.any { packed -> animationSet.contains(packed.second.id) } }
                )
            }?.run {
                bundler += ClientboundSetEntityDataPacket(display.id, this)
            }
        }

        override fun invisible(): Boolean = entityDataLock.accessToLock {
            display.isInvisible || forceInvisibility.get() || display.itemStack.`is`(Items.AIR)
        }

        override fun sendEntityData(bundler: PacketBundler) {
            entityDataLock.accessToLock {
                entityData.pack(
                    clean = true,
                    itemFilter = { it.isDirty },
                    valueFilter = { entityDataSet.contains(it.id) }
                )
            }?.markVisible(!invisible())?.run {
                bundler += ClientboundSetEntityDataPacket(display.id, this)
            }
        }

        override fun sendEntityData(showItem: Boolean, bundler: PacketBundler) {
            entityDataLock.accessToLock {
                entityData.pack(
                    valueFilter = { entityDataSet.contains(it.id) }
                )
            }?.markVisible(showItem && !invisible())?.run {
                bundler += ClientboundSetEntityDataPacket(display.id, this)
            }
        }
        
        private fun List<SynchedEntityData.DataValue<*>>.markVisible(showItem: Boolean) = map {
            if (it.id == itemSerializer.id) SynchedEntityData.DataValue(
                it.id,
                EntityDataSerializers.ITEM_STACK,
                if (showItem) display.itemStack else EMPTY_ITEM
            ) else it
        }

        private val addPacket
            get() = ClientboundAddEntityPacket(
                display.id,
                display.uuid,
                display.x,
                display.y + yOffset,
                display.z,
                display.xRot,
                display.yRot,
                display.type,
                0,
                display.deltaMovement,
                display.yHeadRot.toDouble()
            )

        private val removePacket
            get() = ClientboundRemoveEntitiesPacket(display.id)
    }

    override fun tint(itemStack: ItemStack, rgb: Int): ItemStack = itemStack.clone().apply {
        val meta = itemMeta
        if (meta is LeatherArmorMeta) {
            itemMeta = meta.apply {
                setColor(Color.fromRGB(rgb))
            }
        }
    }

    override fun createHitBox(entity: EntityAdapter, bone: RenderedBone, namedBoundingBox: NamedBoundingBox, mountController: MountController, listener: HitBoxListener): HitBox? {
        val handle = entity.handle() as? Entity ?: return null
        val newBox = namedBoundingBox.center()
        return HitBoxImpl(
            namedBoundingBox.name,
            newBox,
            bone,
            listener,
            handle,
            mountController
        ).craftEntity
    }

    override fun version(): NMSVersion = NMSVersion.V1_21_R2

    override fun adapt(entity: org.bukkit.entity.Entity): EntityAdapter {
        entity as CraftEntity
        return object : EntityAdapter {

            override fun entity(): org.bukkit.entity.Entity = entity
            override fun handle(): Entity = entity.vanillaEntity
            override fun id(): Int = handle().id
            override fun dead(): Boolean = (handle() as? LivingEntity)?.isDeadOrDying == true || handle().removalReason != null || !handle().valid
            override fun invisible(): Boolean = handle().isInvisible || (handle() as? LivingEntity)?.hasEffect(MobEffects.INVISIBILITY) == true
            override fun glow(): Boolean = handle().isCurrentlyGlowing

            override fun onWalk(): Boolean {
                return handle().isWalking()
            }

            override fun scale(): Double {
                val handle = handle()
                return if (handle is LivingEntity) handle.scale.toDouble() else 1.0
            }

            override fun pitch(): Float = handle().xRot
            override fun ground(): Boolean = handle().onGround()
            override fun bodyYaw(): Float = handle().yRot
            override fun headYaw(): Float = if (handle() is LivingEntity) handle().yHeadRot else bodyYaw()
            override fun fly(): Boolean = handle().isFlying

            override fun damageTick(): Float {
                val handle = handle()
                if (handle !is LivingEntity) return 0F
                val duration = handle.invulnerableDuration.toFloat()
                if (duration <= 0F) return 0F
                val knockBack = 1 - (handle.getAttribute(Attributes.KNOCKBACK_RESISTANCE)?.value?.toFloat() ?: 0F)
                return handle.invulnerableTime.toFloat() / duration * knockBack
            }

            override fun walkSpeed(): Float {
                val handle = handle()
                if (handle !is LivingEntity) return 0F
                if (!handle.onGround) return 1F
                val speed = handle.getEffect(MobEffects.MOVEMENT_SPEED)?.amplifier ?: 0
                val slow = handle.getEffect(MobEffects.MOVEMENT_SLOWDOWN)?.amplifier ?: 0
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

    override fun createPlayerHead(profile: GameProfile): ItemStack = VanillaItemStack(Items.PLAYER_HEAD).apply {
        set(DataComponents.PROFILE, ResolvableProfile(profile))
    }.asBukkit()

    override fun isProxyOnlineMode(): Boolean = ONLINE_MODE
}
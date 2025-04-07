package kr.toxicity.model.nms.v1_21_R4

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup
import com.google.common.collect.ImmutableList
import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import com.mojang.datafixers.util.Pair
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.*
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.tracker.ModelRotation
import kr.toxicity.model.api.tracker.Tracker
import net.minecraft.core.component.DataComponents
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.util.Brightness
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.level.entity.LevelEntityGetterAdapter
import net.minecraft.world.level.entity.PersistentEntitySectionManager
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform.*
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot.*
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.Vector3f
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NMSImpl : NMS {

    companion object {
        private const val INJECT_NAME = "bettermodel_channel_handler"

        //Spigot
        private val getGameProfile: (net.minecraft.world.entity.player.Player) -> GameProfile = createAdaptedFieldGetter { it.gameProfile }
        private val getConnection: (ServerCommonPacketListenerImpl) -> Connection = createAdaptedFieldGetter { it.connection }
        private val entityTracker = ServerLevel::class.java.fields.firstOrNull {
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
                    entityTracker?.get(this)?.let {
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
        private val transformSet = Display::class.java.serializers().map { e ->
            e.toSerializerId()
        }.toSet() + itemId + sharedFlag
    }

    private class PacketBundlerImpl(
        private val list: MutableList<Packet<in ClientGamePacketListener>>
    ) : PacketBundler, MutableList<Packet<in ClientGamePacketListener>> by list {
        override fun copy(): PacketBundler = PacketBundlerImpl(ArrayList(list))
        override fun send(player: Player) {
            val connection = (player as CraftPlayer).handle.connection
            when (size) {
                0 -> {}
                1 -> connection.send(get(0))
                else -> connection.send(ClientboundBundlePacket(this))
            }
        }
    }

    override fun hide(player: Player, entity: org.bukkit.entity.Entity) {
        val connection = (player as CraftPlayer).handle
        val entity = (entity as CraftEntity).handle
        val task = {
            connection.connection.send(ClientboundBundlePacket(listOf(
                ClientboundSetEntityDataPacket(entity.id, entity.entityData.pack()),
                ClientboundSetEquipmentPacket(entity.id, EquipmentSlot.entries.map { e ->
                    Pair.of(e, Items.AIR.defaultInstance)
                })
            )))
        }
        if (entity === connection) BetterModel.inst().scheduler().asyncTaskLater(1, task) else task()
    }

    inner class PlayerChannelHandlerImpl(
        private val player: Player
    ) : PlayerChannelHandler, ChannelDuplexHandler() {
        private val connection = (player as CraftPlayer).handle.connection
        private val entityUUIDMap = ConcurrentHashMap<UUID, EntityTracker>()
        private val slim = run {
            val encodedValue = getGameProfile((player as CraftPlayer).handle)
                .properties["textures"]
            encodedValue.isNotEmpty() && JsonParser.parseString(String(Base64.getDecoder().decode(encodedValue.first().value)))
                .asJsonObject
                .getAsJsonObject("textures")
                .getAsJsonObject("SKIN")
                .get("metadata")?.asJsonObject
                ?.get("model")?.asString == "slim"
        }

        init {
            val pipeLine = getConnection(connection).channel.pipeline()
            pipeLine.toMap().forEach {
                if (it.value is Connection) pipeLine.addBefore(it.key, INJECT_NAME, this)
            }
        }
        
        private var showPlayerLimb = true
        override fun showPlayerLimb(): Boolean = showPlayerLimb
        override fun showPlayerLimb(show: Boolean) {
            showPlayerLimb = show
        }
        override fun isSlim(): Boolean = slim

        override fun close() {
            val channel = getConnection(connection).channel
            channel.eventLoop().submit {
                channel.pipeline().remove(INJECT_NAME)
            }
            unregisterAll()
        }

        override fun unregisterAll() {
            entityUUIDMap.values.toList().forEach {
                it.remove(player)
            }
        }

        override fun player(): Player = player
        private fun send(packet: Packet<*>) = connection.send(packet)

        private fun Int.toPlayerEntity() = toEntity(connection.player.serverLevel())
        private fun Int.toTracker() = toPlayerEntity()?.let {
            entityUUIDMap[it.uuid]
        }

        override fun startTrack(tracker: EntityTracker) {
            val entity = (tracker.source() as CraftEntity).handle
            entityUUIDMap.computeIfAbsent(entity.uuid) {
                tracker
            }
        }

        override fun endTrack(tracker: EntityTracker) {
            val e = tracker.source()
            val handle = (e as CraftEntity).handle
            entityUUIDMap.remove(handle.uuid)
            val list = arrayListOf<Packet<ClientGamePacketListener>>()
            list += ClientboundSetEntityDataPacket(handle.id, handle.entityData.pack())
            if (e is LivingEntity) {
                e.equipment?.let { i ->
                    list += ClientboundSetEquipmentPacket(handle.id, org.bukkit.inventory.EquipmentSlot.entries.mapNotNull {
                        it to (runCatching {
                            i.getItem(it)
                        }.getOrNull() ?: return@mapNotNull null)
                    }.map { (type, item) ->
                        Pair.of(when (type) {
                            HAND -> EquipmentSlot.MAINHAND
                            OFF_HAND -> EquipmentSlot.OFFHAND
                            FEET -> EquipmentSlot.FEET
                            LEGS -> EquipmentSlot.LEGS
                            CHEST -> EquipmentSlot.CHEST
                            org.bukkit.inventory.EquipmentSlot.HEAD -> EquipmentSlot.HEAD
                            BODY -> EquipmentSlot.BODY
                            SADDLE -> EquipmentSlot.SADDLE
                        }, CraftItemStack.asNMSCopy(item))
                    })
                }
            }
            list += ClientboundSetPassengersPacket(handle)
            send(ClientboundBundlePacket(list))
        }

        private fun <T : ClientGamePacketListener> Packet<in T>.handle(): Packet<in T> {
            when (this) {
                is ClientboundBundlePacket -> return ClientboundBundlePacket(subPackets().map {
                    it.handle()
                })
                is ClientboundAddEntityPacket -> {
                    id.toPlayerEntity()?.let { e ->
                        if (!e.bukkitEntity.persistentDataContainer.has(Tracker.TRACKING_ID)) return this
                        BetterModel.inst().scheduler().taskLater(1, e.bukkitEntity.location) {
                            EntityTracker.tracker(e.bukkitEntity)?.let {
                                if (it.canBeSpawnedAt(player)) it.spawn(player)
                            }
                        }
                    }
                }
                is ClientboundRemoveEntitiesPacket -> {
                    entityIds
                        .asSequence()
                        .mapNotNull {
                            it.toTracker()
                        }
                        .forEach {
                            it.remove()
                        }
                }
                is ClientboundSetPassengersPacket -> {
                    vehicle.toTracker()?.let {
                        return it.mountPacket()
                    }
                }
                is ClientboundSetEntityDataPacket -> if (id.toTracker() != null) return ClientboundSetEntityDataPacket(id, packedItems().map {
                    if (it.id == sharedFlag) SynchedEntityData.DataValue<Byte>(
                        it.id,
                        EntityDataSerializers.BYTE,
                        ((it.value() as Byte).toInt() and 1.inv() or (1 shl 5) and (1 shl 6).inv()).toByte()
                    ) else it
                })
                is ClientboundSetEquipmentPacket -> if (entity.toTracker() != null) return ClientboundSetEquipmentPacket(entity, EquipmentSlot.entries.map { e ->
                    Pair.of(e, Items.AIR.defaultInstance)
                })
            }
            return this
        }

        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            if (msg is Packet<*>) {
                super.write(ctx, msg.handle(), promise)
                return
            }
            super.write(ctx, msg, promise)
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            when (msg) {
                is ServerboundSetCarriedItemPacket -> {
                    if (connection.player.id.toTracker() != null) {
                        connection.send(ClientboundSetHeldSlotPacket(player.inventory.heldItemSlot))
                        return
                    }
                }
                is ServerboundPlayerActionPacket -> {
                    if (connection.player.id.toTracker() != null) return
                }
            }
            super.channelRead(ctx, msg)
        }

        private fun EntityTracker.remove() {
            remove(player)
        }
    }

    override fun mount(tracker: EntityTracker, bundler: PacketBundler) {
        (bundler as PacketBundlerImpl).add(tracker.mountPacket())
    }

    private fun EntityTracker.mountPacket(entity: Entity = adapter.handle() as Entity): ClientboundSetPassengersPacket {
        val map = displays().mapNotNull {
            (it as? ModelDisplayImpl)?.display
        }
        val passengers = entity.passengers
        entity.passengers = ImmutableList.builder<Entity>()
            .addAll(map)
            .addAll(entity.passengers)
            .build()
        val packet = ClientboundSetPassengersPacket(entity)
        entity.passengers = passengers
        return packet
    }

    override fun inject(player: Player): PlayerChannelHandlerImpl = PlayerChannelHandlerImpl(player)

    override fun createBundler(): PacketBundler = PacketBundlerImpl(mutableListOf())
    private fun PacketBundler.unwrap(): PacketBundlerImpl = this as PacketBundlerImpl

    override fun create(location: Location): ModelDisplay = ModelDisplayImpl(ItemDisplay(EntityType.ITEM_DISPLAY, (location.world as CraftWorld).handle).apply {
        billboardConstraints = Display.BillboardConstraints.FIXED
        moveTo(
            location.x,
            location.y,
            location.z,
            location.yaw,
            0F
        )
        valid = false
        persist = false
        itemTransform = ItemDisplayContext.FIXED
        entityData.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 3)
    })

    private inner class ModelDisplayImpl(
        val display: ItemDisplay
    ) : ModelDisplay {

        private var isDead = false

        override fun rotate(rotation: ModelRotation, bundler: PacketBundler) {
            if (isDead) return
            display.xRot = rotation.x
            display.yRot = rotation.y
            bundler.unwrap().add(ClientboundMoveEntityPacket.Rot(
                display.id,
                rotation.packedY(),
                rotation.packedX(),
                display.onGround
            ))
        }

        override fun sync(entity: EntityAdapter) {
            isDead = entity.dead()
            display.setGlowingTag(entity.glow())
            if (BetterModel.inst().configManager().followMobInvisibility()) display.isInvisible = entity.invisible()
        }

        override fun display(transform: org.bukkit.entity.ItemDisplay.ItemDisplayTransform) {
            display.itemTransform = when (transform) {
                NONE -> ItemDisplayContext.NONE
                THIRDPERSON_LEFTHAND -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                THIRDPERSON_RIGHTHAND -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                FIRSTPERSON_LEFTHAND -> ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                FIRSTPERSON_RIGHTHAND -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                org.bukkit.entity.ItemDisplay.ItemDisplayTransform.HEAD -> ItemDisplayContext.HEAD
                GUI -> ItemDisplayContext.GUI
                GROUND -> ItemDisplayContext.GROUND
                FIXED -> ItemDisplayContext.FIXED
            }
        }

        override fun spawn(bundler: PacketBundler) {
            bundler.unwrap().add(display.addPacket)
            val f = display.transformationInterpolationDuration
            frame(0)
            bundler.unwrap().add(ClientboundSetEntityDataPacket(display.id, display.entityData.pack()))
            frame(f)
        }

        override fun frame(frame: Int) {
            display.transformationInterpolationDuration = frame
        }

        override fun remove(bundler: PacketBundler) {
            bundler.unwrap().add(removePacket)
        }

        override fun teleport(location: Location, bundler: PacketBundler) {
            display.moveTo(
                location.x,
                location.y,
                location.z,
                location.yaw,
                0F
            )
            bundler.unwrap().add(ClientboundTeleportEntityPacket.teleport(display.id, PositionMoveRotation.of(display), emptySet(), display.onGround))
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

        override fun syncPosition(adapter: EntityAdapter, bundler: PacketBundler) {
            val handle = adapter.handle() as net.minecraft.world.entity.LivingEntity? ?: return
            display.setPos(handle.position())
            display.onGround = handle.onGround
            bundler.unwrap().add(ClientboundEntityPositionSyncPacket(
                display.id,
                PositionMoveRotation.of(handle),
                handle.onGround
            ))
        }

        override fun transform(transformation: Transformation) {
            display.setTransformation(com.mojang.math.Transformation(
                transformation.translation,
                transformation.leftRotation,
                transformation.scale,
                transformation.rightRotation
            ))
        }

        override fun send(bundler: PacketBundler) {
            if (display.isInvisible) {
                val item = display.itemStack
                display.itemStack = Items.AIR.defaultInstance
                bundler.unwrap().add(dataPacket)
                display.itemStack = item
            } else {
                bundler.unwrap().add(dataPacket)
            }
        }

        private val dataPacket
            get(): ClientboundSetEntityDataPacket = ClientboundSetEntityDataPacket(display.id, display.entityData.pack().filter {
                transformSet.contains(it.id)
            })

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
            set(DataComponents.DYED_COLOR, DyedItemColor(rgb))
            set(DataComponents.CUSTOM_MODEL_DATA, get(DataComponents.CUSTOM_MODEL_DATA)?.let {
                CustomModelData(it.floats, it.flags, it.strings, listOf(rgb))
            })
        })
    }

    override fun createHitBox(entity: EntityAdapter, supplier: HitBoxSource, namedBoundingBox: NamedBoundingBox, mountController: MountController, listener: HitBoxListener): HitBox? {
        val handle = (entity.entity() as? CraftLivingEntity)?.handle ?: return null
        val scale = entity.scale()
        val newBox = namedBoundingBox.center() * scale
        val height = newBox.y() / 2
        return HitBoxImpl(
            namedBoundingBox.name,
            height,
            newBox,
            supplier,
            listener,
            handle,
            mountController,
            entity
        ).apply {
            attributes.getInstance(Attributes.SCALE)!!.baseValue = height / 0.52
            refreshDimensions()
            handle.level().addFreshEntity(this)
        }
    }
    override fun version(): NMSVersion = NMSVersion.V1_21_R4

    override fun adapt(entity: LivingEntity): EntityAdapter {
        val craftEntity = entity as CraftLivingEntity
        return object : EntityAdapter {
            
            override fun entity(): LivingEntity = entity
            override fun handle(): net.minecraft.world.entity.LivingEntity = craftEntity.vanillaEntity as net.minecraft.world.entity.LivingEntity
            override fun dead(): Boolean = handle().isDeadOrDying
            override fun invisible(): Boolean = handle().isInvisible
            override fun glow(): Boolean = handle().isCurrentlyGlowing

            override fun onWalk(): Boolean {
                return handle().isWalking()
            }

            override fun scale(): Double {
                return handle().attributes.getInstance(Attributes.SCALE)?.value ?: 1.0
            }

            override fun pitch(): Float {
                return handle().xRot
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
                val duration = handle.invulnerableDuration.toFloat()
                if (duration <= 0F) return 0F
                val knockBack = 1 - (handle.getAttribute(Attributes.KNOCKBACK_RESISTANCE)?.value?.toFloat() ?: 0F)
                return handle.invulnerableTime.toFloat() / duration * knockBack * (1F - 1F / (handle.deltaMovement.length().toFloat() * 20 + 1F))
            }

            override fun walkSpeed(): Float {
                val handle = handle()
                if (!handle.onGround) return 1F
                val speed = handle.getEffect(MobEffects.SPEED)?.amplifier ?: 0
                val slow = handle.getEffect(MobEffects.SLOWNESS)?.amplifier ?: 0
                return (1F + (speed - slow) * 0.2F)
                    .coerceAtLeast(0.2F)
                    .coerceAtMost(2F)
            }

            override fun passengerPosition(): Vector3f {
                return handle().passengerPosition(scale())
            }
        }
    }

    override fun isSync(): Boolean = isTickThread
}
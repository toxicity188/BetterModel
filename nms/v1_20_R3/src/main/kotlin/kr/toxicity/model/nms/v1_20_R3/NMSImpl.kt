package kr.toxicity.model.nms.v1_20_R3

import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import com.mojang.datafixers.util.Pair
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.papermc.paper.chunk.system.entity.EntityLookup
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.data.blueprint.NamedBoundingBox
import kr.toxicity.model.api.mount.MountController
import kr.toxicity.model.api.nms.*
import kr.toxicity.model.api.tracker.EntityTracker
import kr.toxicity.model.api.tracker.ModelRotation
import kr.toxicity.model.api.tracker.Tracker
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
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.level.entity.LevelEntityGetterAdapter
import net.minecraft.world.level.entity.PersistentEntitySectionManager
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
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
        private val spigotChunkAccess = ServerLevel::class.java.fields.firstOrNull {
            it.type == PersistentEntitySectionManager::class.java
        }?.apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        private val ServerLevel.levelGetter
            get(): LevelEntityGetter<Entity> {
                return if (BetterModel.IS_PAPER) {
                    entityLookup
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
        private val transformSet = Display::class.java.serializers().map { e ->
            e.toSerializerId()
        }.toSet() + itemId + sharedFlag
    }

    private class PacketBundlerImpl(
        private val list: MutableList<Packet<ClientGamePacketListener>>
    ) : PacketBundler {
        override fun copy(): PacketBundler = PacketBundlerImpl(ArrayList(list))
        override fun send(player: Player) {
            val connection = (player as CraftPlayer).handle.connection
            when (list.size) {
                0 -> {}
                1 -> connection.send(list[0])
                else -> connection.send(ClientboundBundlePacket(list))
            }
        }
        override fun isEmpty(): Boolean = list.isEmpty()
        operator fun plusAssign(other: Packet<ClientGamePacketListener>) {
            list += other
        }
    }

    override fun hide(player: Player, entity: org.bukkit.entity.Entity) {
        val connection = (player as CraftPlayer).handle
        val entity = (entity as CraftEntity).vanillaEntity
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
        private val uuidValuesView = Collections.unmodifiableCollection(entityUUIDMap.values)
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
            val pipeLine = connection.connection.channel.pipeline()
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
        override fun trackedTrackers(): Collection<EntityTracker> = uuidValuesView

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
            val entity = (tracker.sourceEntity() as CraftEntity).vanillaEntity
            entityUUIDMap.computeIfAbsent(entity.uuid) {
                tracker
            }
        }

        override fun endTrack(tracker: EntityTracker) {
            val e = tracker.sourceEntity()
            val handle = (e as CraftEntity).vanillaEntity
            entityUUIDMap.remove(handle.uuid)
            val list = arrayListOf<Packet<ClientGamePacketListener>>()
            list += ClientboundSetEntityDataPacket(handle.id, handle.entityData.nonDefaultValues!!)
            if (handle is LivingEntity) {
                list += ClientboundSetEquipmentPacket(handle.id, EquipmentSlot.entries.mapNotNull {
                    runCatching {
                        Pair.of(it, handle.getItemBySlot(it))
                    }.getOrNull()
                })
            }
            list += ClientboundSetPassengersPacket(handle)
            send(ClientboundBundlePacket(list))
        }

        private fun <T : ClientGamePacketListener> Packet<in T>.handle(): Packet<in T> {
            when (this) {
                is ClientboundBundlePacket -> return ClientboundBundlePacket(subPackets().mapNotNull {
                    it.handle() as? Packet<ClientGamePacketListener>
                })
                is ClientboundAddEntityPacket -> {
                    id.toPlayerEntity()?.let { e ->
                        if (!e.bukkitEntity.persistentDataContainer.has(Tracker.TRACKING_ID)) return this
                        BetterModel.inst().scheduler().taskLater(1, e.bukkitEntity) {
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
                        return it.mountPacket(array = useByteBuf { buffer ->
                            write(buffer)
                            buffer.readVarInt()
                            buffer.readVarIntArray()
                        })
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
                        connection.send(ClientboundSetCarriedItemPacket(player.inventory.heldItemSlot))
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
        bundler.unwrap() += tracker.mountPacket()
    }

    private fun EntityTracker.mountPacket(entity: Entity = adapter.handle() as Entity, array: IntArray = entity.passengers.map {
        it.id
    }.toIntArray()): ClientboundSetPassengersPacket {
        return useByteBuf { buffer ->
            buffer.writeVarInt(entity.id)
            buffer.writeVarIntArray((displays().mapNotNull {
                (it as? ModelDisplayImpl)?.display?.id
            }.toIntArray() + array))
            ClientboundSetPassengersPacket(buffer)
        }
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
        itemTransform = ItemDisplayContext.FIXED
        entityData.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 3)
    })

    private inner class ModelDisplayImpl(
        val display: ItemDisplay
    ) : ModelDisplay {

        private var forceGlow = false

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

        override fun sync(entity: EntityAdapter) {
            display.valid = !entity.dead()
            display.onGround = entity.ground()
            display.setGlowingTag(entity.glow() || forceGlow)
            if (BetterModel.inst().configManager().followMobInvisibility()) display.isInvisible = entity.invisible()
        }

        override fun display(transform: org.bukkit.entity.ItemDisplay.ItemDisplayTransform) {
            display.itemTransform = ItemDisplayContext.BY_ID.apply(transform.ordinal)
        }

        override fun spawn(bundler: PacketBundler) {
            bundler.unwrap() += addPacket
            val f = display.transformationInterpolationDuration
            frame(0)
            bundler.unwrap() += ClientboundSetEntityDataPacket(display.id, display.entityData.nonDefaultValues!!)
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
            bundler.unwrap() += ClientboundTeleportEntityPacket(display)
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
            teleport(adapter.entity().location, bundler)
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
                bundler.unwrap() += dataPacket
                display.itemStack = item
            } else {
                bundler.unwrap() += dataPacket
            }
        }

        private val dataPacket
            get(): ClientboundSetEntityDataPacket = ClientboundSetEntityDataPacket(display.id, display.entityData.pack().filter {
                transformSet.contains(it.id)
            })

        private val removePacket
            get() = ClientboundRemoveEntitiesPacket(display.id)

        private val addPacket
            get() = ClientboundAddEntityPacket(
                display.id,
                display.uuid,
                display.x,
                display.y,
                display.z,
                display.xRot,
                display.yRot,
                display.type,
                0,
                display.deltaMovement,
                display.yHeadRot.toDouble()
            )
    }

    override fun tint(itemStack: ItemStack, rgb: Int): ItemStack {
        if (itemStack.isAirOrEmpty) return itemStack
        val meta = itemStack.itemMeta
        if (meta is LeatherArmorMeta) {
            itemStack.itemMeta = meta.apply {
                setColor(Color.fromRGB(rgb))
            }
        }
        return itemStack
    }

    override fun createHitBox(entity: EntityAdapter, supplier: HitBoxSource, namedBoundingBox: NamedBoundingBox, mountController: MountController, listener: HitBoxListener): HitBox? {
        val handle = entity.handle() as? LivingEntity ?: return null
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

    override fun version(): NMSVersion = NMSVersion.V1_20_R3

    override fun adapt(entity: org.bukkit.entity.Entity): EntityAdapter {
        entity as CraftEntity
        return object : EntityAdapter {

            override fun entity(): org.bukkit.entity.Entity = entity
            override fun handle(): Entity = entity.vanillaEntity
            override fun dead(): Boolean = (handle() as? LivingEntity)?.isDeadOrDying == true || !handle().valid
            override fun invisible(): Boolean = handle().isInvisible
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

    override fun isSync(): Boolean = isTickThread
}
package kr.toxicity.model.nms.v1_21_R2

import kr.toxicity.model.api.util.MathUtil
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Display
import org.joml.Quaternionf
import org.joml.Vector3f
import java.lang.reflect.Field

internal fun Field.toEntityDataAccessor() = run {
    isAccessible = true
    get(null) as EntityDataAccessor<*>
}

internal fun Class<*>.accessors() = declaredFields.filter { f ->
    EntityDataAccessor::class.java.isAssignableFrom(f.type)
}.map {
    it.toEntityDataAccessor()
}

internal val DISPLAY_SET = Display::class.java.accessors()

@Suppress("UNCHECKED_CAST")
internal val DISPLAY_INTERPOLATION_DELAY = DISPLAY_SET.first() as EntityDataAccessor<Int>
@Suppress("UNCHECKED_CAST")
internal val DISPLAY_INTERPOLATION_DURATION = DISPLAY_SET[1] as EntityDataAccessor<Int>
@Suppress("UNCHECKED_CAST")
internal val DISPLAY_TRANSLATION = DISPLAY_SET[3] as EntityDataAccessor<Vector3f>
@Suppress("UNCHECKED_CAST")
internal val DISPLAY_SCALE = DISPLAY_SET[4] as EntityDataAccessor<Vector3f>
@Suppress("UNCHECKED_CAST")
internal val DISPLAY_ROTATION = DISPLAY_SET[5] as EntityDataAccessor<Quaternionf>


internal class TransformationData {

    private val delay = SynchedEntityData.DataValue(DISPLAY_INTERPOLATION_DELAY.id, DISPLAY_INTERPOLATION_DELAY.serializer, 0)
    private var _duration = 0
    private val duration get() = SynchedEntityData.DataValue(DISPLAY_INTERPOLATION_DURATION.id, DISPLAY_INTERPOLATION_DURATION.serializer, _duration)
    private val translation = Item(Vector3f(), DISPLAY_TRANSLATION, MathUtil::isSimilar)
    private val scale = Item(Vector3f(), DISPLAY_SCALE, MathUtil::isSimilar)
    private val rotation = Item(Quaternionf(), DISPLAY_ROTATION, MathUtil::isSimilar)

    fun packDirty(): List<SynchedEntityData.DataValue<*>>? {
        val i = translation.cleanIndex + scale.cleanIndex + rotation.cleanIndex
        if (i == 0) return null
        return ArrayList<SynchedEntityData.DataValue<*>>(i + 2).apply {
            add(delay)
            add(duration)
            translation.value?.let { add(it) }
            scale.value?.let { add(it) }
            rotation.value?.let { add(it) }
        }
    }

    fun transform(
        duration: Int,
        translation: Vector3f,
        scale: Vector3f,
        rotation: Quaternionf
    ) {
        _duration = duration
        this.translation.set(translation)
        this.scale.set(scale)
        this.rotation.set(rotation)
    }

    fun pack() = listOf(
        delay,
        duration,
        translation.forceValue,
        scale.forceValue,
        rotation.forceValue
    )

    private class Item<T : Any>(
        initialValue: T,
        private val accessor: EntityDataAccessor<T>,
        private val dirtyChecker: (T, T) -> Boolean
    ) {
        private var _t: T = initialValue
        private var _dirty = false

        val dirty get() = _dirty
        val cleanIndex get() = if (dirty) 1 else 0
        val value get() = if (_dirty) {
            _dirty = false
            forceValue
        } else null
        val forceValue get() = SynchedEntityData.DataValue(accessor.id, accessor.serializer, _t)

        fun set(vector3f: T) {
            if (dirtyChecker(_t, vector3f)) return
            _dirty = true
            _t = vector3f
        }
    }
}


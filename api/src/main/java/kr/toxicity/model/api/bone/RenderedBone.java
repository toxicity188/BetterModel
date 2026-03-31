/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.BlueprintElement;
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.platform.PlatformItemStack;
import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.platform.PlatformPlayer;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.util.*;
import kr.toxicity.model.api.util.collection.SingletonSequencedSet;
import kr.toxicity.model.api.util.function.BonePredicate;
import kr.toxicity.model.api.util.function.FloatConstantSupplier;
import kr.toxicity.model.api.util.function.FloatSupplier;
import kr.toxicity.model.api.util.lock.DuplexLock;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A rendered item-display.
 */
public final class RenderedBone implements BoneEventHandler {

    private static final int INITIAL_TINT_VALUE = 0xFFFFFF;
    private static final Vector3f EMPTY_VECTOR = new Vector3f();
    private static final Quaternionf EMPTY_QUATERNION = new Quaternionf();
    private static final BonePosition EMPTY_POSITION = new BonePosition(EMPTY_VECTOR, EMPTY_VECTOR, null);

    @Getter
    @NotNull
    final RendererGroup group;
    private final BoneMovement defaultFrame;
    private volatile BoneRenderContext renderContext;
    private final BoneEventDispatcher eventDispatcher = new BoneEventDispatcher();

    @NotNull
    @Getter
    final RenderedBone root;
    @Nullable
    @Getter
    final RenderedBone parent;
    final RenderedBone[] children;

    private volatile SequencedSet<RenderedBone> flattenBones;

    private final Int2ObjectMap<PlatformItemStack> tintCacheMap = new Int2ObjectOpenHashMap<>();
    @Getter
    private final boolean dummyBone;
    private final Object itemLock = new Object();

    //Resource
    @Getter
    @Nullable
    private final ModelDisplay display;
    @Getter
    @Nullable
    private HitBox hitBox;
    @Getter
    @Nullable
    private ModelNametag nametag;

    //Item
    @Getter
    @Setter
    private BoneItemMapper itemMapper;
    private volatile int previousTint = INITIAL_TINT_VALUE, tint = INITIAL_TINT_VALUE;
    private volatile TransformedItemStack itemStack;

    //Animation
    private final BoneStateHandler globalState;
    private final Map<UUID, BoneStateHandler> perPlayerState = new ConcurrentHashMap<>();
    private volatile ModelRotation rotation = ModelRotation.EMPTY;

    private Supplier<Vector3f> defaultPosition = FunctionUtil.asSupplier(EMPTY_VECTOR);
    private FloatSupplier scale = FloatConstantSupplier.ONE;

    private Function<Vector3f, Vector3f> positionModifier = p -> p;
    private Vector3f lastModifiedPosition = new Vector3f();
    private Function<Quaternionf, Quaternionf> rotationModifier = r -> r;
    private Quaternionf lastModifiedRotation = new Quaternionf();

    /**
     * Creates entity.
     * @param group group
     * @param parent parent entity
     * @param context render context
     * @param movement spawn movement
     * @param childrenMapper mapper
     */
    @ApiStatus.Internal
    public RenderedBone(
        @NotNull RendererGroup group,
        @Nullable RenderedBone parent,
        @NotNull BoneRenderContext context,
        @NotNull BoneMovement movement,
        @NotNull Function<RenderedBone, RenderedBone[]> childrenMapper
    ) {
        this.group = group;
        this.parent = parent;
        this.renderContext = context;
        itemMapper = group.getItemMapper();
        root = parent != null ? parent.root : this;
        this.itemStack = itemMapper.apply(renderContext, group.getItemStack());
        this.dummyBone = group.getItemStack().isAir() && itemMapper == BoneItemMapper.EMPTY;
        defaultFrame = movement;
        children = childrenMapper.apply(this);
        if (!dummyBone) {
            display = BetterModel.nms().create(context.source().location(), context.source() instanceof RenderSource.Entity ? -4096 : 0, d -> {
                d.display(itemMapper.transform());
                d.invisible(!group.getParent().visibility());
                d.viewRange(EntityUtil.entityModelViewRadius());
                applyItem(d);
            });
        } else display = null;
        globalState = new BoneStateHandler(null, uuid -> {});
    }

    public void locator(@NotNull BoneIKSolver solver) {
        if (getGroup().getParent() instanceof BlueprintElement.NullObject nullObject) {
            var ikTarget = nullObject.ikTarget();
            if (ikTarget == null) return;
            solver.addLocator(nullObject.ikSource(), ikTarget, this);
        }
    }

    private @NotNull BoneStateHandler state(@Nullable PlatformPlayer player) {
        return state(player != null ? player.uuid() : null);
    }

    @NotNull BoneStateHandler state(@Nullable UUID uuid) {
        return uuid == null ? globalState : perPlayerState.getOrDefault(uuid, globalState);
    }

    private @NotNull BoneStateHandler getOrCreateState(@Nullable PlatformPlayer player) {
        return getOrCreateState(player != null ? player.uuid() : null);
    }

    private @NotNull BoneStateHandler getOrCreateState(@Nullable UUID uuid) {
        return uuid == null ? globalState : perPlayerState.computeIfAbsent(uuid, u -> {
            eventDispatcher.onStateCreated(this, u);
            return new BoneStateHandler(u, targetUUID -> eventDispatcher.onStateRemoved(this, targetUUID));
        });
    }

    public @Nullable RunningAnimation runningAnimation() {
        return globalState.state.runningAnimation();
    }

    @Override
    public @NotNull BoneEventDispatcher eventDispatcher() {
        return eventDispatcher;
    }

    public boolean updateItem(@NotNull Predicate<RenderedBone> predicate) {
        return itemStack(predicate, itemMapper.apply(renderContext, itemStack));
    }

    public boolean updateItem(@NotNull BoneRenderContext context) {
        synchronized (this) {
            renderContext = context;
        }
        return updateItem(bone -> true);
    }

    /**
     * Creates hit box.
     * @param entity target entity
     * @param predicate predicate
     * @param listener hit box listener
     * @return success
     */
    public boolean createHitBox(@NotNull BaseEntity entity, @NotNull Predicate<RenderedBone> predicate, @Nullable HitBoxListener listener) {
        if (predicate.test(this)) {
            var previous = hitBox;
            synchronized (this) {
                if (previous != hitBox) return false;
                var h = group.getHitBox();
                if (h == null) h = ModelBoundingBox.MIN;
                var l = eventDispatcher.onCreateHitBox(this, (listener != null ? listener : HitBoxListener.EMPTY).toBuilder()).build();
                if (hitBox != null) hitBox.removeHitBox();
                hitBox = BetterModel.nms().createHitBox(entity, this, h, group.getMountController(), l);
                return hitBox != null;
            }
        }
        return false;
    }

    /**
     * Creates nametag
     * @param predicate predicate
     * @param consumer nametag consumer
     * @return success
     */
    public boolean createNametag(@NotNull Predicate<RenderedBone> predicate, @NotNull Consumer<ModelNametag> consumer) {
        if (nametag == null && predicate.test(this)) {
            synchronized (this) {
                if (nametag != null) return false;
                nametag = BetterModel.nms().createNametag(this, consumer);
            }
            return true;
        }
        return false;
    }

    /**
     * Make item has enchantment or not
     * @param predicate predicate
     * @param enchant should enchant
     * @return success or not
     */
    public boolean enchant(@NotNull Predicate<RenderedBone> predicate, boolean enchant) {
        return itemStack(predicate, itemStack.modify(i -> i.enchant(enchant)));
    }

    /**
     * Sets the scale of this bone
     * @param scale scale
     */
    public void scale(@NotNull FloatSupplier scale) {
        this.scale = scale;
    }

    /**
     * Applies some function at display
     * @param predicate predicate
     * @param consumer consumer
     * @return success or not
     */
    public boolean applyAtDisplay(@NotNull Predicate<RenderedBone> predicate, @NotNull Consumer<ModelDisplay> consumer) {
        if (display != null && predicate.test(this)) {
            consumer.accept(display);
            return true;
        }
        return false;
    }

    /**
     * Changes displayed item
     * @param predicate predicate
     * @param itemStack target item
     * @return success
     */
    public boolean itemStack(@NotNull Predicate<RenderedBone> predicate, @NotNull TransformedItemStack itemStack) {
        if (this.itemStack != itemStack && predicate.test(this)) {
            synchronized (itemLock) {
                if (this.itemStack == itemStack) return false;
                this.itemStack = itemStack;
                if (display != null) display.invisible(itemStack.isAir());
                tintCacheMap.clear();
                return applyItem();
            }
        }
        return false;
    }

    /**
     * Adds rotation modifier.
     * @param predicate predicate
     * @param function animation consumer
     * @return whether to success
     */
    public synchronized boolean addRotationModifier(@NotNull Predicate<RenderedBone> predicate, @NotNull Function<Quaternionf, Quaternionf> function) {
        if (predicate.test(this)) {
            rotationModifier = rotationModifier.andThen(function);
            return true;
        }
        return false;
    }

    /**
     * Adds position modifier.
     * @param predicate predicate
     * @param function animation consumer
     * @return whether to success
     */
    public synchronized boolean addPositionModifier(@NotNull Predicate<RenderedBone> predicate, @NotNull Function<Vector3f, Vector3f> function) {
        if (predicate.test(this)) {
            positionModifier = positionModifier.andThen(function);
            return true;
        }
        return false;
    }

    public boolean rotate(@NotNull ModelRotation rotation, @NotNull PacketBundler bundler) {
        this.rotation = rotation;
        if (display != null) {
            display.rotate(rotation, bundler);
            return true;
        }
        return false;
    }

    public boolean tick() {
        return globalState.tick();
    }

    public boolean tick(@NotNull UUID uuid) {
        var get = perPlayerState.get(uuid);
        return get != null && get.tick();
    }

    public void dirtyUpdate(@NotNull PacketBundler bundler) {
        var d = display;
        if (d != null) d.sendDirtyEntityData(bundler);
    }

    public void forceUpdate(boolean showItem, @NotNull PacketBundler bundler) {
        var d = display;
        if (d != null) d.sendEntityData(showItem, bundler);
    }

    public void forceUpdate(@NotNull PacketBundler bundler) {
        var d = display;
        if (d != null) d.sendEntityData(!d.invisible(), bundler);
    }

    public void sendTransformation(@Nullable UUID uuid, @NotNull AnimationBundler bundler) {
        state(uuid).sendTransformation(bundler);
    }

    public void forceTransformation(@NotNull PacketBundler bundler) {
        var d = globalState.transformer;
        if (d != null) d.sendTransformation(bundler);
    }

    public int interpolationDuration() {
        return globalState.interpolationDuration();
    }

    public @NotNull Vector3f worldPosition() {
        return worldPosition(EMPTY_POSITION);
    }

    public @NotNull Vector3f worldPosition(@NotNull BonePosition position) {
        return worldPosition(position, new BoneMovement());
    }

    public @NotNull Vector3f worldPosition(@NotNull BoneMovement cache) {
        return worldPosition(EMPTY_POSITION, cache);
    }

    public @NotNull Vector3f worldPosition(@NotNull BonePosition position, @NotNull BoneMovement cache) {
        return state(position.state()).worldPosition(position, cache);
    }

    public @NotNull Vector3f worldRotation() {
        return worldRotation(null);
    }

    public @NotNull Vector3f worldRotation(@Nullable UUID uuid) {
        return state(uuid).worldRotation();
    }

    public void defaultPosition(@NotNull Supplier<Vector3f> movement) {
        defaultPosition = movement;
    }

    private @NotNull Vector3f modifiedPosition(boolean preventModifierUpdate) {
        return preventModifierUpdate ? lastModifiedPosition : (lastModifiedPosition = positionModifier.apply(lastModifiedPosition.set(EMPTY_VECTOR)));
    }

    private @NotNull Quaternionf modifiedRotation(boolean preventModifierUpdate) {
        return preventModifierUpdate ? lastModifiedRotation : (lastModifiedRotation = rotationModifier.apply(lastModifiedRotation.set(EMPTY_QUATERNION)));
    }

    public boolean tint(@NotNull Predicate<RenderedBone> predicate) {
        return tint(predicate, previousTint);
    }

    public boolean tint(@NotNull Predicate<RenderedBone> predicate, int tint) {
        if (this.tint != tint && predicate.test(this)) {
            synchronized (itemLock) {
                if (this.tint == tint) return false;
                this.previousTint = this.tint;
                this.tint = tint;
                return applyItem();
            }
        }
        return false;
    }

    private boolean applyItem() {
        if (display != null) {
            applyItem(display);
            return true;
        }
        return false;
    }

    private void applyItem(@NotNull ModelDisplay targetDisplay) {
        targetDisplay.item(itemStack.isAir() ? itemStack.itemStack() : tintCacheMap.computeIfAbsent(tint, i -> BetterModel.nms().tint(itemStack.itemStack(), i)));
    }

    public void teleport(@NotNull PlatformLocation location, @NotNull PacketBundler bundler) {
        if (display != null) display.teleport(location, bundler);
    }

    public void spawn(boolean hide, @NotNull PacketBundler bundler) {
        if (display != null) display.spawn(!hide && !display.invisible(), bundler);
        var transformer = globalState.transformer;
        if (transformer != null) transformer.sendTransformation(bundler);
    }

    public boolean addAnimation(@NotNull AnimationOverrideState overrideState, @NotNull BlueprintAnimation animator, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        var get = animator.animator().get(name());
        if (get == null && modifier.override(animator.override()) && overrideState.shouldSkip()) return false;
        var type = modifier.type(animator.loop());
        var iterator = get != null ? get.iterator(type) : animator.emptyIterator(type);
        getOrCreateState(modifier.player()).state.addAnimation(animator.name(), iterator, modifier, removeTask);
        return true;
    }

    public boolean replaceAnimation(@NotNull AnimationOverrideState overrideState, @NotNull String target, @NotNull BlueprintAnimation animator, @NotNull AnimationModifier modifier) {
        var get = animator.animator().get(name());
        if (get == null && modifier.override(animator.override()) && overrideState.shouldSkip()) return false;
        var type = modifier.type(animator.loop());
        var iterator = get != null ? get.iterator(type) : animator.emptyIterator(type);
        state(modifier.player()).state.replaceAnimation(target, iterator, modifier);
        return true;
    }

    /**
     * Stops bone's animation
     * @param filter filter
     * @param name animation's name
     * @param player player
     */
    public boolean stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String name, @Nullable PlatformPlayer player) {
        return filter.test(this) && state(player).state.stopAnimation(name);
    }

    /**
     * Removes model's display
     * @param bundler packet bundler
     */
    public void remove(@NotNull PacketBundler bundler) {
        if (display != null) display.remove(bundler);
        if (nametag != null) nametag.remove(bundler);
    }

    public @NotNull Stream<RenderedBone> flatten() {
        return flattenBones().stream();
    }

    @Unmodifiable
    @NotNull
    public SequencedSet<RenderedBone> flattenBones() {
        if (flattenBones != null) return flattenBones;
        synchronized (this) {
            if (flattenBones != null) return flattenBones;
            return flattenBones = children.length == 0 ? SingletonSequencedSet.of(this) : Stream.concat(
                Stream.of(this),
                Arrays.stream(children).flatMap(RenderedBone::flatten)
            ).collect(Collectors.collectingAndThen(
                Collectors.toCollection(ObjectLinkedOpenHashSet::new),
                Collections::unmodifiableSequencedSet
            ));
        }
    }

    public boolean matchTree(@NotNull BonePredicate predicate, @NotNull BiPredicate<RenderedBone, BonePredicate> mapper) {
        var parentResult = mapper.test(this, predicate);
        var childPredicate = predicate.children(parentResult);
        for (RenderedBone value : children) {
            if (value.matchTree(childPredicate, mapper)) parentResult = true;
        }
        return parentResult;
    }

    public boolean matchAnimation(@NotNull AnimationOverrideState overrideState, @NotNull BiPredicate<RenderedBone, AnimationOverrideState> mapper) {
        var parentResult = mapper.test(this, overrideState);
        if (parentResult) overrideState = AnimationOverrideState.MATCHED;
        for (RenderedBone value : children) {
            if (value.matchAnimation(overrideState, mapper)) parentResult = true;
        }
        return parentResult;
    }

    @NotNull
    public Vector3f hitBoxPosition() {
        return hitBoxPosition(new BoneMovement());
    }

    @NotNull
    public Vector3f hitBoxPosition(@NotNull BoneMovement cache) {
        var box = getGroup().getHitBox();
        if (box != null) return worldPosition(new BonePosition(EMPTY_VECTOR, group.getHitBoxPoint(), null), cache);
        return worldPosition(cache);
    }

    public float hitBoxScale() {
        return scale.getAsFloat();
    }

    @NotNull
    public ModelRotation rotation() {
        return rotation;
    }

    final class BoneStateHandler {

        private final @Nullable UUID uuid;
        private final Consumer<UUID> consumer;

        //States
        private final AnimationStateHandler<AnimationProgress> state;
        private final BoneMovement before = new BoneMovement(), after = new BoneMovement(), current = new BoneMovement();
        private final DisplayTransformer transformer = display != null ? display.createTransformer() : null;

        //Flags
        private boolean firstTick = true;
        private boolean skipInterpolation = false;
        private final AtomicBoolean updateAfter = new AtomicBoolean();
        private final AtomicBoolean updateCurrent = new AtomicBoolean();

        //Caches
        private final Vector3f positionCache = new Vector3f(), scaleCache = new Vector3f();
        private final Quaternionf rotationCache = new Quaternionf();

        //Lock
        private final DuplexLock lock = new DuplexLock();

        private BoneStateHandler(@Nullable UUID uuid, @NotNull Consumer<UUID> consumer) {
            this.uuid = uuid;
            this.consumer = consumer;
            state = new AnimationStateHandler<>(
                AnimationProgress.EMPTY,
                (b, a) -> skipInterpolation = (a != null && a.skipInterpolation()) || (parent != null && parent.state(uuid).skipInterpolation)
            );
        }

        @NotNull BoneMovement after() {
            if (!updateAfter.compareAndSet(true, false)) return after;
            var keyframe = state.afterKeyframe(AnimationProgress.EMPTY);
            var preventModifierUpdate = interpolationDuration() < 1;
            var def = keyframe.animate(defaultFrame, after);
            if (parent != null) {
                var p = parent.state(uuid).after();
                MathUtil.fma(
                        def.position().rotate(p.rotation()),
                        p.scale(),
                        p.position()
                    ).sub(parent.lastModifiedPosition)
                    .add(modifiedPosition(preventModifierUpdate));
                def.scale().mul(p.scale());
                def.rotation().set((keyframe.globalRotation() ? rotationCache.identity() : p.rotation().div(parent.lastModifiedRotation, rotationCache))
                    .mul(def.rotation())
                    .mul(modifiedRotation(preventModifierUpdate)));
            } else {
                def.position().add(modifiedPosition(preventModifierUpdate));
                def.rotation().mul(modifiedRotation(preventModifierUpdate));
            }
            return def;
        }

        private boolean tick() {
            var result = state.tick(() -> {
                if (uuid != null) {
                    perPlayerState.remove(uuid);
                    consumer.accept(uuid);
                }
            }) || firstTick;
            if (result && updateAfter.compareAndSet(false, true)) {
                lock.accessToWriteLock(() -> before.set(current));
                updateCurrent.set(true);
            }
            firstTick = false;
            return result;
        }

        private float progress() {
            return 1F - state.progress();
        }

        private int interpolationDuration() {
            if (skipInterpolation) return 0;
            var frame = state.frame() / (float) Tracker.MINECRAFT_TICK_MULTIPLIER;
            return Math.round(frame + MathUtil.FLOAT_COMPARISON_EPSILON);
        }

        private void sendTransformation(@NotNull AnimationBundler bundler) {
            if (!updateCurrent.compareAndSet(true, false)) return;
            var after = after();
            var movement = lock.accessToWriteLock(() -> current.set(after));
            if (transformer == null) return;
            var mul = scale.getAsFloat();
            transformer.transform(
                interpolationDuration(),
                MathUtil.fma(
                    itemStack.offset().rotate(movement.rotation(), positionCache)
                        .add(movement.position())
                        .add(root.group.getPosition()),
                    mul,
                    itemStack.position()
                ).add(defaultPosition.get()),
                movement.scale()
                    .mul(itemStack.scale(), scaleCache)
                    .mul(mul)
                    .max(EMPTY_VECTOR),
                movement.rotation(),
                bundler
            );
        }

        private @NotNull Vector3f worldPosition(@NotNull BonePosition position, @NotNull BoneMovement cache) {
            var progress = progress();
            var interpolated = lock.accessToReadLock(() -> before.lerp(current, progress, cache));
            return MathUtil.fma(
                    interpolated.position()
                        .add(itemStack.offset())
                        .add(position.localOffset())
                        .rotate(interpolated.rotation()),
                    interpolated.scale(),
                    position.globalOffset()
                )
                .add(root.getGroup().getPosition())
                .mul(scale.getAsFloat())
                .rotateX(-rotation.radianX())
                .rotateY(-rotation.radianY());
        }

        private @NotNull Vector3f worldRotation() {
            var progress = progress();
            return lock.accessToReadLock(() -> InterpolationUtil.lerp(before.rawRotation(), current.rawRotation(), progress));
        }
    }

    public @NotNull BoneName name() {
        return getGroup().name();
    }

    public @NotNull UUID uuid() {
        return getGroup().uuid();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof RenderedBone bone)) return false;
        return uuid().equals(bone.uuid());
    }

    @Override
    public int hashCode() {
        return uuid().hashCode();
    }

    @Override
    public String toString() {
        return name().toString();
    }
}

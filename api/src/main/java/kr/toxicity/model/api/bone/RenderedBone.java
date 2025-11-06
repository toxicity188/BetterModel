/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bone;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.BlueprintElement;
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.util.*;
import kr.toxicity.model.api.util.function.BonePredicate;
import kr.toxicity.model.api.util.function.FloatConstantSupplier;
import kr.toxicity.model.api.util.function.FloatSupplier;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A rendered item-display.
 */
public final class RenderedBone implements BoneEventHandler {

    private static final int INITIAL_TINT_VALUE = 0xFFFFFF;
    private static final Vector3f EMPTY_VECTOR = new Vector3f();
    private static final int MAX_IK_ITERATION = 20;

    @Getter
    @NotNull
    private final RendererGroup group;
    private final BoneMovement defaultFrame;
    private final RenderSource<?> renderSource;
    private final BoneEventDispatcher eventDispatcher = new BoneEventDispatcher();

    @NotNull
    @Getter
    private final RenderedBone root;
    @Nullable
    @Getter
    private final RenderedBone parent;

    private RenderedBone locator;

    @Getter
    @NotNull
    private final Map<BoneName, RenderedBone> children;

    private final Int2ObjectMap<ItemStack> tintCacheMap = new Int2ObjectOpenHashMap<>();
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
     * @param renderSource render source
     * @param movement spawn movement
     * @param childrenMapper mapper
     */
    @ApiStatus.Internal
    public RenderedBone(
            @NotNull RendererGroup group,
            @Nullable RenderedBone parent,
            @NotNull RenderSource<?> renderSource,
            @NotNull BoneMovement movement,
            @NotNull Function<RenderedBone, Map<BoneName, RenderedBone>> childrenMapper
    ) {
        this.group = group;
        this.parent = parent;
        this.renderSource = renderSource;
        itemMapper = group.getItemMapper();
        root = parent != null ? parent.root : this;
        this.itemStack = itemMapper.apply(renderSource, group.getItemStack());
        this.dummyBone = group.getItemStack().isAir() && itemMapper == BoneItemMapper.EMPTY;
        defaultFrame = movement;
        children = childrenMapper.apply(this);
        if (!dummyBone) {
            display = BetterModel.nms().create(renderSource.location(), renderSource instanceof RenderSource.Entity ? -4096 : 0, d -> {
                d.display(itemMapper.transform());
                d.invisible(!group.getParent().visibility());
                d.viewRange(EntityUtil.ENTITY_MODEL_VIEW_RADIUS);
                applyItem(d);
            });
        } else display = null;
        globalState = new BoneStateHandler(null, uuid -> {});
    }

    public void locator(@NotNull Map<BoneName, RenderedBone> boneMap) {
        if (getGroup().getParent() instanceof BlueprintElement.BlueprintNullObject nullObject) {
            var get = boneMap.get(nullObject.ikTarget());
            if (get != null) get.locator(this);
        }
    }

    private void locator(@NotNull RenderedBone bone) {
        locator = bone;
        if (parent != null) parent.locator(bone);
    }

    private @NotNull BoneStateHandler state(@Nullable Player player) {
        return state(player != null ? player.getUniqueId() : null);
    }

    private @NotNull BoneStateHandler state(@Nullable UUID uuid) {
        return uuid == null ? globalState : perPlayerState.getOrDefault(uuid, globalState);
    }

    private @NotNull BoneStateHandler getOrCreateState(@Nullable Player player) {
        return getOrCreateState(player != null ? player.getUniqueId() : null);
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
        return itemStack(predicate, itemMapper.apply(renderSource, itemStack));
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
                if (h == null) h = ModelBoundingBox.MIN.named(name());
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
        return itemStack(predicate, itemStack.modify(i -> {
            var meta = i.getItemMeta();
            if (meta == null) return i;
            meta.setEnchantmentGlintOverride(enchant);
            i.setItemMeta(meta);
            return i;
        }));
    }

    /**
     * Sets bone's move duration.
     * @param predicate predicate
     * @param duration duration
     * @return success or not
     */
    public boolean moveDuration(@NotNull Predicate<RenderedBone> predicate, int duration) {
        if (display != null && predicate.test(this)) {
            display.moveDuration(duration);
            return true;
        }
        return false;
    }

    /**
     * Sets the scale of this bone
     * @param scale scale
     */
    public void scale(@NotNull FloatSupplier scale) {
        this.scale = scale;
    }

    /**
     * Apply a glow to this model.
     * @param glow should glow
     * @return success or not
     */
    public boolean glow(@NotNull Predicate<RenderedBone> predicate, boolean glow) {
        if (display != null && predicate.test(this)) {
            display.glow(glow);
            return true;
        }
        return false;
    }
    /**
     * Sets a view range of this model.
     * @param viewRange view range
     * @return success or not
     */
    public boolean viewRange(@NotNull Predicate<RenderedBone> predicate, float viewRange) {
        if (display != null && predicate.test(this)) {
            display.viewRange(viewRange);
            return true;
        }
        return false;
    }
    /**
     * Sets a glow color to this model.
     * @param glowColor hex glow color
     * @return success or not
     */
    public boolean glowColor(@NotNull Predicate<RenderedBone> predicate, int glowColor) {
        if (display != null && predicate.test(this)) {
            display.glowColor(glowColor);
            return true;
        }
        return false;
    }

    /**
     * Apply a billboard to this model.
     * @param billboard billboard
     * @return success or not
     */
    public boolean billboard(@NotNull Predicate<RenderedBone> predicate, @NotNull Display.Billboard billboard) {
        if (display != null && predicate.test(this)) {
            display.billboard(billboard);
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

    public boolean brightness(@NotNull Predicate<RenderedBone> predicate, int block, int sky) {
        if (display != null && predicate.test(this)) {
            display.brightness(block, sky);
            return true;
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

    public void sendTransformation(@Nullable UUID uuid, @NotNull PacketBundler bundler) {
        state(uuid).sendTransformation(bundler);
    }

    public void forceTransformation(@NotNull PacketBundler bundler) {
        var d = globalState.transformer;
        if (d != null) d.sendTransformation(bundler);
    }

    public void applyLocator(@Nullable UUID uuid) {
        if (locator != null) fabrik(
                flatten().filter(b -> b.locator == locator)
                        .map(b -> b.state(uuid))
                        .toList(),
                locator.root.group.getPosition().add(locator.state(uuid).after().position(), new Vector3f())
                        .sub(root.group.getPosition())
        );
    }

    public int interpolationDuration() {
        return globalState.interpolationDuration();
    }

    public @NotNull Vector3f worldPosition() {
        return worldPosition(EMPTY_VECTOR);
    }

    public @NotNull Vector3f worldPosition(@NotNull Vector3f localOffset) {
        return worldPosition(localOffset, EMPTY_VECTOR);
    }

    public @NotNull Vector3f worldPosition(@NotNull Vector3f localOffset, @NotNull Vector3f globalOffset) {
        return worldPosition(localOffset, globalOffset, null);
    }

    public @NotNull Vector3f worldPosition(@NotNull Vector3f localOffset, @NotNull Vector3f globalOffset, @Nullable UUID uuid) {
        var state = state(uuid);
        var progress = state.progress();
        var after = state.after();
        var before = state.before();
        return MathUtil.fma(
                        InterpolationUtil.lerp(before.position(), after.position(), progress)
                                .add(itemStack.offset())
                                .add(localOffset)
                                .rotate(
                                        MathUtil.toQuaternion(InterpolationUtil.lerp(before.rawRotation(), after.rawRotation(), progress))
                                ),
                        InterpolationUtil.lerp(before.scale(), after.scale(), progress),
                        globalOffset

                )
                .add(root.getGroup().getPosition())
                .mul(scale.getAsFloat())
                .rotateX(-rotation.radianX())
                .rotateY(-rotation.radianY());
    }

    public @NotNull Vector3f worldRotation() {
        return worldRotation(null);
    }

    public @NotNull Vector3f worldRotation(@Nullable UUID uuid) {
        var state = state(uuid);
        var progress = state.progress();
        var after = state.after();
        var before = state.before();
        return InterpolationUtil.lerp(before.rawRotation(), after.rawRotation(), progress);
    }

    public void defaultPosition(@NotNull Supplier<Vector3f> movement) {
        defaultPosition = movement;
    }

    private @NotNull Vector3f modifiedPosition(boolean preventModifierUpdate) {
        return preventModifierUpdate ? lastModifiedPosition : (lastModifiedPosition = positionModifier.apply(new Vector3f()));
    }

    private @NotNull Quaternionf modifiedRotation(boolean preventModifierUpdate) {
        return preventModifierUpdate ? lastModifiedRotation : (lastModifiedRotation = rotationModifier.apply(new Quaternionf()));
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

    public @NotNull BoneName name() {
        return getGroup().name();
    }

    public void teleport(@NotNull Location location, @NotNull PacketBundler bundler) {
        if (display != null) display.teleport(location, bundler);
    }

    public void spawn(boolean hide, @NotNull PacketBundler bundler) {
        if (display != null) display.spawn(!hide && !display.invisible(), bundler);
        var transformer = globalState.transformer;
        if (transformer != null) transformer.sendTransformation(bundler);
    }

    public boolean addAnimation(@NotNull AnimationPredicate filter, @NotNull BlueprintAnimation animator, @NotNull AnimationModifier modifier, @NotNull AnimationEventHandler eventHandler) {
        if (filter.test(this)) {
            var get = animator.animator().get(name());
            if (get == null && modifier.override(animator.override()) && !filter.isChildren()) return false;
            var type = modifier.type(animator.loop());
            var iterator = get != null ? get.iterator(type) : animator.emptyIterator(type);
            getOrCreateState(modifier.player()).state.addAnimation(animator.name(), iterator, modifier, eventHandler);
            return true;
        }
        return false;
    }

    public boolean replaceAnimation(@NotNull AnimationPredicate filter, @NotNull String target, @NotNull BlueprintAnimation animator, @NotNull AnimationModifier modifier) {
        if (filter.test(this)) {
            var get = animator.animator().get(name());
            if (get == null && modifier.override(animator.override()) && !filter.isChildren()) return false;
            var type = modifier.type(animator.loop());
            var iterator = get != null ? get.iterator(type) : animator.emptyIterator(type);
            state(modifier.player()).state.replaceAnimation(target, iterator, modifier);
            return true;
        }
        return false;
    }

    /**
     * Stops bone's animation
     * @param filter filter
     * @param name animation's name
     * @param player player
     */
    public boolean stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String name, @Nullable Player player) {
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

    /**
     * Toggles some part
     * @param predicate predicate
     * @param toggle toggle
     * @return success
     */
    public boolean togglePart(@NotNull Predicate<RenderedBone> predicate, boolean toggle) {
        if (display != null && predicate.test(this)) {
            display.invisible(!toggle);
            return true;
        }
        return false;
    }

    public @NotNull Stream<RenderedBone> flatten() {
        return Stream.concat(
                Stream.of(this),
                children.values().stream().flatMap(RenderedBone::flatten)
        );
    }

    public void iterateTree(@NotNull Consumer<RenderedBone> boneConsumer) {
        boneConsumer.accept(this);
        for (RenderedBone value : children.values()) {
            value.iterateTree(boneConsumer);
        }
    }

    public boolean matchTree(@NotNull Predicate<RenderedBone> bonePredicate) {
        var result = bonePredicate.test(this);
        for (RenderedBone value : children.values()) {
            if (value.matchTree(bonePredicate)) result = true;
        }
        return result;
    }

    public boolean matchTree(@NotNull BonePredicate predicate, @NotNull BiPredicate<RenderedBone, BonePredicate> mapper) {
        var parentResult = mapper.test(this, predicate);
        var childPredicate = predicate.children(parentResult);
        for (RenderedBone value : children.values()) {
            if (value.matchTree(childPredicate, mapper)) parentResult = true;
        }
        return parentResult;
    }

    public boolean matchTree(@NotNull AnimationPredicate predicate, @NotNull BiPredicate<RenderedBone, AnimationPredicate> mapper) {
        var parentResult = mapper.test(this, predicate);
        var childPredicate = predicate;
        if (parentResult) childPredicate = childPredicate.children();
        for (RenderedBone value : children.values()) {
            if (value.matchTree(childPredicate, mapper)) parentResult = true;
        }
        return parentResult;
    }

    @NotNull
    public Vector3f hitBoxPosition() {
        var box = getGroup().getHitBox();
        if (box != null) return worldPosition(box.centerPoint());
        return worldPosition();
    }

    @NotNull
    public Quaternionf hitBoxViewRotation() {
        return MathUtil.toQuaternion(worldRotation())
                .rotateLocalX(-rotation.radianX())
                .rotateLocalY(-rotation.radianY());
    }

    public float hitBoxScale() {
        return scale.getAsFloat();
    }

    @NotNull
    public ModelRotation rotation() {
        return rotation;
    }

    private final class BoneStateHandler {
        private boolean firstTick = true;
        private boolean skipInterpolation = false;
        private boolean sent;
        private final @Nullable UUID uuid;
        private final Consumer<UUID> consumer;
        private final AnimationStateHandler<AnimationMovement> state;
        private volatile BoneMovement beforeTransform, afterTransform, relativeOffsetCache;
        private final DisplayTransformer transformer = display != null ? display.createTransformer() : null;

        private BoneStateHandler(@Nullable UUID uuid, @NotNull Consumer<UUID> consumer) {
            this.uuid = uuid;
            this.consumer = consumer;
            state = new AnimationStateHandler<>(
                    AnimationMovement.EMPTY,
                    (b, a) -> {
                        skipInterpolation = false;
                        relativeOffsetCache = null;
                        if (a != null && a.skipInterpolation()) root.state(uuid).skipInterpolation = true;
                    }
            );
        }

        private @NotNull BoneMovement before() {
            return beforeTransform != null ? beforeTransform : defaultFrame;
        }

        private @NotNull BoneMovement after() {
            return afterTransform != null ? afterTransform : relativeOffset();
        }

        public boolean tick() {
            var result = state.tick(() -> {
                if (uuid != null) {
                    perPlayerState.remove(uuid);
                    consumer.accept(uuid);
                }
            }) || firstTick;
            if (result) {
                beforeTransform = afterTransform;
                afterTransform = relativeOffset();
                sent = false;
            }
            firstTick = false;
            return result;
        }

        public float progress() {
            return 1F - state.progress();
        }

        public int interpolationDuration() {
            if (root.state(uuid).skipInterpolation) return 0;
            var frame = state.frame() / (float) Tracker.MINECRAFT_TICK_MULTIPLIER;
            return Math.round(frame + MathUtil.FLOAT_COMPARISON_EPSILON);
        }

        private @NotNull BoneMovement relativeOffset() {
            if (relativeOffsetCache != null) return relativeOffsetCache;
            var keyframe = state.afterKeyframe();
            if (keyframe == null) keyframe = AnimationMovement.EMPTY;
            var def = defaultFrame.plus(keyframe);
            var preventModifierUpdate = interpolationDuration() < 1;
            if (parent != null) {
                var p = parent.state(uuid).relativeOffset();
                return relativeOffsetCache = new BoneMovement(
                        MathUtil.fma(
                                        def.position().rotate(p.rotation()),
                                        p.scale(),
                                        p.position()
                                ).sub(parent.lastModifiedPosition)
                                .add(modifiedPosition(preventModifierUpdate)),
                        def.scale().mul(p.scale()),
                        (keyframe.globalRotation() ? new Quaternionf() : p.rotation().div(parent.lastModifiedRotation, new Quaternionf()))
                                .mul(def.rotation())
                                .mul(modifiedRotation(preventModifierUpdate)),
                        def.rawRotation()
                );
            }
            return relativeOffsetCache = new BoneMovement(
                    def.position().add(modifiedPosition(preventModifierUpdate)),
                    def.scale(),
                    def.rotation().mul(modifiedRotation(preventModifierUpdate)),
                    def.rawRotation()
            );
        }

        private void sendTransformation(@NotNull PacketBundler bundler) {
            if (sent || transformer == null) return;
            sent = true;
            var boneMovement = after();
            var mul = scale.getAsFloat();
            transformer.transform(
                    interpolationDuration(),
                    MathUtil.fma(
                            itemStack.offset().rotate(boneMovement.rotation(), new Vector3f())
                                    .add(boneMovement.position())
                                    .add(root.group.getPosition()),
                            mul,
                            itemStack.position()
                    ).add(defaultPosition.get()),
                    boneMovement.scale()
                            .mul(itemStack.scale(), new Vector3f())
                            .mul(mul)
                            .max(EMPTY_VECTOR),
                    boneMovement.rotation(),
                    bundler
            );
        }
    }

    private static void fabrik(@NotNull List<BoneStateHandler> bones, @NotNull Vector3f target) {
        if (bones.size() < 2) return;

        var first = bones.getFirst().after().position();
        var last = bones.getLast().after().position();

        var rootPos = new Vector3f(first);

        float[] lengths = new float[bones.size() - 1];
        for (int i = 0; i < bones.size() - 1; i++) {
            lengths[i] = bones.get(i).after().position()
                    .distance(bones.get(i + 1).after().position());
        }
        for (int iter = 0; iter < MAX_IK_ITERATION; iter++) {
            // Forward
            last.set(target);
            for (int i = bones.size() - 2; i >= 0; i--) {
                var current = bones.get(i).after().position();
                var next = bones.get(i + 1).after().position();
                current.set(InterpolationUtil.lerp(next, current, lengths[i] / current.distance(next)));
            }
            // Backward
            first.set(rootPos);
            for (int i = 0; i < bones.size() - 1; i++) {
                var current = bones.get(i).after().position();
                var next = bones.get(i + 1).after().position();
                next.set(InterpolationUtil.lerp(current, next, lengths[i] / current.distance(next)));
            }
            // Check
            if (last.distance(target) < MathUtil.FRAME_EPSILON) break;
        }
        for (int i = 0; i < bones.size() - 1; i++) {
            var current = bones.get(i).after();
            var next = bones.get(i + 1).after();

            var dir = next.position().sub(current.position(), new Vector3f());
            current.rotation().set(MathUtil.fromToRotation(dir).mul(current.rotation()));
        }
    }
}

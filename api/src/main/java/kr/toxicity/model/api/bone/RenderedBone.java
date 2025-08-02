package kr.toxicity.model.api.bone;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.InterpolationUtil;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import kr.toxicity.model.api.util.function.FloatConstantSupplier;
import kr.toxicity.model.api.util.function.FloatSupplier;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A rendered item-display.
 */
public final class RenderedBone {

    private static final Vector3f EMPTY_VECTOR = new Vector3f();
    private static final Consumer<PacketBundler> EMPTY_TICKER = b -> {};
    private static final ItemStack AIR = new ItemStack(Material.AIR);

    @Getter
    @NotNull
    private final RendererGroup group;
    private final BoneMovement defaultFrame;

    @NotNull
    @Getter
    private final RenderedBone root;
    @Nullable
    @Getter
    private final RenderedBone parent;

    @Getter
    @NotNull
    private final Map<BoneName, RenderedBone> children;

    private final AnimationStateHandler<AnimationMovement> state = new AnimationStateHandler<>(
            AnimationMovement.EMPTY,
            (a, s, t) -> a.time(t),
            t -> relativeOffsetCache = null
    );
    private final Int2ObjectMap<ItemStack> tintCacheMap = new Int2ObjectOpenHashMap<>();
    @Getter
    private final boolean dummyBone;
    private final Object itemLock = new Object();

    //Resource
    @Getter
    @Nullable
    private ModelDisplay display;
    @Getter
    @Nullable
    private HitBox hitBox;

    //Item
    @Getter
    @Setter
    private BoneItemMapper itemMapper;
    private volatile int previousTint, tint = 0xFFFFFF;
    private volatile TransformedItemStack itemStack;

    //Animation
    private boolean firstTick = true;
    private boolean beforeVisible = true;
    private Consumer<PacketBundler> nextTicker = EMPTY_TICKER;
    private volatile BoneMovement beforeTransform, afterTransform, relativeOffsetCache;
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
     * @param source source
     * @param movement spawn movement
     * @param childrenMapper mapper
     */
    @ApiStatus.Internal
    public RenderedBone(
            @NotNull RendererGroup group,
            @Nullable RenderedBone parent,
            @NotNull RenderSource<?> source,
            @NotNull BoneMovement movement,
            @NotNull TrackerModifier modifier,
            @NotNull Function<RenderedBone, Map<BoneName, RenderedBone>> childrenMapper
    ) {
        this.group = group;
        this.parent = parent;
        itemMapper = group.getItemMapper();
        root = parent != null ? parent.root : this;
        this.itemStack = itemMapper.apply(source, group.getItemStack());
        this.dummyBone = group.getItemStack().isAir() && itemMapper == BoneItemMapper.EMPTY;
        defaultFrame = movement;
        children = childrenMapper.apply(this);
        if (!dummyBone) {
            display = BetterModel.plugin().nms().create(source.location(), source instanceof RenderSource.Entity ? -4096 : 0, d -> {
                d.display(itemMapper.transform());
                d.viewRange(modifier.viewRange());
                d.invisible(!group.getParent().visibility());
                applyItem(d);
            });
        }
    }

    public @Nullable RunningAnimation runningAnimation() {
        return state.runningAnimation();
    }

    public boolean updateItem(@NotNull Predicate<RenderedBone> predicate, @NotNull RenderSource<?> source) {
        return itemStack(predicate, itemMapper.apply(source, itemStack));
    }

    /**
     * Creates hit box.
     * @param entity target entity
     * @param predicate predicate
     * @param listener hit box listener
     */
    public boolean createHitBox(@NotNull EntityAdapter entity, @NotNull Predicate<RenderedBone> predicate, @Nullable HitBoxListener listener) {
        if (predicate.test(this)) {
            var previous = hitBox;
            synchronized (this) {
                if (previous != hitBox) return false;
                var h = group.getHitBox();
                if (h == null) h = ModelBoundingBox.MIN.named(group.getName());
                var l = listener;
                if (hitBox != null) {
                    hitBox.removeHitBox();
                    if (l == null) l = hitBox.listener();
                }
                hitBox = BetterModel.plugin().nms().createHitBox(entity, this, h, group.getMountController(), l != null ? l : HitBoxListener.EMPTY);
                return hitBox != null;
            }
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
            if (enchant) {
                meta.addEnchant(Enchantment.UNBREAKING, 0, true);
            } else {
                meta.removeEnchant(Enchantment.UNBREAKING);
            }
            i.setItemMeta(meta);
            return i;
        }));
    }

    /**
     * Sets bone's move duration.
     * @param duration duration
     */
    public void moveDuration(int duration) {
        if (display != null) display.moveDuration(duration);
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

    public boolean tick(@NotNull PacketBundler bundler) {
        nextTicker.accept(bundler);
        if (state.tick() || firstTick) {
            beforeTransform = afterTransform;
            var boneMovement = afterTransform = relativeOffset();
            var d = display;
            firstTick = false;
            if (d == null) return true;
            var afterVisible = isVisible();
            var nowVisible = afterVisible && !beforeVisible;
            if (nowVisible) {
                sendTransformation(d, 0, bundler);
                nextTicker = b -> {
                    nextTicker = EMPTY_TICKER;
                    sendTransformation(d, toInterpolationDuration(frame() - 1), b);
                };
            }
            setup(d, boneMovement);
            if (!nowVisible && (afterVisible || beforeVisible)) {
                sendTransformation(d, toInterpolationDuration(frame()), bundler);
            }
            beforeVisible = afterVisible;
            return true;
        }
        return false;
    }

    public boolean isVisible() {
        return afterTransform != null && afterTransform.isVisible();
    }

    public void forceUpdate(@NotNull PacketBundler bundler) {
        var d = display;
        if (d != null) d.sendEntityData(bundler);
    }

    public void forceUpdate(boolean showItem, @NotNull PacketBundler bundler) {
        var d = display;
        if (d != null) d.sendEntityData(showItem, bundler);
    }

    private static int toInterpolationDuration(float delay) {
        return delay <= 0 ? 0 : Math.max(Math.round(delay / (float) Tracker.MINECRAFT_TICK_MULTIPLIER), 1);
    }

    public @NotNull Vector3f worldPosition() {
        return worldPosition(EMPTY_VECTOR);
    }

    public @NotNull Vector3f worldPosition(@NotNull Vector3f localOffset) {
        return worldPosition(localOffset, EMPTY_VECTOR);
    }

    public @NotNull Vector3f worldPosition(@NotNull Vector3f localOffset, @NotNull Vector3f globalOffset) {
        var progress = 1F - state.progress();
        var after = afterTransform != null ? afterTransform : relativeOffset();
        var before = beforeTransform != null ? beforeTransform : BoneMovement.EMPTY;
        return MathUtil.fma(
                        InterpolationUtil.lerp(before.transform(), after.transform(), progress)
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

    public @NotNull Quaternionf worldRotation() {
        var progress = 1F - state.progress();
        var after = afterTransform != null ? afterTransform : relativeOffset();
        var before = beforeTransform != null ? beforeTransform : BoneMovement.EMPTY;
        return MathUtil.toQuaternion(InterpolationUtil.lerp(before.rawRotation(), after.rawRotation(), progress))
                .rotateLocalX(-rotation.radianX())
                .rotateLocalY(-rotation.radianY());
    }

    private static void sendTransformation(@NotNull ModelDisplay display, int duration, @NotNull PacketBundler bundler) {
        display.frame(duration);
        display.sendTransformation(bundler);
    }

    private void setup(@NotNull ModelDisplay display, @NotNull BoneMovement boneMovement) {
        var mul = scale.getAsFloat();
        display.transform(
                MathUtil.fma(
                        itemStack.offset().rotate(boneMovement.rotation(), new Vector3f())
                                .add(boneMovement.transform())
                                .add(root.group.getPosition()),
                        mul,
                        itemStack.position()
                ).add(defaultPosition.get()),
                boneMovement.scale()
                        .mul(itemStack.scale(), new Vector3f())
                        .mul(mul)
                        .max(EMPTY_VECTOR),
                boneMovement.rotation()
        );
    }

    public void defaultPosition(@NotNull Supplier<Vector3f> movement) {
        defaultPosition = movement;
    }

    private float frame() {
        var frame = state.frame();
        return frame == 0 && parent != null ? parent.frame() : frame;
    }

    private @NotNull BoneMovement defaultFrame() {
        var keyframe = state.getKeyframe();
        return defaultFrame.plus(keyframe != null ? keyframe : AnimationMovement.EMPTY);
    }

    private @NotNull BoneMovement relativeOffset() {
        if (relativeOffsetCache != null) return relativeOffsetCache;
        var def = defaultFrame();
        var preventModifierUpdate = toInterpolationDuration(frame()) < 1;
        if (parent != null) {
            var p = parent.relativeOffset();
            return relativeOffsetCache = new BoneMovement(
                    MathUtil.fma(
                            def.transform().rotate(p.rotation()),
                            p.scale(),
                            p.transform()
                    ).sub(parent.lastModifiedPosition)
                            .add(modifiedPosition(preventModifierUpdate)),
                    def.scale().mul(p.scale()),
                    p.rotation().div(parent.lastModifiedRotation, new Quaternionf())
                            .mul(def.rotation())
                            .mul(modifiedRotation(preventModifierUpdate)),
                    def.rawRotation()
            );
        }
        return relativeOffsetCache = new BoneMovement(
                def.transform().add(modifiedPosition(preventModifierUpdate)),
                def.scale(),
                def.rotation().mul(modifiedRotation(preventModifierUpdate)),
                def.rawRotation()
        );
    }

    private @NotNull Vector3f modifiedPosition(boolean preventModifierUpdate) {
        return preventModifierUpdate ? lastModifiedPosition : (lastModifiedPosition = positionModifier.apply(new Vector3f()));
    }

    private @NotNull Quaternionf modifiedRotation(boolean preventModifierUpdate) {
        return preventModifierUpdate ? lastModifiedRotation : (lastModifiedRotation = rotationModifier.apply(new Quaternionf()));
    }

    public boolean tint(@NotNull Predicate<RenderedBone> predicate, int tint) {
        if (tint == -1) tint = previousTint;
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
        targetDisplay.item(itemStack.isAir() ? AIR : tintCacheMap.computeIfAbsent(tint, i -> BetterModel.plugin().nms().tint(itemStack.itemStack(), i)));
    }

    public @NotNull BoneName getName() {
        return getGroup().getName();
    }


    public void teleport(@NotNull Location location, @NotNull PacketBundler bundler) {
        if (display != null) display.teleport(location, bundler);
    }

    public void spawn(boolean hide, @NotNull PacketBundler bundler) {
        if (display != null) display.spawn(!hide && !display.invisible(), bundler);
    }

    public boolean addAnimation(@NotNull AnimationPredicate filter, @NotNull BlueprintAnimation animator, @NotNull AnimationModifier modifier, Runnable removeTask) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            if (get == null && modifier.override(animator.override()) && !filter.isChildren()) return false;
            var type = modifier.type(animator.loop());
            var iterator = get != null ? get.iterator(type) : animator.emptyIterator(type);
            state.addAnimation(animator.name(), iterator, modifier, removeTask);
            return true;
        }
        return false;
    }

    public boolean replaceAnimation(@NotNull AnimationPredicate filter, @NotNull String target, @NotNull BlueprintAnimation animator, @NotNull AnimationModifier modifier) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            if (get == null && modifier.override(animator.override()) && !filter.isChildren()) return false;
            var type = modifier.type(animator.loop());
            var iterator = get != null ? get.iterator(type) : animator.emptyIterator(type);
            state.replaceAnimation(target, iterator, modifier);
            return true;
        }
        return false;
    }

    /**
     * Stops bone's animation
     * @param filter filter
     * @param name animation's name
     */
    public boolean stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String name) {
        return filter.test(this) && state.stopAnimation(name);
    }

    /**
     * Removes model's display
     * @param bundler packet bundler
     */
    public void remove(@NotNull PacketBundler bundler) {
        if (display != null) display.remove(bundler);
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

    /**
     * Gets bone
     * @param predicate predicate
     * @return matched bone
     */
    public @Nullable RenderedBone boneOf(@NotNull Predicate<RenderedBone> predicate) {
        return findNotNullByTree(b -> predicate.test(b) ? b : null);
    }

    public @NotNull Stream<RenderedBone> flatten() {
        return Stream.concat(
                Stream.of(this),
                children.values().stream().flatMap(RenderedBone::flatten)
        );
    }

    public <T> @Nullable T findNotNullByTree(@NotNull Function<RenderedBone, T> mapper) {
        var value = mapper.apply(this);
        if (value != null) return value;
        for (RenderedBone renderedBone : children.values()) {
            var childValue = renderedBone.findNotNullByTree(mapper);
            if (childValue != null) return childValue;
        }
        return null;
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
        return worldRotation();
    }

    public float hitBoxScale() {
        return scale.getAsFloat();
    }

    @NotNull
    public ModelRotation hitBoxRotation() {
        return rotation;
    }
}

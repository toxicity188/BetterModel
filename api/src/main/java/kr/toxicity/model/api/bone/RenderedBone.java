package kr.toxicity.model.api.bone;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.AnimationPredicate;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.TrackerModifier;
import kr.toxicity.model.api.util.*;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/**
 * A rendered item-display.
 */
public final class RenderedBone implements HitBoxSource {

    private static final Vector3f EMPTY_VECTOR = new Vector3f();
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

    private final SequencedMap<String, TreeIterator> animators = new LinkedHashMap<>();
    private final Collection<TreeIterator> reversedView = animators.sequencedValues().reversed();
    private final Int2ObjectOpenHashMap<ItemStack> tintCacheMap = new Int2ObjectOpenHashMap<>();
    private final AtomicBoolean forceUpdateAnimation = new AtomicBoolean(true);
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
    private int tint;
    private TransformedItemStack cachedItem, itemStack;

    //Animation
    private AnimationMovement keyFrame = null;
    private volatile long delay = 0;
    private TreeIterator currentIterator = null;
    private BoneMovement beforeTransform, afterTransform, relativeOffsetCache;
    private ModelRotation rotation = ModelRotation.EMPTY;

    private Supplier<Vector3f> defaultPosition = FunctionUtil.asSupplier(new Vector3f());
    private Supplier<Float> scale = FunctionUtil.asSupplier(1F);

    private Function<Vector3f, Vector3f> positionModifier = p -> p;
    private Vector3f lastModifiedPosition = new Vector3f();
    private Function<Quaternionf, Quaternionf> rotationModifier = r -> r;
    private Quaternionf lastModifiedRotation = new Quaternionf();

    /**
     * Creates entity.
     * @param group group
     * @param parent parent entity
     * @param itemStack item
     * @param transform display transform
     * @param firstLocation spawn location
     * @param movement spawn movement
     * @param childrenMapper mapper
     */
    @ApiStatus.Internal
    public RenderedBone(
            @NotNull RendererGroup group,
            @Nullable RenderedBone parent,
            @NotNull TransformedItemStack itemStack,
            @NotNull ItemDisplay.ItemDisplayTransform transform,
            @NotNull Location firstLocation,
            @NotNull BoneMovement movement,
            @NotNull TrackerModifier modifier,
            @NotNull Function<RenderedBone, Map<BoneName, RenderedBone>> childrenMapper
    ) {
        this.group = group;
        this.parent = parent;
        itemMapper = group.getItemMapper();
        var r = this;
        while (r.getParent() != null) r = r.getParent();
        root = r;
        var visible = itemMapper != BoneItemMapper.EMPTY || group.getParent().visibility();
        this.cachedItem = itemStack;
        this.itemStack = visible ? itemStack : itemStack.asAir();
        this.dummyBone = ItemUtil.isEmpty(itemStack);
        if (!dummyBone) {
            display = BetterModel.plugin().nms().create(firstLocation);
            display.display(transform);
            display.viewRange(modifier.viewRange());
            display.item(this.itemStack.itemStack());
        }
        defaultFrame = movement;
        children = Collections.unmodifiableMap(childrenMapper.apply(this));
    }

    public @Nullable RunningAnimation runningAnimation() {
        var iterator = currentIterator;
        return iterator != null ? iterator.animation : null;
    }

    public boolean updateItem(@NotNull BonePredicate predicate, @NotNull RenderSource source) {
        return itemStack(predicate, itemMapper.apply(source, cachedItem));
    }

    /**
     * Creates hit box.
     * @param entity target entity
     * @param predicate predicate
     * @param listener hit box listener
     */
    public boolean createHitBox(@NotNull EntityAdapter entity, @NotNull Predicate<RenderedBone> predicate, @Nullable HitBoxListener listener) {
        if (predicate.test(this)) {
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
        return false;
    }

    /**
     * Make item has enchantment or not
     * @param predicate predicate
     * @param enchant should enchant
     * @return success or not
     */
    public boolean enchant(@NotNull BonePredicate predicate, boolean enchant) {
        if (predicate.test(this)) {
            synchronized (itemLock) {
                itemStack = itemStack.modify(i -> {
                    if (ItemUtil.isEmpty(i)) return i;
                    var meta = i.getItemMeta();
                    if (enchant) {
                        meta.addEnchant(Enchantment.UNBREAKING, 0, true);
                    } else {
                        meta.removeEnchant(Enchantment.UNBREAKING);
                    }
                    i.setItemMeta(meta);
                    return i;
                });
                return applyItem();
            }
        }
        return false;
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
    public void scale(@NotNull Supplier<Float> scale) {
        this.scale = scale;
    }

    /**
     * Apply a glow to this model.
     * @param glow should glow
     * @param glowColor hex glow color
     * @return success or not
     */
    public boolean glow(@NotNull BonePredicate predicate, boolean glow, int glowColor) {
        if (predicate.test(this) && display != null) {
            display.glow(glow);
            display.glowColor(glowColor);
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
    public boolean itemStack(@NotNull BonePredicate predicate, @NotNull TransformedItemStack itemStack) {
        if (predicate.test(this)) {
            synchronized (itemLock) {
                this.itemStack = cachedItem = itemStack;
                tintCacheMap.clear();
                return applyItem();
            }
        }
        return false;
    }

    public boolean brightness(@NotNull BonePredicate predicate, int block, int sky) {
        if (predicate.test(this) && display != null) {
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
    public synchronized boolean addRotationModifier(@NotNull BonePredicate predicate, @NotNull Function<Quaternionf, Quaternionf> function) {
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
    public synchronized boolean addPositionModifier(@NotNull BonePredicate predicate, @NotNull Function<Vector3f, Vector3f> function) {
        if (predicate.test(this)) {
            positionModifier = positionModifier.andThen(function);
            return true;
        }
        return false;
    }

    private boolean keyframeFinished() {
        return delay <= 0;
    }

    private boolean shouldUpdateAnimation() {
        return forceUpdateAnimation.compareAndSet(true, false) || keyframeFinished() || delay % 5 == 0;
    }

    private boolean updateAnimation() {
        synchronized (animators) {
            var iterator = reversedView.iterator();
            while (iterator.hasNext()) {
                var next = iterator.next();
                if (!next.get()) continue;
                if (currentIterator == null) {
                    if (updateKeyframe(iterator, next)) {
                        currentIterator = next;
                        return true;
                    }
                } else if (currentIterator != next) {
                    if (updateKeyframe(iterator, next)) {
                        currentIterator.clear();
                        currentIterator = next;
                        delay = 0;
                        return true;
                    }
                } else if (keyframeFinished()) {
                    if (updateKeyframe(iterator, next)) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        relativeOffsetCache = null;
        keyFrame = null;
        return true;
    }

    private boolean updateKeyframe(Iterator<TreeIterator> iterator, TreeIterator next) {
        if (!next.hasNext()) {
            next.run();
            iterator.remove();
            return false;
        } else {
            relativeOffsetCache = null;
            keyFrame = next.next();
            return true;
        }
    }

    public synchronized boolean move(@Nullable ModelRotation rotation, @NotNull PacketBundler bundler) {
        var d = display;
        if (rotation != null) {
            this.rotation = rotation;
            if (d != null) d.rotate(rotation, bundler);
        }
        --delay;
        if (shouldUpdateAnimation() && updateAnimation()) {
            var f = frame();
            delay = f;
            beforeTransform = afterTransform;
            var entityMovement = afterTransform = relativeOffset();
            if (d != null) {
                d.frame(toInterpolationDuration(f));
                setup(entityMovement);
                d.sendTransformation(bundler);
                return true;
            }
        }
        return false;
    }

    public void forceUpdate(@NotNull PacketBundler bundler) {
        var d = display;
        if (d != null) d.sendEntityData(bundler);
    }

    public void forceUpdate(boolean showItem, @NotNull PacketBundler bundler) {
        var d = display;
        if (d != null) d.sendEntityData(showItem, bundler);
    }

    private static int toInterpolationDuration(long delay) {
        return (int) Math.ceil((float) delay / 5F);
    }

    public @NotNull Vector3f worldPosition() {
        return worldPosition(EMPTY_VECTOR);
    }

    public @NotNull Vector3f worldPosition(@NotNull Vector3f localOffset) {
        return worldPosition(localOffset, EMPTY_VECTOR);
    }

    public @NotNull Vector3f worldPosition(@NotNull Vector3f localOffset, @NotNull Vector3f globalOffset) {
        var progress = 1F - progress();
        var after = afterTransform != null ? afterTransform : relativeOffset();
        var before = beforeTransform != null ? beforeTransform : BoneMovement.EMPTY;
        return VectorUtil.linear(before.transform(), after.transform(), progress)
                .add(itemStack.offset())
                .add(localOffset)
                .mul(VectorUtil.linear(before.scale(), after.scale(), progress))
                .rotate(
                        MathUtil.toQuaternion(VectorUtil.linear(before.rawRotation(), after.rawRotation(), progress))
                )
                .add(globalOffset)
                .add(root.getGroup().getPosition())
                .mul(scale.get())
                .rotateX(-rotation.radianX())
                .rotateY(-rotation.radianY());
    }

    public @NotNull Quaternionf worldRotation() {
        var progress = 1F - progress();
        var after = afterTransform != null ? afterTransform : relativeOffset();
        var before = beforeTransform != null ? beforeTransform : BoneMovement.EMPTY;
        return new Quaternionf()
                .rotateZYX(
                        0,
                        -rotation.radianY(),
                        -rotation.radianX()
                ).mul(MathUtil.toQuaternion(VectorUtil.linear(before.rawRotation(), after.rawRotation(), progress)));
    }

    private void setup(@NotNull BoneMovement boneMovement) {
        if (display != null) {
            var mul = scale.get();
            display.transform(
                    new Vector3f(boneMovement.transform())
                            .add(root.group.getPosition())
                            .add(new Vector3f(itemStack.offset()).rotate(boneMovement.rotation()))
                            .mul(mul)
                            .add(defaultPosition.get()),
                    new Vector3f(boneMovement.scale())
                            .mul(itemStack.scale())
                            .mul(mul),
                    boneMovement.rotation()
            );
        }
    }

    public void defaultPosition(@NotNull Supplier<Vector3f> movement) {
        defaultPosition = () -> new Vector3f(movement.get()).add(itemStack.position());
    }

    private int frame() {
        return keyFrame != null ? Math.round(keyFrame.time() * 100) : parent != null ? parent.frame() : 0;
    }

    private @NotNull BoneMovement defaultFrame() {
        return defaultFrame.plus(keyFrame != null ? keyFrame : new AnimationMovement(0, null, null, null));
    }

    private float progress() {
        var f = frame();
        return f == 0 ? 0F : delay / (float) f;
    }

    private @NotNull BoneMovement relativeOffset() {
        if (relativeOffsetCache != null) return relativeOffsetCache;
        var def = defaultFrame();
        if (parent != null) {
            var p = parent.relativeOffset();
            return relativeOffsetCache = new BoneMovement(
                    def.transform()
                            .mul(p.scale())
                            .rotate(p.rotation())
                            .add(p.transform())
                            .sub(parent.lastModifiedPosition)
                            .add(modifiedPosition()),
                    def.scale().mul(p.scale()),
                    new Quaternionf(p.rotation())
                            .div(parent.lastModifiedRotation)
                            .mul(def.rotation())
                            .mul(modifiedRotation()),
                    def.rawRotation()
            );
        }
        return relativeOffsetCache = new BoneMovement(
                def.transform().add(modifiedPosition()),
                def.scale(),
                def.rotation().mul(modifiedRotation()),
                def.rawRotation()
        );
    }

    private @NotNull Vector3f modifiedPosition() {
        return lastModifiedPosition = positionModifier.apply(new Vector3f());
    }

    private @NotNull Quaternionf modifiedRotation() {
        return lastModifiedRotation = rotationModifier.apply(new Quaternionf());
    }

    public boolean tint(@NotNull BonePredicate predicate, int tint) {
        if (predicate.test(this)) {
            synchronized (itemLock) {
                this.tint = tint;
                return applyItem();
            }
        }
        return false;
    }

    private boolean applyItem() {
        if (display != null) {
            display.item(itemStack.isAir() ? AIR : tintCacheMap.computeIfAbsent(tint, i -> BetterModel.plugin().nms().tint(itemStack.itemStack(), tint)));
            return true;
        } else return false;
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

    public boolean addAnimation(@NotNull AnimationPredicate filter, @NotNull String parent, @NotNull BlueprintAnimation animator, @NotNull AnimationModifier modifier, Runnable removeTask) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            if (get == null && animator.override() && !filter.isChildren()) return false;
            var type = modifier.type(animator.loop());
            var iterator = get != null ? new TreeIterator(parent, get.iterator(type), modifier, removeTask) : new TreeIterator(parent, animator.emptyIterator(type), modifier, removeTask);
            synchronized (animators) {
                animators.putLast(parent, iterator);
            }
            forceUpdateAnimation.set(true);
            return true;
        }
        return false;
    }

    public boolean replaceAnimation(@NotNull AnimationPredicate filter, @NotNull String target, @NotNull String parent, @NotNull BlueprintAnimation animator, @NotNull AnimationModifier modifier) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            if (get == null && animator.override() && !filter.isChildren()) return false;
            var type = modifier.type(animator.loop());
            synchronized (animators) {
                var v = animators.get(target);
                if (v != null) animators.replace(target, get != null ? new TreeIterator(parent, get.iterator(type), v.modifier, v.removeTask) : new TreeIterator(parent, animator.emptyIterator(type), v.modifier, v.removeTask));
                else animators.replace(target, get != null ? new TreeIterator(parent, get.iterator(type), modifier, () -> {
                }) : new TreeIterator(parent, animator.emptyIterator(type), modifier, () -> {
                }));
            }
            forceUpdateAnimation.set(true);
            return true;
        }
        return false;
    }

    /**
     * Stops bone's animation
     * @param filter filter
     * @param parent animation's name
     */
    public void stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String parent) {
        if (filter.test(this)) {
            synchronized (animators) {
                if (animators.remove(parent) != null) forceUpdateAnimation.set(true);
            }
        }
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
    public boolean togglePart(@NotNull BonePredicate predicate, boolean toggle) {
        if (predicate.test(this)) {
            synchronized (itemLock) {
                itemStack = toggle ? cachedItem : cachedItem.asAir();
                return applyItem();
            }
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

    public boolean iterateTree(@NotNull BonePredicate predicate, @NotNull BiPredicate<RenderedBone, BonePredicate> mapper) {
        var parentResult = mapper.test(this, predicate);
        var childPredicate = predicate.children(parentResult);
        for (RenderedBone value : children.values()) {
            if (value.iterateTree(childPredicate, mapper)) parentResult = true;
        }
        return parentResult;
    }

    public boolean iterateAnimation(@NotNull AnimationPredicate predicate, @NotNull BiPredicate<RenderedBone, AnimationPredicate> mapper) {
        var parentResult = mapper.test(this, predicate);
        var childPredicate = predicate;
        if (parentResult) childPredicate = childPredicate.children();
        for (RenderedBone value : children.values()) {
            if (value.iterateAnimation(childPredicate, mapper)) parentResult = true;
        }
        return parentResult;
    }

    public record RunningAnimation(@NotNull String name, @NotNull AnimationIterator.Type type) {}

    private class TreeIterator implements AnimationIterator, Supplier<Boolean>, Runnable {
        private final RunningAnimation animation;
        private final AnimationIterator iterator;
        private final AnimationModifier modifier;
        private final Runnable removeTask;

        private final AnimationMovement previous;

        private boolean started = false;
        private boolean ended = false;

        public TreeIterator(String name, AnimationIterator iterator, AnimationModifier modifier, Runnable removeTask) {
            animation = new RunningAnimation(name, iterator.type());
            this.iterator = iterator;
            this.modifier = modifier;
            this.removeTask = removeTask;

            previous = keyFrame != null ? keyFrame.time((float) modifier.end() / 20) : new AnimationMovement((float) modifier.end() / 20, null, null, null);
        }

        @NotNull
        @Override
        public AnimationMovement first() {
            return iterator.first();
        }

        @Override
        public int index() {
            return iterator.index();
        }

        @Override
        public int lastIndex() {
            return iterator.lastIndex();
        }

        @Override
        public void run() {
            removeTask.run();
        }

        @Override
        public Boolean get() {
            return modifier.predicate().get();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext() || (modifier.end() > 0 && !ended);
        }

        @Override
        public AnimationMovement next() {
            if (!started) {
                started = true;
                return first().time((float) modifier.start() / 20);
            }
            if (!iterator.hasNext()) {
                ended = true;
                return previous;
            }
            var nxt = iterator.next();
            nxt = nxt.time(nxt.time() / modifier.speedValue());
            return nxt;
        }

        @Override
        public void clear() {
            iterator.clear();
            started = ended = !iterator.hasNext();
        }

        @NotNull
        @Override
        public Type type() {
            return iterator.type();
        }
    }

    @NotNull
    @Override
    public Vector3f hitBoxPosition() {
        var box = getGroup().getHitBox();
        if (box != null) return worldPosition(box.centerPoint());
        return worldPosition();
    }

    @NotNull
    @Override
    public Quaternionf hitBoxViewRotation() {
        return worldRotation();
    }

    @Override
    public float hitBoxScale() {
        return scale.get();
    }

    @NotNull
    @Override
    public ModelRotation hitBoxRotation() {
        return rotation;
    }
}

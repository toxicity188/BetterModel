package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.TrackerModifier;
import kr.toxicity.model.api.util.*;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.*;

/**
 * A rendered item-display.
 */
public final class RenderedBone implements HitBoxSource {

    private static final Vector3f EMPTY_VECTOR = new Vector3f();

    @Getter
    @NotNull
    private final RendererGroup group;
    @Getter
    @Nullable
    private ModelDisplay display;
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
    private AnimationMovement keyFrame = null;
    private long delay = 0;
    private TransformedItemStack cachedItem, itemStack;

    private final List<Consumer<AnimationMovement>> movementModifier = new ArrayList<>();
    @Getter
    @Nullable
    private HitBox hitBox;

    @Getter
    private final boolean dummyBone;
    private int tint;
    private TreeIterator currentIterator = null;
    private BoneMovement beforeTransform, afterTransform, relativeOffsetCache;
    private Supplier<Vector3f> defaultPosition = FunctionUtil.asSupplier(new Vector3f());
    private ModelRotation rotation = ModelRotation.EMPTY;
    private Supplier<Float> scale = FunctionUtil.asSupplier(1F);

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
        var r = this;
        while (r.getParent() != null) r = r.getParent();
        root = r;
        var visible = group.getLimb() != null || group.getParent().visibility();
        this.cachedItem = itemStack;
        this.itemStack = visible ? itemStack : itemStack.asAir();
        this.dummyBone = ItemUtil.isEmpty(itemStack);
        if (!dummyBone) {
            display = BetterModel.inst().nms().create(firstLocation);
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
            hitBox = BetterModel.inst().nms().createHitBox(entity, this, h, group.getMountController(), l != null ? l : HitBoxListener.EMPTY);
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
        return false;
    }

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
            this.itemStack = cachedItem = itemStack;
            return applyItem();
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
     * Adds animation modifier.
     * @param predicate predicate
     * @param consumer animation consumer
     * @return whether to success
     */
    public boolean addAnimationMovementModifier(@NotNull BonePredicate predicate, @NotNull Consumer<AnimationMovement> consumer) {
        if (predicate.test(this)) {
            synchronized (movementModifier) {
                movementModifier.add(consumer);
            }
            return true;
        }
        return false;
    }

    private void updateAnimation() {
        synchronized (animators) {
            var iterator = reversedView.iterator();
            var check = true;
            while (iterator.hasNext()) {
                var next = iterator.next();
                if (!next.get()) continue;
                if (currentIterator == null) {
                    if (updateKeyframe(iterator, next)) {
                        currentIterator = next;
                        check = false;
                        break;
                    }
                } else if (currentIterator != next) {
                    if (updateKeyframe(iterator, next)) {
                        currentIterator.clear();
                        currentIterator = next;
                        delay = 0;
                        check = false;
                        break;
                    }
                } else if (delay <= 0) {
                    if (updateKeyframe(iterator, next)) {
                        check = false;
                        break;
                    }
                } else {
                    check = false;
                    break;
                }
            }
            if (check) {
                keyFrame = null;
            }
        }
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

    public boolean move(@Nullable ModelRotation rotation, @NotNull PacketBundler bundler) {
        var d = display;
        if (rotation != null) {
            this.rotation = rotation;
            if (d != null) d.rotate(rotation, bundler);
        }
        --delay;
        updateAnimation();
        if (delay <= 0) {
            var f = frame();
            delay = f;
            beforeTransform = afterTransform;
            var entityMovement = afterTransform = relativeOffset();
            if (d != null) {
                d.frame(f <= 0 ? 0 : toInterpolationDuration(f));
                setup(entityMovement);
                d.send(bundler);
                return true;
            }
        }
        return false;
    }

    public void forceUpdate(@NotNull PacketBundler bundler) {
        var d = display;
        if (d != null && delay > 0) {
            var speed = currentIterator != null ? currentIterator.deltaSpeed() : 1F;
            delay = Math.round((float) delay / speed);
            d.frame(toInterpolationDuration(delay));
            d.send(bundler);
        }
    }

    private static int toInterpolationDuration(long delay) {
        return (int) Math.floor((float) delay / 5F) + 1;
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
                        MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(VectorUtil.linear(before.rawRotation(), after.rawRotation(), progress)))
                )
                .add(globalOffset)
                .add(root.getGroup().getPosition())
                .mul(scale.get())
                .rotateX((float) -Math.toRadians(rotation.x()))
                .rotateY((float) -Math.toRadians(rotation.y()));
    }

    private void setup(@NotNull BoneMovement boneMovement) {
        if (display != null) {
            var mul = scale.get();
            display.transform(new Transformation(
                    new Vector3f(boneMovement.transform())
                            .add(root.group.getPosition())
                            .add(new Vector3f(itemStack.offset()).rotate(boneMovement.rotation()))
                            .mul(mul)
                            .add(defaultPosition.get()),
                    boneMovement.rotation(),
                    new Vector3f(boneMovement.scale())
                            .mul(itemStack.scale())
                            .mul(mul),
                    new Quaternionf()
            ));
        }
    }

    public void defaultPosition(@NotNull Supplier<Vector3f> movement) {
        defaultPosition = group.getLimb() != null ? () -> new Vector3f(movement.get()).add(group.getLimb().getPosition()) : movement;
    }

    private int frame() {
        return keyFrame != null ? Math.round(keyFrame.time() * 100) : parent != null ? parent.frame() : 5;
    }

    private BoneMovement defaultFrame() {
        var k = keyFrame != null ? keyFrame.copyNotNull() : new AnimationMovement(0, new Vector3f(), new Vector3f(), new Vector3f());
        synchronized (movementModifier) {
            for (Consumer<AnimationMovement> consumer : movementModifier) {
                consumer.accept(k);
            }
        }
        return defaultFrame.plus(k);
    }

    private float progress() {
        var f = frame();
        return f == 0 ? 0F : delay / (float) f;
    }

    private BoneMovement relativeOffset() {
        if (relativeOffsetCache != null) return relativeOffsetCache;
        var def = defaultFrame();
        if (parent != null) {
            var p = parent.relativeOffset();
            return relativeOffsetCache = new BoneMovement(
                    new Vector3f(p.transform()).add(new Vector3f(def.transform()).mul(p.scale()).rotate(p.rotation())),
                    new Vector3f(def.scale()).mul(p.scale()),
                    new Quaternionf(p.rotation()).mul(def.rotation()),
                    def.rawRotation()
            );
        }
        return relativeOffsetCache = def;
    }

    public boolean tint(@NotNull BonePredicate predicate, int tint) {
        if (predicate.test(this)) {
            this.tint = tint;
            return applyItem();
        }
        return false;
    }

    private boolean applyItem() {
        if (display != null) {
            display.item(BetterModel.inst().nms().tint(itemStack.itemStack().clone(), tint));
            return true;
        } else return false;
    }

    public @NotNull BoneName getName() {
        return getGroup().getName();
    }

    public void teleport(@NotNull Location location, @NotNull PacketBundler bundler) {
        if (display != null) display.teleport(location, bundler);
    }

    public void spawn(@NotNull PacketBundler bundler) {
        if (display != null) display.spawn(bundler);
    }

    public void addLoop(@NotNull Predicate<RenderedBone> filter, @NotNull String parent, @NotNull BlueprintAnimation animator, AnimationModifier modifier, Runnable removeTask) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            var iterator = get != null ? new TreeIterator(parent, get.loopIterator(), modifier, removeTask) : new TreeIterator(parent, animator.emptyLoopIterator(), modifier, removeTask);
            synchronized (animators) {
                animators.putLast(parent, iterator);
            }
        }
    }

    public void addSingle(@NotNull Predicate<RenderedBone> filter, @NotNull String parent, @NotNull BlueprintAnimation animator, AnimationModifier modifier, Runnable removeTask) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            var iterator = get != null ? new TreeIterator(parent, get.singleIterator(), modifier, removeTask) : new TreeIterator(parent, animator.emptySingleIterator(), modifier, removeTask);
            synchronized (animators) {
                animators.putLast(parent, iterator);
            }
        }
    }

    public void replaceLoop(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull String parent, @NotNull BlueprintAnimation animator) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            synchronized (animators) {
                var v = animators.get(target);
                if (v != null) animators.replace(target, get != null ? new TreeIterator(parent, get.loopIterator(), v.modifier, v.removeTask) : new TreeIterator(parent, animator.emptyLoopIterator(), v.modifier, v.removeTask));
                else animators.replace(target, get != null ? new TreeIterator(parent, get.loopIterator(), AnimationModifier.DEFAULT_LOOP, () -> {
                }) : new TreeIterator(parent, animator.emptyLoopIterator(), AnimationModifier.DEFAULT_LOOP, () -> {
                }));
            }
        }
    }

    public void replaceSingle(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull String parent, @NotNull BlueprintAnimation animator) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            synchronized (animators) {
                var v = animators.get(target);
                if (v != null) animators.replace(target, get != null ? new TreeIterator(parent, get.singleIterator(), v.modifier, v.removeTask) : new TreeIterator(parent, animator.emptySingleIterator(), v.modifier, v.removeTask));
                else animators.replace(target, get != null ? new TreeIterator(parent, get.singleIterator(), AnimationModifier.DEFAULT, () -> {
                }) : new TreeIterator(parent, animator.emptySingleIterator(), AnimationModifier.DEFAULT, () -> {
                }));
            }
        }
    }

    /**
     * Stops bone's animation
     * @param filter filter
     * @param parent animation's name
     */
    public void stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String parent) {
        if (filter.test(this)) {
            synchronized (animators) {
                animators.remove(parent);
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
            itemStack = toggle ? cachedItem : cachedItem.asAir();
            return applyItem();
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

    public record RunningAnimation(@NotNull String name, @NotNull AnimationIterator.Type type) {}

    private static class TreeIterator implements AnimationIterator, Supplier<Boolean>, Runnable {
        private final RunningAnimation animation;
        private final AnimationIterator iterator;
        private final AnimationModifier modifier;
        private final Runnable removeTask;

        private boolean started = false;
        private boolean ended = false;

        private float cachedSpeed = 1F;

        public TreeIterator(String name, AnimationIterator iterator, AnimationModifier modifier, Runnable removeTask) {
            animation = new RunningAnimation(name, iterator.type());
            this.iterator = iterator;
            this.modifier = modifier;
            this.removeTask = removeTask;
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

        public float deltaSpeed() {
            var previous = cachedSpeed;
            return (cachedSpeed = modifier.speedValue()) / previous;
        }

        @Override
        public AnimationMovement next() {
            if (!started) {
                started = true;
                return first().time((float) modifier.start() / 20);
            }
            if (!iterator.hasNext()) {
                ended = true;
                return new AnimationMovement((float) modifier.end() / 20, null, null, null);
            }
            var nxt = iterator.next();
            nxt = nxt.time(Math.max(nxt.time() / (cachedSpeed = modifier.speedValue()), 0.01F));
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
        if (box != null) return worldPosition(box.centerPoint().mul(-1F, 1F, -1F));
        return worldPosition();
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

    public void despawn() {
        iterateTree(b -> {
            if (b.hitBox != null) b.hitBox.removeHitBox();
        });
    }
}

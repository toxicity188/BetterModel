package kr.toxicity.model.api.bone;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.TrackerMovement;
import kr.toxicity.model.api.util.BonePredicate;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.VectorUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A rendered item-display.
 */
public final class RenderedBone implements HitBoxSource {

    @Getter
    private final RendererGroup group;
    @Getter
    private ModelDisplay display;
    private final BoneMovement defaultFrame;

    @Nullable
    private final RenderedBone parent;

    @Setter
    private Map<String, RenderedBone> children = Collections.emptyMap();

    private final SequencedMap<String, TreeIterator> animators = new LinkedHashMap<>();
    private final Collection<TreeIterator> reversedView = animators.sequencedValues().reversed();
    private AnimationMovement keyFrame = null;
    private long delay = 0;
    private TransformedItemStack itemStack, cachedStack;

    private final List<Consumer<AnimationMovement>> movementModifier = new ArrayList<>();
    @Getter
    private HitBox hitBox;

    private int tint;
    private TreeIterator currentIterator = null;
    private BoneMovement beforeTransform, afterTransform, relativeOffsetCache;
    private Vector3f defaultPosition = new Vector3f();
    private ModelRotation rotation = ModelRotation.EMPTY;

    /**
     * Creates entity.
     * @param group group
     * @param parent parent entity
     * @param itemStack item
     * @param transform display transform
     * @param firstLocation spawn location
     * @param movement spawn movement
     */
    public RenderedBone(
            @NotNull RendererGroup group,
            @Nullable RenderedBone parent,
            @NotNull TransformedItemStack itemStack,
            @NotNull ItemDisplay.ItemDisplayTransform transform,
            @NotNull Location firstLocation,
            @NotNull BoneMovement movement
    ) {
        this.group = group;
        this.parent = parent;
        var visible = group.getLimb() != null || group.getParent().visibility();
        this.itemStack = cachedStack = visible ? itemStack : TransformedItemStack.EMPTY;
        if (!itemStack.isEmpty()) {
            display = BetterModel.inst().nms().create(firstLocation);
            display.display(transform);
            if (visible) display.item(itemStack.itemStack());
        }
        defaultFrame = movement;
    }

    /**
     * Creates hit box.
     * @param entity target entity
     * @param predicate predicate
     * @param listener hit box listener
     */
    @ApiStatus.Internal
    public void createHitBox(@NotNull EntityAdapter entity, @NotNull Predicate<RenderedBone> predicate, @Nullable HitBoxListener listener) {
        var h = group.getHitBox();
        if (h != null && predicate.test(this)) {
            var l = listener;
            if (hitBox != null) {
                hitBox.removeHitBox();
                if (l == null) l = hitBox.listener();
            }
            hitBox = BetterModel.inst().nms().createHitBox(entity, this, h, group.getMountController(), l != null ? l : HitBoxListener.EMPTY);
        }
        forEachChildren(e -> e.createHitBox(entity, predicate, listener));
    }

    /**
     * Changes displayed item
     * @param predicate predicate
     * @param itemStack target item
     * @return success
     */
    @ApiStatus.Internal
    public boolean itemStack(@NotNull BonePredicate predicate, @NotNull TransformedItemStack itemStack) {
        var checked = false;
        if (predicate.test(this)) {
            this.itemStack = cachedStack = itemStack;
            checked = applyItem();
        }
        var children = predicate.children(checked);
        if (checkChildren(e -> e.itemStack(children, itemStack))) checked = true;
        return checked;
    }

    @ApiStatus.Internal
    public boolean brightness(@NotNull BonePredicate predicate, int block, int sky) {
        var checked = false;
        if (predicate.test(this) && display != null) {
            display.brightness(block, sky);
            checked = true;
        }
        var children = predicate.children(checked);
        if (checkChildren(e -> e.brightness(children, block, sky))) checked = true;
        return checked;
    }

    /**
     * Adds animation modifier.
     * @param predicate predicate
     * @param consumer animation consumer
     * @return whether to success
     */
    @ApiStatus.Internal
    public boolean addAnimationMovementModifier(@NotNull BonePredicate predicate, @NotNull Consumer<AnimationMovement> consumer) {
        var checked = false;
        if (predicate.test(this)) {
            synchronized (movementModifier) {
                movementModifier.add(consumer);
            }
            checked = true;
        }
        var children = predicate.children(checked);
        if (checkChildren(e -> e.addAnimationMovementModifier(children, consumer))) checked = true;
        return checked;
    }

    /**
     * Adds all renderers to list.
     * @param renderers target list
     */
    @ApiStatus.Internal
    public void renderers(List<RenderedBone> renderers) {
        renderers.add(this);
        forEachChildren(e -> e.renderers(renderers));
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

    @ApiStatus.Internal
    public void move(@Nullable ModelRotation rotation, @NotNull TrackerMovement movement, @NotNull PacketBundler bundler) {
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
            var entityMovement = (afterTransform = movement.plus(relativeOffset())).plus(defaultPosition);
            if (d != null) {
                d.frame(f <= 0 ? 0 : toInterpolationDuration(f));
                setup(entityMovement);
                d.send(bundler);
            }
        }
        forEachChildren(e -> e.move(rotation, movement, bundler));
    }

    @ApiStatus.Internal
    public void forceUpdate(@NotNull PacketBundler bundler) {
        forceUpdate0(bundler);
        forEachChildren(e -> e.forceUpdate(bundler));
    }

    private void forceUpdate0(@NotNull PacketBundler bundler) {
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
        return worldPosition(new Vector3f());
    }

    public @NotNull Vector3f worldPosition(@NotNull Vector3f offset) {
        if (afterTransform != null) {
            var progress = 1F - progress();
            var before = beforeTransform != null ? beforeTransform : BoneMovement.EMPTY;
            return VectorUtil.linear(before.transform(), afterTransform.transform(), progress)
                    .add(offset)
                    .add(itemStack.offset())
                    .mul(VectorUtil.linear(before.scale(), afterTransform.scale(), progress))
                    .rotate(
                            MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(VectorUtil.linear(before.rawRotation(), afterTransform.rawRotation(), progress)))
                    )
                    .add(defaultPosition)
                    .rotateX((float) -Math.toRadians(rotation.x()))
                    .rotateY((float) -Math.toRadians(rotation.y()));
        }
        return new Vector3f();
    }

    private void setup(@NotNull BoneMovement boneMovement) {
        if (display != null) {
            display.transform(new Transformation(
                    new Vector3f(boneMovement.transform()).add(new Vector3f(itemStack.offset()).rotate(boneMovement.rotation())),
                    boneMovement.rotation(),
                    new Vector3f(boneMovement.scale()).mul(itemStack.scale()),
                    new Quaternionf()
            ));
        }
    }

    @ApiStatus.Internal
    public void defaultPosition(@NotNull Vector3f movement) {
        defaultPosition = group.getLimb() != null ? new Vector3f(movement).add(group.getLimb().getPosition()) : movement;
        forEachChildren(e -> e.defaultPosition(movement));
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

    @ApiStatus.Internal
    public boolean tint(@NotNull BonePredicate predicate, int tint) {
        var checked = false;
        if (predicate.test(this)) {
            this.tint = tint;
            checked = applyItem();
        }
        var children = predicate.children(checked);
        if (checkChildren(e -> e.tint(children, tint))) checked = true;
        return checked;
    }

    private boolean applyItem() {
        if (display != null) {
            display.item(BetterModel.inst().nms().tint(itemStack.itemStack().clone(), tint));
            return true;
        } else return false;
    }

    public @NotNull String getName() {
        return getGroup().getName();
    }

    @ApiStatus.Internal
    public void teleport(@NotNull Location location, @NotNull PacketBundler bundler) {
        if (display != null) display.teleport(location, bundler);
        forEachChildren(e -> e.teleport(location, bundler));
    }

    @ApiStatus.Internal
    public void spawn(@NotNull PacketBundler bundler) {
        if (display != null) display.spawn(bundler);
        forEachChildren(e -> e.spawn(bundler));
    }

    @ApiStatus.Internal
    public void addLoop(@NotNull Predicate<RenderedBone> filter, @NotNull String parent, @NotNull BlueprintAnimation animator, AnimationModifier modifier, Runnable removeTask) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            var iterator = get != null ? new TreeIterator(parent, get.loopIterator(), modifier, removeTask) : new TreeIterator(parent, animator.emptyLoopIterator(), modifier, removeTask);
            synchronized (animators) {
                animators.putLast(parent, iterator);
            }
        }
        forEachChildren(e -> e.addLoop(filter, parent, animator, modifier, () -> {}));
    }

    @ApiStatus.Internal
    public void addSingle(@NotNull Predicate<RenderedBone> filter, @NotNull String parent, @NotNull BlueprintAnimation animator, AnimationModifier modifier, Runnable removeTask) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            var iterator = get != null ? new TreeIterator(parent, get.singleIterator(), modifier, removeTask) : new TreeIterator(parent, animator.emptySingleIterator(), modifier, removeTask);
            synchronized (animators) {
                animators.putLast(parent, iterator);
            }
        }
        forEachChildren(e -> e.addSingle(filter, parent, animator, modifier, () -> {}));
    }

    @ApiStatus.Internal
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
        forEachChildren(e -> e.replaceLoop(filter, target, parent, animator));
    }

    @ApiStatus.Internal
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
        forEachChildren(e -> e.replaceSingle(filter, target, parent, animator));
    }

    /**
     * Stops bone's animation
     * @param filter filter
     * @param parent animation's name
     */
    @ApiStatus.Internal
    public void stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String parent) {
        if (filter.test(this)) {
            synchronized (animators) {
                animators.remove(parent);
            }
        }
        forEachChildren(e -> e.stopAnimation(filter, parent));
    }

    /**
     * Removes model's display
     * @param bundler packet bundler
     */
    @ApiStatus.Internal
    public void remove(@NotNull PacketBundler bundler) {
        if (display != null) display.remove(bundler);
        forEachChildren(e -> e.remove(bundler));
    }

    /**
     * Toggles some part
     * @param predicate predicate
     * @param toggle toggle
     * @return success
     */
    @ApiStatus.Internal
    public boolean togglePart(@NotNull BonePredicate predicate, boolean toggle) {
        var checked = false;
        if (predicate.test(this)) {
            itemStack = toggle ? cachedStack : TransformedItemStack.EMPTY;
            checked = applyItem();
        }
        var children = predicate.children(checked);
        if (checkChildren(e -> e.togglePart(children, toggle))) checked = true;
        return checked;
    }

    /**
     * Gets bone
     * @param predicate predicate
     * @return matched bone
     */
    @ApiStatus.Internal
    public @Nullable RenderedBone boneOf(@NotNull Predicate<RenderedBone> predicate) {
        if (predicate.test(this)) return this;
        for (RenderedBone value : children.values()) {
            var get = value.boneOf(predicate);
            if (get != null) return get;
        }
        return null;
    }

    private void forEachChildren(@NotNull Consumer<RenderedBone> consumer) {
        children.values().forEach(consumer);
    }

    private boolean checkChildren(@NotNull Function<RenderedBone, Boolean> function) {
        var checked = false;
        for (RenderedBone value : children.values()) {
            if (function.apply(value)) checked = true;
        }
        return checked;
    }

    private static class TreeIterator implements BlueprintAnimator.AnimatorIterator, Supplier<Boolean>, Runnable {
        private final String name;
        private final BlueprintAnimator.AnimatorIterator iterator;
        private final AnimationModifier modifier;
        private final Runnable removeTask;

        private boolean started = false;
        private boolean ended = false;

        private float cachedSpeed = 1F;

        public TreeIterator(String name, BlueprintAnimator.AnimatorIterator iterator, AnimationModifier modifier, Runnable removeTask) {
            this.name = name;
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

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TreeIterator that = (TreeIterator) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }

    @NotNull
    @Override
    public Vector3f hitBoxPosition() {
        var hitBox = group.getHitBox();
        return worldPosition(hitBox != null ? hitBox.centerVector() : new Vector3f());
    }

    @NotNull
    @Override
    public ModelRotation hitBoxRotation() {
        return rotation;
    }

    public void despawn() {
        if (hitBox != null) hitBox.removeHitBox();
        for (RenderedBone value : children.values()) {
            value.despawn();
        }
    }
}

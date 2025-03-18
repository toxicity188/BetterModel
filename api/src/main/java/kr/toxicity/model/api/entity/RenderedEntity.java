package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.blueprint.AnimationMovement;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimator;
import kr.toxicity.model.api.data.renderer.AnimationModifier;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.VectorUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A rendered item-display.
 */
public final class RenderedEntity implements TransformSupplier, AutoCloseable {

    @Getter
    private final RendererGroup group;
    @Getter
    private ModelDisplay display;
    private final EntityMovement defaultFrame;

    @Nullable
    private final RenderedEntity parent;

    @Setter
    private Map<String, RenderedEntity> children = Collections.emptyMap();

    private final SequencedMap<String, TreeIterator> animators = new LinkedHashMap<>();
    private AnimationMovement keyFrame = null;
    private long delay = 0;
    private ItemStack itemStack;

    private final List<Consumer<AnimationMovement>> movementModifier = new ArrayList<>();
    @Getter
    private HitBox hitBox;

    private boolean visible;
    private int tint;

    /**
     * Creates entity.
     * @param group group
     * @param parent parent entity
     * @param itemStack item
     * @param transform display transform
     * @param firstLocation spawn location
     * @param movement spawn movement
     */
    public RenderedEntity(
            @NotNull RendererGroup group,
            @Nullable RenderedEntity parent,
            @Nullable ItemStack itemStack,
            @NotNull ItemDisplay.ItemDisplayTransform transform,
            @NotNull Location firstLocation,
            @NotNull EntityMovement movement
    ) {
        this.group = group;
        this.parent = parent;
        this.itemStack = itemStack;
        if (itemStack != null) {
            display = BetterModel.inst().nms().create(firstLocation);
            display.display(transform);
            if (group.getParent().visibility()) display.item(itemStack);
        }
        defaultFrame = movement;
        visible = group.getLimb() != null || group.getParent().visibility();
    }

    public @NotNull ItemStack itemStack() {
        return group.getItemStack();
    }

    /**
     * Creates hit box.
     * @param entity target entity
     * @param predicate predicate
     * @param listener hit box listener
     */
    @ApiStatus.Internal
    public void createHitBox(@NotNull EntityAdapter entity, @NotNull Predicate<RenderedEntity> predicate, @Nullable HitBoxListener listener) {
        var h = group.getHitBox();
        if (h != null && predicate.test(this)) {
            var l = listener;
            if (hitBox != null) {
                hitBox.remove();
                if (l == null) l = hitBox.listener();
            }
            hitBox = BetterModel.inst().nms().createHitBox(entity, this, h, l != null ? l : HitBoxListener.EMPTY);
        }
        for (RenderedEntity value : children.values()) {
            value.createHitBox(entity, predicate, listener);
        }
    }

    /**
     * Changes displayed item
     * @param predicate predicate
     * @param itemStack target item
     */
    @ApiStatus.Internal
    public void itemStack(@NotNull Predicate<RenderedEntity> predicate, @NotNull ItemStack itemStack) {
        if (predicate.test(this)) {
            this.itemStack = itemStack;
            if (display != null) {
                display.item(itemStack);
            }
        }
        for (RenderedEntity value : children.values()) {
            value.itemStack(predicate, itemStack);
        }
    }

    /**
     * Adds animation modifier.
     * @param predicate predicate
     * @param consumer animation consumer
     * @return whether to success
     */
    public boolean addAnimationMovementModifier(@NotNull Predicate<RenderedEntity> predicate, @NotNull Consumer<AnimationMovement> consumer) {
        if (predicate.test(this)) {
            movementModifier.add(consumer);
            return true;
        }
        var ret = false;
        for (RenderedEntity value : children.values()) {
            if (value.addAnimationMovementModifier(predicate, consumer)) ret = true;
        }
        return ret;
    }

    /**
     * Adds all renderers to list.
     * @param renderers target list
     */
    @ApiStatus.Internal
    public void renderers(List<RenderedEntity> renderers) {
        renderers.add(this);
        children.values().forEach(c -> c.renderers(renderers));
    }

    private TreeIterator currentIterator = null;
    public void updateAnimation() {
        synchronized (animators) {
            var iterator = animators.reversed().values().iterator();
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
        for (RenderedEntity e : children.values()) {
            e.updateAnimation();
        }
    }

    private boolean updateKeyframe(Iterator<TreeIterator> iterator, TreeIterator next) {
        if (!next.hasNext()) {
            next.run();
            iterator.remove();
            return false;
        } else {
            keyFrame = next.next();
            return true;
        }
    }

    private TrackerMovement lastMovement;
    private EntityMovement beforeTransform, lastTransform;
    private Vector3f defaultPosition = new Vector3f();
    private ModelRotation rotation = ModelRotation.EMPTY;

    public void lastMovement(@NotNull TrackerMovement movement) {
        lastMovement = movement;
        for (RenderedEntity value : children.values()) {
            value.lastMovement(movement);
        }
    }

    public void move(@Nullable ModelRotation rotation, @NotNull TrackerMovement movement, @NotNull PacketBundler bundler) {
        var d = display;
        if (rotation != null) {
            this.rotation = rotation;
            if (d != null) {
                d.rotate(rotation, bundler);
            }
        }
        if (delay <= 0) {
            var f = frame();
            delay = f;
            beforeTransform = lastTransform;
            var entityMovement = (lastTransform = (lastMovement = movement.copy()).plus(relativeOffset())).plus(defaultPosition);
            if (d != null) {
                d.frame(f <= 0 ? 0 : toInterpolationDuration(f));
                setup(entityMovement);
                d.send(bundler);
            }
        }
        --delay;
        for (RenderedEntity e : children.values()) {
            e.move(rotation, movement, bundler);
        }
    }
    public void forceUpdate(@NotNull PacketBundler bundler) {
        var d = display;
        var speed = currentIterator != null ? currentIterator.modifier.speedValue() : 1F;
        delay = Math.round(speed * (double) delay);
        if (d != null && lastMovement != null && delay > 0) {
            beforeTransform = lastTransform;
            var entityMovement = (lastTransform = lastMovement.copy().plus(relativeOffset())).plus(defaultPosition);
            d.frame(toInterpolationDuration(delay));
            setup(entityMovement);
            d.send(bundler);
        }
    }

    private static int toInterpolationDuration(long delay) {
        return (int) Math.ceil((float) delay / 5F) + 2;
    }

    public @NotNull Vector3f lastTransform() {
        if (lastTransform != null) {
            var progress = 1 - progress();
            var before = beforeTransform != null ? beforeTransform : EntityMovement.EMPTY;
            return VectorUtil.linear(before.transform(), lastTransform.transform(), progress)
                    .mul(VectorUtil.linear(before.scale(), lastTransform.scale(), progress))
                    .rotate(
                            MathUtil.toQuaternion(MathUtil.blockBenchToDisplay(VectorUtil.linear(before.rawRotation(), lastTransform.rawRotation(), progress)))
                    )
                    .add(defaultPosition)
                    .rotateY((float) -Math.toRadians(rotation.y()));
        }
        return new Vector3f();
    }

    public void setup(@NotNull TrackerMovement movement) {
        setup(movement.plus(relativeOffset().plus(defaultPosition)));
        for (RenderedEntity value : children.values()) {
            value.setup(movement);
        }
    }
    private void setup(@NotNull EntityMovement entityMovement) {
        if (display != null) {
            var limb = group.getLimb();
            display.transform(new Transformation(
                    limb != null ? new Vector3f(entityMovement.transform()).add(new Vector3f(limb.getOffset()).rotate(entityMovement.rotation())) : entityMovement.transform(),
                    entityMovement.rotation(),
                    new Vector3f(entityMovement.scale()).mul(group.getScale()),
                    new Quaternionf()
            ));
        }
    }

    public void defaultPosition(@NotNull Vector3f movement) {
        defaultPosition = group.getLimb() != null ? new Vector3f(movement).add(group.getLimb().getPosition()) : movement;
        for (RenderedEntity value : children.values()) {
            value.defaultPosition(movement);
        }
    }

    private int frame() {
        return keyFrame != null ? Math.round(keyFrame.time() * 100) : parent != null ? parent.frame() : 5;
    }

    private EntityMovement defaultFrame() {
        var k = keyFrame != null ? keyFrame.copyNotNull() : new AnimationMovement(0, new Vector3f(), new Vector3f(), new Vector3f());
        for (Consumer<AnimationMovement> consumer : movementModifier) {
            consumer.accept(k);
        }
        return defaultFrame.plus(k);
    }

    private float progress() {
        return delay / (float) frame();
    }

    private EntityMovement relativeOffset() {
        var def = defaultFrame();
        if (parent != null) {
            var p = parent.relativeOffset();
            return new EntityMovement(
                    new Vector3f(p.transform()).add(new Vector3f(def.transform()).mul(p.scale()).rotate(p.rotation())),
                    new Vector3f(def.scale()).mul(p.scale()),
                    new Quaternionf(p.rotation()).mul(def.rotation()),
                    def.rawRotation()
            );
        }
        return def;
    }
    private EntityMovement relativeHitBoxOffset() {
        var def = defaultFrame();
        var hitBox = group.getHitBox();
        if (hitBox != null) def.transform().add(hitBox.centerVector());
        if (parent != null) {
            var p = parent.relativeOffset();
            return new EntityMovement(
                    new Vector3f(p.transform()).add(new Vector3f(def.transform()).mul(p.scale()).rotate(p.rotation())),
                    new Vector3f(def.scale()).mul(p.scale()),
                    new Quaternionf(p.rotation()).mul(def.rotation()),
                    def.rawRotation()
            );
        }
        return def;
    }

    public void tint(int toggle, @NotNull PacketBundler bundler) {
        tint = toggle;
        if (applyItem()) forceUpdate(bundler);
        for (RenderedEntity value : children.values()) {
            value.tint(toggle, bundler);
        }
    }

    private boolean applyItem() {
        if (display != null) {
            display.item(visible ? BetterModel.inst().nms().tint(itemStack.clone(), tint) : new ItemStack(Material.AIR));
            return true;
        } else return false;
    }

    public @NotNull String getName() {
        return getGroup().getName();
    }

    public void teleport(@NotNull Location location, @NotNull PacketBundler bundler) {
        if (display != null) display.teleport(location, bundler);
        children.values().forEach(e -> e.teleport(location, bundler));
    }

    public void spawn(@NotNull PacketBundler bundler) {
        if (display != null) display.spawn(bundler);
        children.values().forEach(e -> e.spawn(bundler));
    }

    private void applyAnimation() {
        if (lastTransform != null) setup(lastTransform);
    }

    public void addLoop(@NotNull Predicate<RenderedEntity> filter, @NotNull String parent, @NotNull BlueprintAnimation animator, AnimationModifier modifier, Runnable removeTask) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            var iterator = get != null ? new TreeIterator(parent, get.loopIterator(), modifier, removeTask) : new TreeIterator(parent, animator.emptyLoopIterator(), modifier, removeTask);
            synchronized (animators) {
                animators.putLast(parent, iterator);
            }
            applyAnimation();
        }
        children.values().forEach(c -> c.addLoop(filter, parent, animator, modifier, () -> {}));
    }
    public void addSingle(@NotNull Predicate<RenderedEntity> filter, @NotNull String parent, @NotNull BlueprintAnimation animator, AnimationModifier modifier, Runnable removeTask) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            var iterator = get != null ? new TreeIterator(parent, get.singleIterator(), modifier, removeTask) : new TreeIterator(parent, animator.emptySingleIterator(), modifier, removeTask);
            synchronized (animators) {
                animators.putLast(parent, iterator);
            }
            applyAnimation();
        }
        children.values().forEach(c -> c.addSingle(filter, parent, animator, modifier, () -> {}));
    }

    public void replaceLoop(@NotNull Predicate<RenderedEntity> filter, @NotNull String target, @NotNull String parent, @NotNull BlueprintAnimation animator) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            synchronized (animators) {
                var v = animators.get(target);
                if (v != null) animators.replace(target, get != null ? new TreeIterator(parent, get.loopIterator(), v.modifier, v.removeTask) : new TreeIterator(parent, animator.emptyLoopIterator(), v.modifier, v.removeTask));
                else animators.replace(target, get != null ? new TreeIterator(parent, get.loopIterator(), AnimationModifier.DEFAULT_LOOP, () -> {
                }) : new TreeIterator(parent, animator.emptyLoopIterator(), AnimationModifier.DEFAULT_LOOP, () -> {
                }));
            }
            applyAnimation();
        }
        children.values().forEach(c -> c.replaceLoop(filter, target, parent, animator));
    }
    public void replaceSingle(@NotNull Predicate<RenderedEntity> filter, @NotNull String target, @NotNull String parent, @NotNull BlueprintAnimation animator) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            synchronized (animators) {
                var v = animators.get(target);
                if (v != null) animators.replace(target, get != null ? new TreeIterator(parent, get.singleIterator(), v.modifier, v.removeTask) : new TreeIterator(parent, animator.emptySingleIterator(), v.modifier, v.removeTask));
                else animators.replace(target, get != null ? new TreeIterator(parent, get.singleIterator(), AnimationModifier.DEFAULT, () -> {
                }) : new TreeIterator(parent, animator.emptySingleIterator(), AnimationModifier.DEFAULT, () -> {
                }));
            }
            applyAnimation();
        }
        children.values().forEach(c -> c.replaceSingle(filter, target, parent, animator));
    }

    public void stopAnimation(@NotNull Predicate<RenderedEntity> filter, @NotNull String parent) {
        if (filter.test(this)) {
            synchronized (animators) {
                animators.remove(parent);
            }
        }
        children.values().forEach(c -> c.stopAnimation(filter, parent));
    }

    public void remove(@NotNull PacketBundler bundler) {
        if (display != null) display.remove(bundler);
        children.values().forEach(e -> e.remove(bundler));
    }

    public void togglePart(@NotNull PacketBundler bundler, @NotNull Predicate<RenderedEntity> predicate, boolean toggle) {
        if (predicate.test(this)) {
            visible = toggle;
            if (applyItem()) forceUpdate(bundler);
        }
        for (RenderedEntity value : children.values()) {
            value.togglePart(bundler, predicate, toggle);
        }
    }

    @RequiredArgsConstructor
    private class TreeIterator implements BlueprintAnimator.AnimatorIterator, Supplier<Boolean>, Runnable {
        private final String name;
        private final BlueprintAnimator.AnimatorIterator iterator;
        private final AnimationModifier modifier;
        private final Runnable removeTask;
        private boolean started = false;
        private boolean ended = false;

        private AnimationMovement lastMovement;

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
                lastMovement = currentIterator != null ? currentIterator.first() : keyFrame;
                return first().time((float) modifier.start() / 20);
            }
            if (!iterator.hasNext()) {
                ended = true;
                return lastMovement != null ? lastMovement.time((float) modifier.end() / 20) : new AnimationMovement((float) modifier.end() / 20, null, null, null);
            }
            var nxt = iterator.next();
            nxt = nxt.time(Math.max(nxt.time() / modifier.speedValue(), 0.01F));
            return nxt;
        }

        @Override
        public void clear() {
            started = ended = false;
            iterator.clear();
        }

        @Override
        public int length() {
            return iterator.length();
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
    public Vector3f supplyTransform() {
        return lastMovement != null ? lastMovement.plus(relativeHitBoxOffset()).plus(defaultPosition).transform() : relativeHitBoxOffset().plus(defaultPosition).transform();
    }

    @Override
    public void close() throws Exception {
        if (hitBox != null) hitBox.remove();
        if (display != null) display.close();
        for (RenderedEntity value : children.values()) {
            value.close();
        }
    }
}

package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.blueprint.AnimationMovement;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimator;
import kr.toxicity.model.api.data.renderer.AnimationModifier;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import kr.toxicity.model.api.nms.*;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class RenderedEntity implements TransformSupplier, AutoCloseable {

    private static final int ANIMATION_THRESHOLD = 4;

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
    private boolean tint;

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

    public void createHitBox(@NotNull Entity entity, @NotNull Predicate<RenderedEntity> predicate, @Nullable HitBoxListener listener) {
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

    public void renderers(List<ModelDisplay> renderers) {
        if (display != null) renderers.add(display);
        children.values().forEach(c -> c.renderers(renderers));
    }

    private TreeIterator currentIterator = null;
    private void updateAnimation() {
        synchronized (animators) {
            var iterator = animators.reversed().values().iterator();
            var check = true;
            while (iterator.hasNext()) {
                var next = iterator.next();
                if (!next.get()) continue;
                if (!next.hasNext()) {
                    next.run();
                    iterator.remove();
                }
                else {
                    if (check) {
                        if (currentIterator == null) {
                            currentIterator = next;
                            keyFrame = next.next();
                        } else if (!currentIterator.name.equals(next.name)) {
                            currentIterator = next;
                            delay = 0;
                            keyFrame = next.next();
                        } else if (delay <= 0) {
                            keyFrame = next.next();
                        }
                        check = false;
                    } else {
                        next.clear();
                    }
                }
            }
            if (check) {
                keyFrame = null;
            }
        }
    }

    private TrackerMovement lastMovement;
    private EntityMovement lastTransform;
    private Vector3f defaultPosition = new Vector3f();

    public void lastMovement(@NotNull TrackerMovement movement) {
        lastMovement = movement;
        for (RenderedEntity value : children.values()) {
            value.lastMovement(movement);
        }
    }

    public void move(@NotNull TrackerMovement movement, @NotNull PacketBundler bundler) {
        var d = display;
        if (delay <= 0) {
            var f = frame();
            delay = f;
            var entityMovement = lastTransform = (lastMovement = movement.copy()).plus(relativeOffset().plus(defaultPosition));
            if (d != null) {
                d.frame(Math.max(f, ANIMATION_THRESHOLD));
                setup(entityMovement);
                d.send(bundler);
            }
        }
        --delay;
        for (RenderedEntity e : children.values()) {
            e.move(movement, bundler);
        }
        updateAnimation();
    }
    public void forceUpdate(@NotNull PacketBundler bundler) {
        var d = display;
        if (d != null && lastMovement != null && delay > 0) {
            var entityMovement = lastTransform = lastMovement.copy().plus(relativeOffset().plus(defaultPosition));
            d.frame((int) Math.max(delay, ANIMATION_THRESHOLD));
            setup(entityMovement);
            d.send(bundler);
        }
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
        return keyFrame != null ? (int) keyFrame.time() : parent != null ? parent.frame() : 1;
    }

    private EntityMovement defaultFrame() {
        var k = keyFrame != null ? keyFrame.copyNotNull() : new AnimationMovement(0, new Vector3f(), new Vector3f(), new Vector3f());
        for (Consumer<AnimationMovement> consumer : movementModifier) {
            consumer.accept(k);
        }
        return defaultFrame.plus(k);
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

    public double height() {
        var d = (double) defaultPosition.y + (lastTransform != null ? lastTransform.transform().y : 0);
        for (RenderedEntity value : children.values()) {
            var v = value.height();
            if (getGroup().getCenter().y < value.getGroup().getCenter().y) d = v;
        }
        return d;
    }

    public void tint(boolean toggle, @NotNull PacketBundler bundler) {
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

    public void addLoop(@NotNull Predicate<RenderedEntity> filter, @NotNull String parent, @NotNull BlueprintAnimation animator, AnimationModifier modifier, Runnable removeTask) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            var iterator = get != null ? new TreeIterator(parent, get.loopIterator(), modifier, removeTask) : new TreeIterator(parent, animator.emptyLoopIterator(), modifier, removeTask);
            synchronized (animators) {
                animators.putIfAbsent(parent, iterator);
            }
        }
        children.values().forEach(c -> c.addLoop(filter, parent, animator, modifier, () -> {}));
    }
    public void addSingle(@NotNull Predicate<RenderedEntity> filter, @NotNull String parent, @NotNull BlueprintAnimation animator, AnimationModifier modifier, Runnable removeTask) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            var iterator = get != null ? new TreeIterator(parent, get.singleIterator(), modifier, removeTask) : new TreeIterator(parent, animator.emptySingleIterator(), modifier, removeTask);
            synchronized (animators) {
                animators.putIfAbsent(parent, iterator);
            }
        }
        children.values().forEach(c -> c.addSingle(filter, parent, animator, modifier, () -> {}));
    }

    public void replaceLoop(@NotNull Predicate<RenderedEntity> filter, @NotNull String target, @NotNull String parent, @NotNull BlueprintAnimation animator) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            synchronized (animators) {
                var v = animators.get(target);
                if (v != null) animators.replace(target, get != null ? new TreeIterator(parent, get.loopIterator(), v.modifier, v.removeTask) : new TreeIterator(parent, animator.emptyLoopIterator(), v.modifier, v.removeTask));
                else animators.replace(target, get != null ? new TreeIterator(parent, get.loopIterator(), AnimationModifier.DEFAULT, () -> {}) : new TreeIterator(parent, animator.emptyLoopIterator(), AnimationModifier.DEFAULT, () -> {}));
            }
        }
        children.values().forEach(c -> c.replaceLoop(filter, target, parent, animator));
    }
    public void replaceSingle(@NotNull Predicate<RenderedEntity> filter, @NotNull String target, @NotNull String parent, @NotNull BlueprintAnimation animator) {
        if (filter.test(this)) {
            var get = animator.animator().get(getName());
            synchronized (animators) {
                var v = animators.get(target);
                if (v != null) animators.replace(target, get != null ? new TreeIterator(parent, get.singleIterator(), v.modifier, v.removeTask) : new TreeIterator(parent, animator.emptySingleIterator(), v.modifier, v.removeTask));
                else animators.replace(target, get != null ? new TreeIterator(parent, get.singleIterator(), AnimationModifier.DEFAULT, () -> {}) : new TreeIterator(parent, animator.emptySingleIterator(), AnimationModifier.DEFAULT, () -> {}));
            }
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

    private record TreeIterator(
            String name,
            BlueprintAnimator.AnimatorIterator iterator,
            AnimationModifier modifier,
            Runnable removeTask
    ) implements BlueprintAnimator.AnimatorIterator, Supplier<Boolean>, Runnable {

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
            return iterator.hasNext();
        }

        @Override
        public AnimationMovement next() {
            int i;
            if (index() == 0) i = modifier().start();
            else if (index() == lastIndex()) i = modifier().end();
            else i = 0;
            var nxt = iterator.next();
            return i == 0 ? nxt : nxt.time(nxt.time() + i + 1);
        }

        @Override
        public void clear() {
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
        return lastMovement != null ? lastMovement.plus(relativeHitBoxOffset().plus(defaultPosition)).transform() : relativeHitBoxOffset().plus(defaultPosition).transform();
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

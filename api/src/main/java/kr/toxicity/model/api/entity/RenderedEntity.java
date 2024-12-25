package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.ModelRenderer;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class RenderedEntity implements TransformSupplier {

    private static final int ANIMATION_THRESHOLD = 3;

    @Getter
    private final RendererGroup group;
    @Getter
    private ModelDisplay display;
    private final EntityMovement defaultFrame;

    @Nullable
    private final RenderedEntity parent;

    @Setter
    private Map<String, RenderedEntity> children = Collections.emptyMap();

    private final Function<Location, ModelDisplay> displayFunction;
    private final SequencedMap<String, TreeIterator> animators = new LinkedHashMap<>();
    private AnimationMovement keyFrame = null;
    private long delay = 0;
    private final ItemStack itemStack;

    private final List<Consumer<AnimationMovement>> movementModifier = new ArrayList<>();
    @Getter
    private HitBox hitBox;

    private boolean visible;
    private boolean tint;

    public RenderedEntity(
            @NotNull RendererGroup group,
            @Nullable RenderedEntity parent,
            @NotNull Function<Location, ModelDisplay> displayFunction,
            @Nullable Location firstLocation,
            @NotNull EntityMovement movement
    ) {
        this.group = group;
        this.parent = parent;
        this.displayFunction = displayFunction;
        this.display = displayFunction.apply(firstLocation);
        defaultFrame = movement;
        itemStack = group.getItemStack();
        visible = group.getParent().visibility();
    }

    public void createHitBox(@NotNull Entity entity, @NotNull Predicate<RenderedEntity> predicate, @NotNull HitBoxListener listener) {
        var h = group.getHitBox();
        if (h != null && predicate.test(this)) {
            if (hitBox != null) hitBox.remove();
            hitBox = ModelRenderer.inst().nms().createHitBox(entity, this, h, listener);
        }
        for (RenderedEntity value : children.values()) {
            value.createHitBox(entity, predicate, listener);
        }
    }

    public void removeHitBox() {
        if (hitBox != null) hitBox.remove();
        for (RenderedEntity value : children.values()) {
            value.removeHitBox();
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

    public void changeWorld(@NotNull Location location) {
        display = displayFunction.apply(location);
        children.values().forEach(c -> c.changeWorld(location));
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
    private Vector3f defaultPosition = new Vector3f();
    private EntityMovement lastTransformation = null;
    public void move(@NotNull TrackerMovement movement, @NotNull PacketBundler bundler) {
        var d = display;
        if (delay <= 0) {
            var f = frame();
            delay = f;
            var entityMovement = lastTransformation = (lastMovement = movement.copy()).plus(relativeOffset());
            if (d != null) {
                d.frame(Math.max(f, ANIMATION_THRESHOLD));
                d.transform(new Transformation(
                        new Vector3f(entityMovement.transform()).add(defaultPosition),
                        entityMovement.rotation(),
                        new Vector3f(entityMovement.scale()).mul(group.getScale()),
                        new Quaternionf()
                ));
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
            var entityMovement = lastTransformation = lastMovement.copy().plus(relativeOffset());
            d.frame((int) Math.max(delay, ANIMATION_THRESHOLD));
            d.transform(new Transformation(
                    new Vector3f(entityMovement.transform()).add(defaultPosition),
                    entityMovement.rotation(),
                    new Vector3f(entityMovement.scale()).mul(group.getScale()),
                    new Quaternionf()
            ));
            d.send(bundler);
        }
    }

    public void defaultPosition(@NotNull Vector3f movement) {
        defaultPosition = movement;
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

    public void tint(boolean toggle, @NotNull PacketBundler bundler) {
        tint = toggle;
        if (applyItem()) forceUpdate(bundler);
        for (RenderedEntity value : children.values()) {
            value.tint(toggle, bundler);
        }
    }

    private boolean applyItem() {
        if (display != null) {
            display.item(visible ? ModelRenderer.inst().nms().tint(itemStack.clone(), tint) : new ItemStack(Material.AIR));
            return true;
        } else return false;
    }

    public @NotNull String getName() {
        return getGroup().getName();
    }

    public void teleport(@NotNull Location location) {
        if (display != null) display.teleport(location);
        children.values().forEach(e -> e.teleport(location));
    }

    public void spawn(@NotNull PacketBundler bundler) {
        if (display != null) display.spawn(bundler);
        children.values().forEach(e -> e.spawn(bundler));
    }

    public void addLoop(@NotNull String parent, @NotNull BlueprintAnimation animator, AnimationModifier modifier, Runnable removeTask) {
        var get = animator.animator().get(getName());
        var iterator = get != null ? new TreeIterator(parent, get.loopIterator(), modifier, removeTask) : new TreeIterator(parent, animator.emptyLoopIterator(), modifier, removeTask);
        synchronized (animators) {
            animators.putIfAbsent(parent, iterator);
        }
        children.values().forEach(c -> c.addLoop(parent, animator, modifier, () -> {}));
    }
    public void addSingle(@NotNull String parent, @NotNull BlueprintAnimation animator, AnimationModifier modifier, Runnable removeTask) {
        var get = animator.animator().get(getName());
        var iterator = get != null ? new TreeIterator(parent, get.singleIterator(), modifier, removeTask) : new TreeIterator(parent, animator.emptySingleIterator(), modifier, removeTask);
        synchronized (animators) {
            animators.putIfAbsent(parent, iterator);
        }
        children.values().forEach(c -> c.addSingle(parent, animator, modifier, () -> {}));
    }

    public void replaceLoop(@NotNull String target, @NotNull String parent, @NotNull BlueprintAnimation animator) {
        var get = animator.animator().get(getName());
        synchronized (animators) {
            var v = animators.get(target);
            if (v != null) animators.replace(target, get != null ? new TreeIterator(parent, get.loopIterator(), v.modifier, v.removeTask) : new TreeIterator(parent, animator.emptyLoopIterator(), v.modifier, v.removeTask));
            else animators.replace(target, get != null ? new TreeIterator(parent, get.loopIterator(), AnimationModifier.DEFAULT, () -> {}) : new TreeIterator(parent, animator.emptyLoopIterator(), AnimationModifier.DEFAULT, () -> {}));
        }
        children.values().forEach(c -> c.replaceLoop(target, parent, animator));
    }
    public void replaceSingle(@NotNull String target, @NotNull String parent, @NotNull BlueprintAnimation animator) {
        var get = animator.animator().get(getName());
        synchronized (animators) {
            var v = animators.get(target);
            if (v != null) animators.replace(target, get != null ? new TreeIterator(parent, get.singleIterator(), v.modifier, v.removeTask) : new TreeIterator(parent, animator.emptySingleIterator(), v.modifier, v.removeTask));
            else animators.replace(target, get != null ? new TreeIterator(parent, get.singleIterator(), AnimationModifier.DEFAULT, () -> {}) : new TreeIterator(parent, animator.emptySingleIterator(), AnimationModifier.DEFAULT, () -> {}));
        }
        children.values().forEach(c -> c.replaceSingle(target, parent, animator));
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
        return lastTransformation != null ? lastTransformation.transform() : relativeOffset().transform();
    }
}

package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.blueprint.AnimationMovement;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimator;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PacketBundler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
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

public final class RenderedEntity implements AutoCloseable {
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
    private final Set<TreeIterator> animators = new TreeSet<>(Comparator.reverseOrder());
    private AnimationMovement keyFrame = null;
    private long delay = 0;
    private ItemStack itemStack;

    private final List<Consumer<AnimationMovement>> movementModifier = new ArrayList<>();

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
        if (display != null) {
            try {
                display.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        display = displayFunction.apply(location);
        children.values().forEach(c -> c.changeWorld(location));
    }

    private TreeIterator currentIterator = null;
    private void updateAnimation() {
        var iterator = animators.iterator();
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
                        var get = next.next();
                        keyFrame = get.time() < 4 ? get.time(4) : get;
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

    private TrackerMovement lastMovement = null;
    public void move(@NotNull TrackerMovement movement, @NotNull PacketBundler bundler) {
        updateAnimation();
        var d = display;
        if (delay <= 0) {
            var f = frame();
            delay = f;
            if (d != null) {
                d.frame(Math.max(f, 4));
                var entityMovement = (lastMovement = movement.copy()).plus(relativeOffset());
                d.transform(new Transformation(
                        entityMovement.transform(),
                        entityMovement.rotation(),
                        entityMovement.scale(),
                        new Quaternionf()
                ));
                d.send(bundler);
            }
        }
        --delay;
        for (RenderedEntity e : children.values()) {
            e.move(movement, bundler);
        }
    }
    public void forceUpdate(@NotNull PacketBundler bundler) {
        var d = display;
        if (d != null && lastMovement != null) {
            var entityMovement = lastMovement.copy().plus(relativeOffset());
            var f = frame() - delay;
            d.frame((int) Math.max(f, 4));
            d.transform(new Transformation(
                    entityMovement.transform(),
                    entityMovement.rotation(),
                    entityMovement.scale(),
                    new Quaternionf()
            ));
            d.send(bundler);
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
        itemStack = ModelRenderer.inst().nms().tint(itemStack, toggle);
        if (display != null) display.item(itemStack);
        forceUpdate(bundler);
        for (RenderedEntity value : children.values()) {
            value.tint(toggle, bundler);
        }
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

    public void addLoop(@NotNull String parent, @NotNull BlueprintAnimation animator, Supplier<Boolean> predicate, Runnable removeTask) {
        var get = animator.animator().get(getName());
        synchronized (animators) {
            if (get != null) {
                animators.add(new TreeIterator(parent, get.loopIterator(), predicate, removeTask));
            } else {
                animators.add(new TreeIterator(parent, animator.emptyLoopIterator(), predicate, removeTask));
            }
        }
        children.values().forEach(c -> c.addLoop(parent, animator, predicate, () -> {}));
    }
    public void addSingle(@NotNull String parent, @NotNull BlueprintAnimation animator, Supplier<Boolean> predicate, Runnable removeTask) {
        var get = animator.animator().get(getName());
        synchronized (animators) {
            if (get != null) {
                animators.add(new TreeIterator(parent, get.singleIterator(), predicate, removeTask));
            } else {
                animators.add(new TreeIterator(parent, animator.emptySingleIterator(), predicate, removeTask));
            }
        }
        children.values().forEach(c -> c.addSingle(parent, animator, predicate, () -> {}));
    }

    public void remove(@NotNull PacketBundler bundler) {
        if (display != null) display.remove(bundler);
        children.values().forEach(e -> e.remove(bundler));
    }

    private class TreeIterator implements BlueprintAnimator.AnimatorIterator, Comparable<TreeIterator>, Supplier<Boolean>, Runnable {

        private final String name;
        private final int index = animators.size();
        private final Supplier<Boolean> predicate;
        private final BlueprintAnimator.AnimatorIterator iterator;
        private final Runnable removeTask;

        private TreeIterator(@NotNull String name, @NotNull BlueprintAnimator.AnimatorIterator iterator, Supplier<Boolean> predicate, Runnable removeTask) {
            this.name = name;
            this.iterator = iterator;
            this.predicate = predicate;
            this.removeTask = removeTask;
        }

        @Override
        public void run() {
            removeTask.run();
        }

        @Override
        public Boolean get() {
            return predicate.get();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public AnimationMovement next() {
            return iterator.next();
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

        @Override
        public int compareTo(@NotNull RenderedEntity.TreeIterator o) {
            return Integer.compare(index, o.index);
        }
    }

    @Override
    public void close() throws Exception {
        if (display != null) display.close();
    }
}

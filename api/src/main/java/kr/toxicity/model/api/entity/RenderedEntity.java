package kr.toxicity.model.api.entity;

import kr.toxicity.model.api.data.blueprint.AnimationMovement;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimator;
import kr.toxicity.model.api.data.renderer.RendererGroup;
import kr.toxicity.model.api.nms.ModelDisplay;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Predicate;

public class RenderedEntity {
    @Getter
    private final RendererGroup group;
    private final ModelDisplay display;
    private final EntityMovement defaultFrame;

    @Nullable
    private final RenderedEntity parent;

    @Setter
    private Map<String, RenderedEntity> children = Collections.emptyMap();

    private final Set<TreeIterator> animators = new TreeSet<>(Comparator.reverseOrder());
    private AnimationMovement keyFrame = null;
    private long delay = 0;

    public RenderedEntity(@NotNull RendererGroup group, @Nullable RenderedEntity parent, @Nullable ModelDisplay display, @NotNull EntityMovement movement) {
        this.group = group;
        this.parent = parent;
        this.display = display;
        defaultFrame = movement;
    }

    public void move(@NotNull EntityMovement movement) {
        var iterator = animators.iterator();
        var check = true;
        while (iterator.hasNext()) {
            var next = iterator.next();
            if (!next.hasNext()) iterator.remove();
            else {
                if (check) {
                    if (delay <= 0) {
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
        var d = display;
        if (delay <= 0) {
            var f = frame();
            delay = f;
            if (d != null) {
                var entityMovement = relativeOffset().plus(movement);
                d.frame(f);
                d.transform(new Transformation(
                        entityMovement.transform(),
                        entityMovement.rotation(),
                        entityMovement.scale(),
                        new Quaternionf()
                ));
            }
        }
        --delay;
        for (RenderedEntity e : children.values()) {
            e.move(movement);
        }
    }

    private int frame() {
        return keyFrame != null ? (int) Math.max(keyFrame.time(), 1) : (parent != null ? parent.frame() : 1);
    }

    private EntityMovement defaultFrame() {
        return keyFrame != null ? defaultFrame.plus(keyFrame) : defaultFrame;
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

    public @NotNull String getName() {
        return getGroup().getName();
    }

    public void teleport(@NotNull Location location) {
        if (display != null) display.teleport(location);
        children.values().forEach(e -> e.teleport(location));
    }

    public void spawn(@NotNull Player player) {
        if (display != null) display.spawn(player);
        children.values().forEach(e -> e.spawn(player));
    }

    public void addLoop(@NotNull String parent, @NotNull BlueprintAnimation animator) {
        var get = animator.animator().get(getName());
        if (get != null) {
            animators.add(new TreeIterator(parent, get.loopIterator()));
        }
        children.values().forEach(c -> c.addLoop(parent, animator));
    }
    public void addSingle(@NotNull String parent, @NotNull BlueprintAnimation animator) {
        var get = animator.animator().get(getName());
        if (get != null) {
            animators.add(new TreeIterator(parent, get.singleIterator()));
        }
        children.values().forEach(c -> c.addSingle(parent, animator));
    }

    public void remove() {
        if (display != null) display.remove();
        children.values().forEach(RenderedEntity::remove);
    }

    private class TreeIterator implements BlueprintAnimator.AnimatorIterator, Comparable<TreeIterator>, Predicate<Player> {

        private final String name;
        private final int index = animators.size();
        private final BlueprintAnimator.AnimatorIterator iterator;

        private TreeIterator(@NotNull String name, @NotNull BlueprintAnimator.AnimatorIterator iterator) {
            this.name = name;
            this.iterator = iterator;
        }

        @Override
        public boolean test(Player player) {
            return true;
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
}

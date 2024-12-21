package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.ModelRenderer;
import kr.toxicity.model.api.data.blueprint.AnimationMovement;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.entity.RenderedEntity;
import kr.toxicity.model.api.entity.TrackerMovement;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PacketBundler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class RenderInstance implements AutoCloseable {
    private final Map<String, RenderedEntity> entityMap;
    private final Map<String, BlueprintAnimation> animationMap;
    private final Map<UUID, Player> playerMap = new ConcurrentHashMap<>();

    public RenderInstance(@NotNull Map<String, RenderedEntity> entityMap, @NotNull Map<String, BlueprintAnimation> animationMap) {
        this.entityMap = entityMap;
        this.animationMap = animationMap;

        animateLoop("idle");
    }

    @Override
    public void close() throws Exception {
        for (RenderedEntity value : entityMap.values()) {
            value.close();
        }
        for (Player value : playerMap.values()) {
            remove0(value);
        }
        playerMap.clear();
    }

    public void teleport(@NotNull Location location) {
        entityMap.values().forEach(e -> e.teleport(location));
    }

    public void move(@NotNull TrackerMovement movement, @NotNull PacketBundler bundler) {
        entityMap.values().forEach(e -> e.move(movement, bundler));
    }

    public boolean addAnimationMovementModifier(@NotNull Consumer<AnimationMovement> consumer) {
        return addAnimationMovementModifier(r -> true, consumer);
    }
    public boolean addAnimationMovementModifier(@NotNull Predicate<RenderedEntity> predicate, @NotNull Consumer<AnimationMovement> consumer) {
        for (RenderedEntity value : entityMap.values()) {
            if (value.addAnimationMovementModifier(predicate, consumer)) return true;
        }
        return false;
    }

    public @NotNull List<ModelDisplay> renderers() {
        var list = new ArrayList<ModelDisplay>();
        entityMap.values().forEach(e -> e.renderers(list));
        return list;
    }

    public void setColor(@Nullable Color color) {
        entityMap.values().forEach(e -> e.setColor(color));
    }

    public boolean animateLoop(@NotNull String animation) {
        return animateLoop(animation, () -> true);
    }

    public boolean animateLoop(@NotNull String animation, Supplier<Boolean> predicate) {
        return animateLoop(animation, predicate, () -> {});
    }

    public boolean animateLoop(@NotNull String animation, Supplier<Boolean> predicate, Runnable removeTask) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        for (RenderedEntity value : entityMap.values()) {
            value.addLoop(animation, get, predicate, removeTask);
        }
        return true;
    }

    public boolean animateSingle(@NotNull String animation) {
        return animateSingle(animation, () -> true);
    }

    public boolean animateSingle(@NotNull String animation, Supplier<Boolean> predicate) {
        return animateSingle(animation, predicate, () -> {});
    }

    public boolean animateSingle(@NotNull String animation, Supplier<Boolean> predicate, Runnable removeTask) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        for (RenderedEntity value : entityMap.values()) {
            value.addSingle(animation, get, predicate, removeTask);
        }
        return true;
    }

    public void spawn(@NotNull Player player) {
        playerMap.computeIfAbsent(player.getUniqueId(), u -> {
            var bundler = ModelRenderer.inst().nms().createBundler();
            entityMap.values().forEach(e -> e.spawn(bundler));
            bundler.send(player);
            return player;
        });
    }
    public void remove(@NotNull Player player) {
        if (playerMap.remove(player.getUniqueId()) == null) return;
        remove0(player);
    }
    private void remove0(@NotNull Player player) {
        var bundler = ModelRenderer.inst().nms().createBundler();
        entityMap.values().forEach(e -> e.remove(bundler));
        bundler.send(player);
    }

    public int viewedPlayerSize() {
        return playerMap.size();
    }

    public @NotNull List<Player> viewedPlayer() {
        return new ArrayList<>(playerMap.values());
    }

}

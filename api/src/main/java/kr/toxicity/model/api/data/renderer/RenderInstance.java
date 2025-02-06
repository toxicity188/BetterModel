package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.blueprint.AnimationMovement;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.entity.RenderedEntity;
import kr.toxicity.model.api.entity.TrackerMovement;
import kr.toxicity.model.api.nms.HitBoxListener;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.script.ScriptProcessor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A manager of each tracker.
 */
@ApiStatus.Internal
public final class RenderInstance implements AutoCloseable {
    @Getter
    private final BlueprintRenderer parent;

    private final Map<String, RenderedEntity> entityMap;
    private final Map<String, BlueprintAnimation> animationMap;
    private final Map<UUID, PlayerChannelHandler> playerMap = new ConcurrentHashMap<>();
    private Predicate<Player> filter = p -> true;
    private Predicate<Player> spawnFilter = p -> !playerMap.containsKey(p.getUniqueId());

    @Getter
    private final ScriptProcessor scriptProcessor = new ScriptProcessor();

    public RenderInstance(@NotNull BlueprintRenderer parent, @NotNull Map<String, RenderedEntity> entityMap, @NotNull Map<String, BlueprintAnimation> animationMap) {
        this.parent = parent;
        this.entityMap = entityMap;
        this.animationMap = animationMap;

        animateLoop("idle");
    }

    public void filter(@NotNull Predicate<Player> filter) {
        this.filter = this.filter.and(filter);
    }

    public @NotNull Predicate<Player> spawnFilter() {
        return spawnFilter;
    }

    public void spawnFilter(@NotNull Predicate<Player> spawnFilter) {
        this.spawnFilter = this.spawnFilter.and(spawnFilter);
    }

    public void createHitBox(@NotNull Entity entity, @NotNull Predicate<RenderedEntity> predicate, @Nullable HitBoxListener listener) {
        for (RenderedEntity value : entityMap.values()) {
            value.createHitBox(entity, predicate, listener);
        }
    }

    @Override
    public void close() throws Exception {
        for (RenderedEntity value : entityMap.values()) {
            value.close();
        }
        for (PlayerChannelHandler value : playerMap.values()) {
            remove0(value.player());
        }
        playerMap.clear();
    }

    public void teleport(@NotNull Location location, @NotNull PacketBundler bundler) {
        entityMap.values().forEach(e -> e.teleport(location, bundler));
    }

    public void move(@NotNull TrackerMovement movement, @NotNull PacketBundler bundler) {
        entityMap.values().forEach(e -> e.move(movement, bundler));
    }

    public void lastMovement(@NotNull TrackerMovement movement) {
        for (RenderedEntity value : entityMap.values()) {
            value.lastMovement(movement);
        }
    }

    public void defaultPosition(@NotNull Vector3f movement) {
        for (RenderedEntity value : entityMap.values()) {
            value.defaultPosition(new Vector3f(movement).add(value.getGroup().getPosition()));
        }
    }

    public void itemStack(@NotNull Predicate<RenderedEntity> predicate, @NotNull ItemStack itemStack) {
        for (RenderedEntity value : entityMap.values()) {
            value.itemStack(predicate, itemStack);
        }
    }

    public void setup(@NotNull TrackerMovement movement) {
        for (RenderedEntity value : entityMap.values()) {
            value.setup(movement);
        }
    }

    public boolean addAnimationMovementModifier(@NotNull Consumer<AnimationMovement> consumer) {
        return addAnimationMovementModifier(r -> true, consumer);
    }
    public boolean addAnimationMovementModifier(@NotNull Predicate<RenderedEntity> predicate, @NotNull Consumer<AnimationMovement> consumer) {
        var ret = false;
        for (RenderedEntity value : entityMap.values()) {
            if (value.addAnimationMovementModifier(predicate, consumer)) ret = true;
        }
        return ret;
    }

    public @NotNull List<RenderedEntity> renderers() {
        var list = new ArrayList<RenderedEntity>();
        entityMap.values().forEach(e -> e.renderers(list));
        return list;
    }

    public double height() {
        return entityMap.values().stream().mapToDouble(RenderedEntity::height).max().orElse(0.0);
    }

    public void tint(boolean toggle) {
        var bundler = BetterModel.inst().nms().createBundler();
        entityMap.values().forEach(e -> e.tint(toggle, bundler));
        if (!bundler.isEmpty()) for (Player player : viewedPlayer()) {
            bundler.send(player);
        }
    }


    public boolean animateLoop(@NotNull String animation) {
        return animateLoop(e -> true, animation, AnimationModifier.DEFAULT, () -> {});
    }

    public boolean animateSingle(@NotNull String animation) {
        return animateSingle(e -> true, animation, AnimationModifier.DEFAULT, () -> {});
    }
    public boolean animateLoop(@NotNull String animation, AnimationModifier modifier) {
        return animateLoop(e -> true, animation, modifier, () -> {});
    }

    public boolean animateSingle(@NotNull String animation, AnimationModifier modifier) {
        return animateSingle(e -> true, animation, modifier, () -> {});
    }

    public boolean animateLoop(@NotNull Predicate<RenderedEntity> filter, @NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        scriptProcessor.animateLoop(get.script(), modifier);
        for (RenderedEntity value : entityMap.values()) {
            value.addLoop(filter, animation, get, modifier, removeTask);
        }
        return true;
    }

    public boolean animateSingle(@NotNull Predicate<RenderedEntity> filter, @NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        scriptProcessor.animateSingle(get.script(), modifier);
        for (RenderedEntity value : entityMap.values()) {
            value.addSingle(filter, animation, get, modifier, removeTask);
        }
        return true;
    }

    public boolean replaceLoop(@NotNull Predicate<RenderedEntity> filter, @NotNull String target, @NotNull String animation) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        scriptProcessor.replaceLoop(get.script(), AnimationModifier.DEFAULT);
        for (RenderedEntity value : entityMap.values()) {
            value.replaceLoop(filter, target, animation, get);
        }
        return true;
    }

    public boolean replaceSingle(@NotNull Predicate<RenderedEntity> filter, @NotNull String target, @NotNull String animation) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        scriptProcessor.replaceSingle(get.script(), AnimationModifier.DEFAULT);
        for (RenderedEntity value : entityMap.values()) {
            value.replaceSingle(filter, target, animation, get);
        }
        return true;
    }

    public void stopAnimation(@NotNull Predicate<RenderedEntity> filter, @NotNull String target) {
        scriptProcessor.stopAnimation(target);
        for (RenderedEntity value : entityMap.values()) {
            value.stopAnimation(filter, target);
        }
    }

    public void spawn(@NotNull Player player, @NotNull PacketBundler bundler) {
        var get = BetterModel.inst().playerManager().player(player.getUniqueId());
        if (get == null) return;
        playerMap.computeIfAbsent(player.getUniqueId(), u -> {
            entityMap.values().forEach(e -> e.spawn(bundler));
            return get;
        });
    }
    public void remove(@NotNull Player player) {
        if (playerMap.remove(player.getUniqueId()) == null) return;
        remove0(player);
    }
    private void remove0(@NotNull Player player) {
        var bundler = BetterModel.inst().nms().createBundler();
        entityMap.values().forEach(e -> e.remove(bundler));
        bundler.send(player);
    }

    public void togglePart(@NotNull Predicate<RenderedEntity> predicate, boolean toggle) {
        var bundler = BetterModel.inst().nms().createBundler();
        entityMap.values().forEach(e -> e.togglePart(bundler, predicate, toggle));
        if (!bundler.isEmpty()) for (Player player : viewedPlayer()) {
            bundler.send(player);
        }
    }

    public int viewedPlayerSize() {
        return playerMap.size();
    }

    public @NotNull List<PlayerChannelHandler> allPlayer() {
        return new ArrayList<>(playerMap.values());
    }
    public @NotNull List<Player> viewedPlayer() {
        return viewedPlayer(filter);
    }
    public @NotNull List<Player> viewedPlayer(@NotNull Predicate<Player> predicate) {
        return playerMap.values().stream().map(PlayerChannelHandler::player).filter(predicate).toList();
    }

}

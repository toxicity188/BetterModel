package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.tracker.TrackerMovement;
import kr.toxicity.model.api.nms.EntityAdapter;
import kr.toxicity.model.api.nms.HitBoxListener;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.script.ScriptProcessor;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.util.BonePredicate;
import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.TransformedItemStack;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A manager of each tracker.
 */
@ApiStatus.Internal
public final class RenderInstance {
    @Getter
    private final BlueprintRenderer parent;

    private final Map<String, RenderedBone> entityMap;
    private final Map<String, BlueprintAnimation> animationMap;
    private final Map<UUID, PlayerChannelHandler> playerMap = new ConcurrentHashMap<>();
    private Predicate<Player> viewFilter = p -> true;
    private Predicate<Player> spawnFilter = OfflinePlayer::isOnline;

    private Consumer<PacketBundler> spawnPacketHandler = b -> {};
    private Consumer<PacketBundler> despawnPacketHandler = b -> {};

    @Getter
    private ModelRotation rotation = ModelRotation.EMPTY;

    @Getter
    private final ScriptProcessor scriptProcessor = new ScriptProcessor();

    public RenderInstance(@NotNull BlueprintRenderer parent, @NotNull Map<String, RenderedBone> entityMap, @NotNull Map<String, BlueprintAnimation> animationMap) {
        this.parent = parent;
        this.entityMap = entityMap;
        this.animationMap = animationMap;

        animateLoop("idle");
    }

    public void viewFilter(@NotNull Predicate<Player> filter) {
        this.viewFilter = this.viewFilter.and(FunctionUtil.throttleTick(filter));
    }

    public void spawnPacketHandler(@NotNull Consumer<PacketBundler> spawnPacketHandler) {
        this.spawnPacketHandler = this.spawnPacketHandler.andThen(spawnPacketHandler);
    }

    public void despawnPacketHandler(@NotNull Consumer<PacketBundler> despawnPacketHandler) {
        this.despawnPacketHandler = this.despawnPacketHandler.andThen(despawnPacketHandler);
    }

    public @NotNull Predicate<Player> spawnFilter() {
        return spawnFilter;
    }

    public void spawnFilter(@NotNull Predicate<Player> spawnFilter) {
        this.spawnFilter = this.spawnFilter.and(spawnFilter);
    }

    public void createHitBox(@NotNull EntityAdapter entity, @NotNull Predicate<RenderedBone> predicate, @Nullable HitBoxListener listener) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.createHitBox(entity, predicate, listener));
        }
    }

    public @Nullable String runningAnimation() {
        for (RenderedBone value : entityMap.values()) {
            var get = value.findNotNullByTree(RenderedBone::runningAnimation);
            if (get != null) return get;
        }
        return null;
    }

    public void despawn() {
        for (RenderedBone value : entityMap.values()) {
            value.despawn();
        }
        var bundler = BetterModel.inst().nms().createBundler();
        remove0(bundler);
        if (!bundler.isEmpty()) for (PlayerChannelHandler value : playerMap.values()) {
            bundler.send(value.player());
        }
        playerMap.clear();
    }

    public void teleport(@NotNull Location location, @NotNull PacketBundler bundler) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.teleport(location, bundler));
        }
    }

    public void move(@Nullable ModelRotation rotation, @NotNull TrackerMovement movement, @NotNull PacketBundler bundler) {
        var rot = rotation == null || rotation.equals(this.rotation) ? null : (this.rotation = rotation);
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.move(rot, movement, bundler));
        }
    }

    public void defaultPosition(@NotNull Vector3f movement) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.defaultPosition(new Vector3f(movement).add(value.getGroup().getPosition())));
        }
    }

    public void forceUpdate(@NotNull PacketBundler bundler) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.forceUpdate(bundler));
        }
    }

    public boolean itemStack(@NotNull BonePredicate predicate, @NotNull TransformedItemStack itemStack) {
        return anyMatch(predicate, (b, p) -> b.itemStack(p, itemStack));
    }

    public boolean brightness(@NotNull BonePredicate predicate, int block, int sky) {
        return anyMatch(predicate, (b, p) -> b.brightness(p, block, sky));
    }

    public boolean addAnimationMovementModifier(@NotNull BonePredicate predicate, @NotNull Consumer<AnimationMovement> consumer) {
        return anyMatch(predicate, (b, p) -> b.addAnimationMovementModifier(p, consumer));
    }

    public @NotNull List<RenderedBone> renderers() {
        var list = new ArrayList<RenderedBone>();
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(list::add);
        }
        return list;
    }

    public @Nullable RenderedBone boneOf(@NotNull String name) {
        for (RenderedBone value : entityMap.values()) {
            var get = value.boneOf(e -> e.getName().equals(name));
            if (get != null) return get;
        }
        return null;
    }

    public double height() {
        var h = 0D;
        for (RenderedBone renderer : renderers()) {
            var lt = renderer.worldPosition().y;
            if (renderer.getName().startsWith("h_")) return lt;
            if (h < lt) h = lt;
        }
        return h;
    }

    public boolean tint(@NotNull BonePredicate predicate, int rgb) {
        return anyMatch(predicate, (b, p) -> b.tint(p, rgb));
    }

    public boolean enchant(@NotNull BonePredicate predicate, boolean enchant) {
        return anyMatch(predicate, (b, p) -> b.enchant(p, enchant));
    }

    public boolean animateLoop(@NotNull String animation) {
        return animateLoop(e -> true, animation, AnimationModifier.DEFAULT_LOOP, () -> {});
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

    public boolean animateLoop(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        scriptProcessor.animateLoop(get.script(), modifier);
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.addLoop(filter, animation, get, modifier, FunctionUtil.throttleTick(removeTask)));
        }
        return true;
    }

    public boolean animateSingle(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        scriptProcessor.animateSingle(get.script(), modifier);
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.addSingle(filter, animation, get, modifier, FunctionUtil.throttleTick(removeTask)));
        }
        return true;
    }

    public boolean replaceLoop(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull String animation) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        scriptProcessor.replaceLoop(get.script(), AnimationModifier.DEFAULT_LOOP);
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.replaceLoop(filter, target, animation, get));
        }
        return true;
    }

    public boolean replaceSingle(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull String animation) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        scriptProcessor.replaceSingle(get.script(), AnimationModifier.DEFAULT);
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.replaceSingle(filter, target, animation, get));
        }
        return true;
    }

    public void stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String target) {
        scriptProcessor.stopAnimation(target);
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.stopAnimation(filter, target));
        }
    }

    public void spawn(@NotNull Player player, @NotNull PacketBundler bundler) {
        var get = BetterModel.inst().playerManager().player(player.getUniqueId());
        if (get == null) return;
        if (playerMap.get(player.getUniqueId()) != null || spawnFilter.test(player)) {
            spawnPacketHandler.accept(bundler);
            for (RenderedBone value : entityMap.values()) {
                value.iterateTree(b -> b.spawn(bundler));
            }
            playerMap.put(player.getUniqueId(), get);
        }
    }

    public void remove(@NotNull Player player) {
        if (playerMap.remove(player.getUniqueId()) == null) return;
        var bundler = BetterModel.inst().nms().createBundler();
        remove0(bundler);
        bundler.send(player);
    }

    private void remove0(@NotNull PacketBundler bundler) {
        despawnPacketHandler.accept(bundler);
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.remove(bundler));
        }
    }

    public boolean togglePart(@NotNull BonePredicate predicate, boolean toggle) {
        return anyMatch(predicate, (b, p) -> b.togglePart(p, toggle));
    }

    private boolean anyMatch(@NotNull BonePredicate predicate, BiFunction<RenderedBone, BonePredicate, Boolean> mapper) {
        var result = false;
        for (RenderedBone value : entityMap.values()) {
            if (value.iterateTree(predicate, mapper)) result = true;
        }
        return result;
    }

    public int playerCount() {
        return playerMap.size();
    }

    public @NotNull Stream<PlayerChannelHandler> allPlayer() {
        return playerMap.values().stream();
    }
    public @NotNull Stream<Player> viewedPlayer() {
        return viewedPlayer(viewFilter);
    }
    public @NotNull Stream<Player> viewedPlayer(@NotNull Predicate<Player> predicate) {
        return allPlayer().map(PlayerChannelHandler::player).filter(predicate);
    }

}

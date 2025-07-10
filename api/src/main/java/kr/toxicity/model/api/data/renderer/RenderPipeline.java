package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.animation.AnimationPredicate;
import kr.toxicity.model.api.animation.AnimationStateHandler;
import kr.toxicity.model.api.animation.RunningAnimation;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTags;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.nms.EntityAdapter;
import kr.toxicity.model.api.nms.HitBoxListener;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.script.AnimationScript;
import kr.toxicity.model.api.script.TimeScript;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.TransformedItemStack;
import kr.toxicity.model.api.util.function.BonePredicate;
import kr.toxicity.model.api.util.function.FloatSupplier;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A pipeline class of each tracker.
 */
@ApiStatus.Internal
public final class RenderPipeline {
    @Getter
    private final ModelRenderer parent;
    @Getter
    private final RenderSource<?> source;

    private final Map<BoneName, RenderedBone> entityMap;
    private final List<RenderedBone> bones;
    @Getter
    private final int displayAmount;
    private final Map<String, BlueprintAnimation> animationMap;
    private final Map<UUID, PlayerChannelHandler> playerMap = new ConcurrentHashMap<>();
    private final Set<UUID> hidePlayerSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private Predicate<Player> viewFilter = p -> true;
    private Predicate<Player> hideFilter = p -> hidePlayerSet.contains(p.getUniqueId());

    private Consumer<PacketBundler> spawnPacketHandler = b -> {};
    private Consumer<PacketBundler> despawnPacketHandler = b -> {};
    private Consumer<PacketBundler> hidePacketHandler = b -> {};
    private Consumer<PacketBundler> showPacketHandler = b -> {};

    @Getter
    private ModelRotation rotation = ModelRotation.INVALID;

    @Getter
    private final AnimationStateHandler<TimeScript> scriptProcessor = new AnimationStateHandler<>(
            TimeScript.EMPTY,
            (a, s, t) -> s == AnimationStateHandler.MappingState.PROGRESS ? a.time(t) : AnimationScript.EMPTY.time(t),
            s -> {
                if (s == null) return;
                if (s.isSync()) {
                    BetterModel.plugin().scheduler().task(getSource().location(), () -> s.accept(getSource()));
                } else s.accept(getSource());
            }
    );

    public RenderPipeline(
            @NotNull ModelRenderer parent,
            @NotNull RenderSource<?> source,
            @NotNull Map<BoneName, RenderedBone> entityMap
    ) {
        this.parent = parent;
        this.source = source;
        this.entityMap = entityMap;
        this.animationMap = parent.animationMap();

        var b = new ArrayList<RenderedBone>();
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b::add);
        }
        bones = Collections.unmodifiableList(b);
        displayAmount = (int) bones.stream()
                .filter(rb -> rb.getDisplay() != null)
                .count();
        animate("idle", new AnimationModifier(6, 0, 1));
    }

    public @NotNull PacketBundler createBundler() {
        return BetterModel.plugin().nms().createBundler(displayAmount);
    }

    public void viewFilter(@NotNull Predicate<Player> filter) {
        this.viewFilter = this.viewFilter.and(Objects.requireNonNull(filter));
    }
    public void hideFilter(@NotNull Predicate<Player> filter) {
        this.hideFilter = this.hideFilter.and(Objects.requireNonNull(filter));
    }

    public void spawnPacketHandler(@NotNull Consumer<PacketBundler> spawnPacketHandler) {
        this.spawnPacketHandler = this.spawnPacketHandler.andThen(Objects.requireNonNull(spawnPacketHandler));
    }

    public void despawnPacketHandler(@NotNull Consumer<PacketBundler> despawnPacketHandler) {
        this.despawnPacketHandler = this.despawnPacketHandler.andThen(Objects.requireNonNull(despawnPacketHandler));
    }
    public void hidePacketHandler(@NotNull Consumer<PacketBundler> despawnPacketHandler) {
        this.hidePacketHandler = this.hidePacketHandler.andThen(Objects.requireNonNull(despawnPacketHandler));
    }
    public void showPacketHandler(@NotNull Consumer<PacketBundler> despawnPacketHandler) {
        this.showPacketHandler = this.showPacketHandler.andThen(Objects.requireNonNull(despawnPacketHandler));
    }

    public boolean isSpawned(@NotNull UUID uuid) {
        return playerMap.containsKey(uuid);
    }

    public void createHitBox(@NotNull EntityAdapter entity, @NotNull Predicate<RenderedBone> predicate, @Nullable HitBoxListener listener) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.createHitBox(entity, predicate, listener));
        }
    }

    public @Nullable RunningAnimation runningAnimation() {
        for (RenderedBone value : entityMap.values()) {
            var get = value.findNotNullByTree(RenderedBone::runningAnimation);
            if (get != null) return get;
        }
        return null;
    }

    public @NotNull String name() {
        return parent.name();
    }

    public void despawn() {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> {
                var hb = b.getHitBox();
                if (hb != null) hb.removeHitBox();
            });
        }
        var bundler = createBundler();
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

    public boolean rotate(@NotNull ModelRotation rotation, @NotNull PacketBundler bundler) {
        if (rotation.equals(this.rotation)) return false;
        var rot = this.rotation = rotation;
        var match = false;
        for (RenderedBone value : entityMap.values()) {
            if (value.matchTree(b -> b.rotate(rot, bundler))) match = true;
        }
        return match;
    }

    public boolean tick(@NotNull PacketBundler bundler) {
        var match = false;
        for (RenderedBone value : entityMap.values()) {
            if (value.matchTree(b -> b.tick(bundler))) match = true;
        }
        return match;
    }

    public void defaultPosition(@NotNull Supplier<Vector3f> movement) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.defaultPosition(movement));
        }
    }

    public void forceUpdate(@NotNull PacketBundler bundler) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.forceUpdate(bundler));
        }
    }
    public void forceUpdate(boolean showItem, @NotNull PacketBundler bundler) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.forceUpdate(showItem, bundler));
        }
    }

    public void scale(@NotNull FloatSupplier scale) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.scale(scale));
        }
    }

    public boolean itemStack(@NotNull BonePredicate predicate, @NotNull TransformedItemStack itemStack) {
        return anyMatch(predicate, (b, p) -> b.itemStack(p, itemStack));
    }

    public boolean updateItem(@NotNull BonePredicate predicate) {
        return anyMatch(predicate, (b, p) -> b.updateItem(p, source));
    }

    public boolean glow(@NotNull BonePredicate predicate, boolean glow, int glowColor) {
        return anyMatch(predicate, (b, p) -> b.glow(p, glow, glowColor));
    }

    public boolean brightness(@NotNull BonePredicate predicate, int block, int sky) {
        return anyMatch(predicate, (b, p) -> b.brightness(p, block, sky));
    }

    public boolean addRotationModifier(@NotNull BonePredicate predicate, @NotNull Function<Quaternionf, Quaternionf> mapper) {
        return anyMatch(predicate, (b, p) -> b.addRotationModifier(p, mapper));
    }

    public boolean addPositionModifier(@NotNull BonePredicate predicate, @NotNull Function<Vector3f, Vector3f> mapper) {
        return anyMatch(predicate, (b, p) -> b.addPositionModifier(p, mapper));
    }

    public @NotNull @Unmodifiable List<RenderedBone> bones() {
        return bones;
    }

    public @Nullable RenderedBone boneOf(@NotNull Predicate<RenderedBone> predicate) {
        for (RenderedBone value : entityMap.values()) {
            var get = value.boneOf(predicate);
            if (get != null) return get;
        }
        return null;
    }

    public double height() {
        var h = 0D;
        for (RenderedBone renderer : bones()) {
            var lt = renderer.hitBoxPosition().y;
            if (renderer.getName().tagged(BoneTags.HEAD, BoneTags.HEAD_WITH_CHILDREN)) return lt;
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

    public void moveDuration(int duration) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.moveDuration(duration));
        }
    }

    public boolean animate(@NotNull String animation) {
        return animate(e -> true, animation, AnimationModifier.DEFAULT, () -> {});
    }

    public boolean animate(@NotNull String animation, @NotNull AnimationModifier modifier) {
        return animate(e -> true, animation, modifier, () -> {});
    }

    public void animate(@NotNull Predicate<RenderedBone> filter, @NotNull BlueprintAnimation animation, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        var script = animation.script();
        if (script != null) scriptProcessor.addAnimation(animation.name(), script.iterator(), modifier, () -> {});
        var playOnceTask = FunctionUtil.playOnce(removeTask);
        var animationPredicate = AnimationPredicate.of(filter);
        for (RenderedBone value : entityMap.values()) {
            value.iterateAnimation(animationPredicate, (b, a) -> b.addAnimation(a, animation, modifier, playOnceTask));
        }
    }

    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        animate(filter, get, modifier, removeTask);
        return true;
    }

    public boolean replace(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull String animation, @NotNull AnimationModifier modifier) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        replace(filter, target, get, modifier);
        return true;
    }

    public void replace(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull BlueprintAnimation animation, @NotNull AnimationModifier modifier) {
        var script = animation.script();
        if (script != null) scriptProcessor.replaceAnimation(target, script.iterator(), modifier);
        var animationPredicate = AnimationPredicate.of(filter);
        for (RenderedBone value : entityMap.values()) {
            value.iterateAnimation(animationPredicate, (b, a) -> b.replaceAnimation(a, target, animation, modifier));
        }
    }

    public void stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String target) {
        scriptProcessor.stopAnimation(target);
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.stopAnimation(filter, target));
        }
    }

    public boolean spawn(@NotNull Player player, @NotNull PacketBundler bundler) {
        var get = BetterModel.plugin().playerManager().player(player.getUniqueId());
        if (get == null) return false;
        playerMap.put(player.getUniqueId(), get);
        spawnPacketHandler.accept(bundler);
        var hided = isHide(player);
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.spawn(hided, bundler));
        }
        return true;
    }

    public boolean remove(@NotNull Player player) {
        if (playerMap.remove(player.getUniqueId()) == null) return false;
        var bundler = createBundler();
        remove0(bundler);
        bundler.send(player);
        return true;
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

    private boolean anyMatch(@NotNull BonePredicate predicate, BiPredicate<RenderedBone, BonePredicate> mapper) {
        var result = false;
        for (RenderedBone value : entityMap.values()) {
            if (value.iterateTree(predicate, mapper)) result = true;
        }
        return result;
    }

    public int playerCount() {
        return playerMap.size();
    }

    public @NotNull Stream<Player> allPlayer() {
        return playerMap.values().stream()
                .map(PlayerChannelHandler::player);
    }
    public @NotNull Stream<Player> nonHidePlayer() {
        return filteredPlayer(p -> !isHide(p));
    }
    public @NotNull Stream<Player> viewedPlayer() {
        return filteredPlayer(viewFilter);
    }
    public @NotNull Stream<Player> filteredPlayer(@NotNull Predicate<Player> predicate) {
        return allPlayer().filter(predicate);
    }

    public boolean hide(@NotNull Player player) {
        if (hidePlayerSet.add(player.getUniqueId())) {
            if (playerMap.containsKey(player.getUniqueId())) {
                var bundler = createBundler();
                forceUpdate(false, bundler);
                hidePacketHandler.accept(bundler);
                if (!bundler.isEmpty()) bundler.send(player);
            }
            BetterModel.plugin().scheduler().task(player, () -> {
                for (RenderedBone bone : bones) {
                    var hb = bone.getHitBox();
                    if (hb != null) hb.hide(player);
                }
            });
            return true;
        } else return false;
    }

    public boolean isHide(@NotNull Player player) {
        return hideFilter.test(player);
    }

    public boolean show(@NotNull Player player) {
        if (hidePlayerSet.remove(player.getUniqueId())) {
            if (playerMap.containsKey(player.getUniqueId())) {
                var bundler = createBundler();
                forceUpdate(true, bundler);
                showPacketHandler.accept(bundler);
                if (!bundler.isEmpty()) bundler.send(player);
            }
            BetterModel.plugin().scheduler().task(player, () -> {
                for (RenderedBone bone : bones) {
                    var hb = bone.getHitBox();
                    if (hb != null) hb.show(player);
                }
            });
            return true;
        } else return false;
    }
}

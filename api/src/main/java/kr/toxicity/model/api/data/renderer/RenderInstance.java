package kr.toxicity.model.api.data.renderer;

import com.destroystokyo.paper.profile.PlayerProfile;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.animation.AnimationMovement;
import kr.toxicity.model.api.animation.AnimationPredicate;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTag;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.nms.EntityAdapter;
import kr.toxicity.model.api.nms.HitBoxListener;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.player.PlayerLimb;
import kr.toxicity.model.api.script.ScriptProcessor;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.util.BonePredicate;
import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.TransformedItemStack;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A manager of each tracker.
 */
@ApiStatus.Internal
public final class RenderInstance {
    @Getter
    private final ModelRenderer parent;
    @Getter
    private final RenderSource source;

    private final Map<BoneName, RenderedBone> entityMap;
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

    public RenderInstance(
            @NotNull ModelRenderer parent,
            @NotNull RenderSource source,
            @NotNull Map<BoneName, RenderedBone> entityMap,
            @NotNull Map<String, BlueprintAnimation> animationMap
    ) {
        this.parent = parent;
        this.source = source;
        this.entityMap = entityMap;
        this.animationMap = animationMap;

        animate("idle", new AnimationModifier(6, 0, 1));
    }

    public void viewFilter(@NotNull Predicate<Player> filter) {
        this.viewFilter = this.viewFilter.and(filter);
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

    public @Nullable RenderedBone.RunningAnimation runningAnimation() {
        for (RenderedBone value : entityMap.values()) {
            var get = value.findNotNullByTree(RenderedBone::runningAnimation);
            if (get != null) return get;
        }
        return null;
    }

    public void despawn() {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> {
                var hb = b.getHitBox();
                if (hb != null) hb.removeHitBox();
            });
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

    public boolean move(@Nullable ModelRotation rotation, @NotNull PacketBundler bundler) {
        var rot = rotation == null || rotation.equals(this.rotation) ? null : (this.rotation = rotation);
        var match = false;
        for (RenderedBone value : entityMap.values()) {
            if (value.matchTree(b -> b.move(rot, bundler))) match = true;
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

    public void scale(@NotNull Supplier<Float> scale) {
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(b -> b.scale(scale));
        }
    }

    public boolean itemStack(@NotNull BonePredicate predicate, @NotNull TransformedItemStack itemStack) {
        return anyMatch(predicate, (b, p) -> b.itemStack(p, itemStack));
    }

    public boolean profile(@NotNull BonePredicate predicate, @NotNull Player player) {
        var profile = player.getPlayerProfile();
        var skinModel = profile.getTextures().getSkinModel();
        return profile(predicate, profile, skinModel);
    }

    public boolean profile(@NotNull BonePredicate predicate, @NotNull Player player, @NotNull PlayerTextures.SkinModel skinModel) {
        var profile = player.getPlayerProfile();
        return profile(predicate, profile, skinModel);
    }

    public boolean profile(@NotNull BonePredicate predicate, @NotNull PlayerProfile profile) {
        var skinModel = profile.getTextures().getSkinModel();
        return profile(predicate, profile, skinModel);
    }

    public boolean profile(@NotNull BonePredicate predicate, @NotNull PlayerProfile profile, @NotNull PlayerTextures.SkinModel skinModel) {
        return anyMatch(predicate, (b, p) -> {
            var mapper = b.getItemMapper();
            if (mapper instanceof PlayerLimb.LimbItemMapper limbItemMapper) {
                b.setItemMapper(limbItemMapper.profile(profile, skinModel));
                return b.updateItem(BonePredicate.TRUE, source);
            }
            return false;
        });
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

    public boolean addAnimationMovementModifier(@NotNull BonePredicate predicate, @NotNull Consumer<AnimationMovement> consumer) {
        return anyMatch(predicate, (b, p) -> b.addAnimationMovementModifier(p, consumer));
    }

    public @NotNull List<RenderedBone> bones() {
        var list = new ArrayList<RenderedBone>();
        for (RenderedBone value : entityMap.values()) {
            value.iterateTree(list::add);
        }
        return list;
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
            var lt = renderer.worldPosition().y;
            if (renderer.getName().tagged(BoneTag.HEAD)) return lt;
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

    public boolean animate(@NotNull String animation, AnimationModifier modifier) {
        return animate(e -> true, animation, modifier, () -> {});
    }

    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        scriptProcessor.animate(get.script(), get.loop(), modifier);
        for (RenderedBone value : entityMap.values()) {
            value.iterateAnimation(AnimationPredicate.of(filter), (b, a) -> b.addAnimation(a, animation, get, modifier, FunctionUtil.throttleTick(removeTask)));
        }
        return true;
    }

    public boolean replace(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull String animation, @NotNull AnimationModifier modifier) {
        var get = animationMap.get(animation);
        if (get == null) return false;
        scriptProcessor.replace(get.script(), get.loop(), modifier);
        for (RenderedBone value : entityMap.values()) {
            value.iterateAnimation(AnimationPredicate.of(filter), (b, a) -> b.replaceAnimation(a, target, animation, get, modifier));
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

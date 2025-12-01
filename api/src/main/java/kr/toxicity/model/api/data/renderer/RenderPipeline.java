/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationPredicate;
import kr.toxicity.model.api.animation.RunningAnimation;
import kr.toxicity.model.api.bone.*;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.util.function.BonePredicate;
import kr.toxicity.model.api.util.function.FloatSupplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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

import static kr.toxicity.model.api.util.CollectionUtil.associate;

/**
 * A pipeline class of each tracker.
 */
public final class RenderPipeline implements BoneEventHandler {

    @Getter
    private final ModelRenderer parent;
    @Getter
    private final RenderSource<?> source;

    private final Map<BoneName, RenderedBone> boneMap;
    private final Map<BoneName, RenderedBone> flattenBoneMap;
    private final int displayAmount;
    private final Map<UUID, SpawnedPlayer> playerMap = new ConcurrentHashMap<>();
    private final Set<UUID> hidePlayerSet = ConcurrentHashMap.newKeySet();

    private final BoneEventDispatcher eventDispatcher = new BoneEventDispatcher();
    private final BoneIKSolver ikSolver;

    private Predicate<Player> viewFilter = p -> true;
    private Predicate<Player> hideFilter = p -> hidePlayerSet.contains(p.getUniqueId());

    private Consumer<PacketBundler> spawnPacketHandler = b -> {};
    private Consumer<PacketBundler> despawnPacketHandler = b -> {};
    private Consumer<PacketBundler> hidePacketHandler = b -> {};
    private Consumer<PacketBundler> showPacketHandler = b -> {};

    @Getter
    private ModelRotation rotation = ModelRotation.INVALID;

    public RenderPipeline(
        @NotNull ModelRenderer parent,
        @NotNull RenderSource<?> source,
        @NotNull Map<BoneName, RenderedBone> boneMap
    ) {
        this.parent = parent;
        this.source = source;
        this.boneMap = boneMap;
        //Bone
        flattenBoneMap = associate(
            boneMap.values()
                .stream()
                .flatMap(RenderedBone::flatten)
                .peek(bone -> bone.extend(this)),
            RenderedBone::name
        );
        ikSolver = new BoneIKSolver(associate(flattenBoneMap.values(), RenderedBone::uuid));
        displayAmount = (int) flattenBoneMap.values().stream()
            .peek(bone -> bone.locator(ikSolver))
            .filter(rb -> rb.getDisplay() != null)
            .count();
    }

    public @NotNull PacketBundler createBundler() {
        return BetterModel.nms().createBundler(displayAmount + 1);
    }

    public @Nullable PlayerChannelHandler channel(@NotNull UUID uuid) {
        var get = playerMap.get(uuid);
        return get != null ? get.handler : null;
    }

    public @NotNull PacketBundler createLazyBundler() {
        return BetterModel.nms().createLazyBundler();
    }

    public @NotNull PacketBundler createParallelBundler() {
        var size = BetterModel.config().packetBundlingSize();
        return size <= 0 ? createBundler() : BetterModel.nms().createParallelBundler(size);
    }

    @Override
    public @NotNull BoneEventDispatcher eventDispatcher() {
        return eventDispatcher;
    }

    public void viewFilter(@NotNull Predicate<Player> filter) {
        this.viewFilter = this.viewFilter.and(Objects.requireNonNull(filter));
    }
    public void hideFilter(@NotNull Predicate<Player> filter) {
        this.hideFilter = this.hideFilter.or(Objects.requireNonNull(filter));
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

    public @Nullable RunningAnimation runningAnimation() {
        return firstNotNull(RenderedBone::runningAnimation);
    }

    public @NotNull String name() {
        return parent.name();
    }

    public void despawn() {
        hitboxes().forEach(HitBox::removeHitBox);
        var bundler = createBundler();
        remove0(bundler);
        if (bundler.isNotEmpty()) allPlayer().forEach(bundler::send);
        playerMap.clear();
    }

    public boolean rotate(@NotNull ModelRotation rotation, @NotNull PacketBundler bundler) {
        if (rotation.equals(this.rotation)) return false;
        this.rotation = rotation;
        return matchTree(b -> b.rotate(rotation, bundler));
    }

    public boolean tick(@NotNull PacketBundler bundler) {
        var match = matchTree(RenderedBone::tick);
        if (match) {
            ikSolver.solve();
            iterateTree(b -> b.sendTransformation(null, bundler));
        }
        return match;
    }

    public boolean tick(@NotNull UUID uuid, @NotNull PacketBundler bundler) {
        var match = matchTree(b -> b.tick(uuid));
        if (match) {
            ikSolver.solve(uuid);
            iterateTree(b -> b.sendTransformation(uuid, bundler));
        }
        return match;
    }

    public void defaultPosition(@NotNull Supplier<Vector3f> movement) {
        iterateTree(b -> b.defaultPosition(movement));
    }

    public void scale(@NotNull FloatSupplier scale) {
        iterateTree(b -> b.scale(scale));
    }

    public boolean addRotationModifier(@NotNull BonePredicate predicate, @NotNull Function<Quaternionf, Quaternionf> mapper) {
        return matchTree(predicate, (b, p) -> b.addRotationModifier(p, mapper));
    }

    public boolean addPositionModifier(@NotNull BonePredicate predicate, @NotNull Function<Vector3f, Vector3f> mapper) {
        return matchTree(predicate, (b, p) -> b.addPositionModifier(p, mapper));
    }

    public @NotNull @Unmodifiable Collection<RenderedBone> bones() {
        return flattenBoneMap.values();
    }

    public @NotNull Stream<HitBox> hitboxes() {
        return bones()
            .stream()
            .map(RenderedBone::getHitBox)
            .filter(Objects::nonNull);
    }

    public @Nullable RenderedBone boneOf(@NotNull BoneName name) {
        return flattenBoneMap.get(name);
    }

    @ApiStatus.Internal
    public boolean spawn(@NotNull Player player, @NotNull PacketBundler bundler, @NotNull Consumer<SpawnedPlayer> consumer) {
        var get = BetterModel.plugin().playerManager().player(player.getUniqueId());
        if (get == null) return false;
        var spawnedPlayer = new SpawnedPlayer(get);
        playerMap.put(player.getUniqueId(), spawnedPlayer);
        spawnPacketHandler.accept(bundler);
        var hided = isHide(player);
        iterateTree(b -> b.spawn(hided, bundler));
        consumer.accept(spawnedPlayer);
        return true;
    }

    @ApiStatus.Internal
    public boolean remove(@NotNull Player player) {
        if (playerMap.remove(player.getUniqueId()) == null) return false;
        var bundler = createBundler();
        remove0(bundler);
        bundler.send(player);
        return true;
    }

    @ApiStatus.Internal
    private void remove0(@NotNull PacketBundler bundler) {
        despawnPacketHandler.accept(bundler);
        iterateTree(b -> b.remove(bundler));
    }

    public boolean matchTree(@NotNull BonePredicate predicate, BiPredicate<RenderedBone, BonePredicate> mapper) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(mapper);
        var result = false;
        for (RenderedBone value : boneMap.values()) {
            if (value.matchTree(predicate, mapper)) result = true;
        }
        return result;
    }

    public boolean matchTree(@NotNull AnimationPredicate predicate, BiPredicate<RenderedBone, AnimationPredicate> mapper) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(mapper);
        var result = false;
        for (RenderedBone value : boneMap.values()) {
            if (value.matchTree(predicate, mapper)) result = true;
        }
        return result;
    }

    public boolean matchTree(@NotNull Predicate<RenderedBone> predicate) {
        Objects.requireNonNull(predicate);
        var result = false;
        for (RenderedBone value : boneMap.values()) {
            if (value.matchTree(predicate)) result = true;
        }
        return result;
    }

    public void iterateTree(@NotNull Consumer<RenderedBone> consumer) {
        Objects.requireNonNull(consumer);
        for (RenderedBone value : boneMap.values()) {
            value.iterateTree(consumer);
        }
    }

    public <T> @Nullable T firstNotNull(@NotNull Function<RenderedBone, T> mapper) {
        return bones()
            .stream()
            .map(mapper)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    public int playerCount() {
        return playerMap.size();
    }

    public @NotNull Stream<Player> allPlayer() {
        return playerMap.values()
            .stream()
            .map(spawned -> spawned.handler.player());
    }

    public @NotNull Stream<Player> nonHidePlayer() {
        return playerMap.values()
            .stream()
            .filter(spawned -> spawned.initialLoad)
            .map(spawned -> spawned.handler.player())
            .filter(viewFilter);
    }
    public @NotNull Stream<Player> viewedPlayer() {
        return allPlayer().filter(viewFilter);
    }

    public boolean hide(@NotNull Player player) {
        if (isHide(player) || !hidePlayerSet.add(player.getUniqueId())) return false;
        if (isSpawned(player.getUniqueId())) {
            var bundler = createBundler();
            iterateTree(b -> b.forceUpdate(false, bundler));
            hidePacketHandler.accept(bundler);
            if (bundler.isNotEmpty()) bundler.send(player);
        }
        BetterModel.plugin().scheduler().task(player, () -> hitboxes().forEach(hb -> hb.hide(player)));
        return true;
    }

    public boolean isHide(@NotNull Player player) {
        return hideFilter.test(player);
    }

    public boolean show(@NotNull Player player) {
        if (!isHide(player) || !hidePlayerSet.remove(player.getUniqueId())) return false;
        if (isSpawned(player.getUniqueId())) {
            var bundler = createBundler();
            iterateTree(b -> b.forceUpdate(true, bundler));
            showPacketHandler.accept(bundler);
            if (bundler.isNotEmpty()) bundler.send(player);
        }
        BetterModel.plugin().scheduler().task(player, () -> hitboxes().forEach(hb -> hb.show(player)));
        return true;
    }

    @RequiredArgsConstructor
    public class SpawnedPlayer {
        private final PlayerChannelHandler handler;
        private volatile boolean initialLoad;

        public void load() {
            if (isHide(handler.player())) return;
            var b = createBundler();
            iterateTree(bone -> bone.forceUpdate(b));
            if (b.isNotEmpty()) b.send(handler.player());
            initialLoad = true;
        }
    }
}

/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
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
 * Represents the rendering pipeline for a specific model instance.
 * <p>
 * This class manages the hierarchy of {@link RenderedBone}s, handles player visibility and packet bundling,
 * and coordinates animation updates and inverse kinematics (IK) solving.
 * </p>
 *
 * @since 1.15.2
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

    /**
     * Creates a new render pipeline.
     *
     * @param parent the parent model renderer
     * @param source the source of the rendering (entity or location)
     * @param boneMap the map of root bones
     * @since 1.15.2
     */
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

    /**
     * Creates a packet bundler for this pipeline.
     *
     * @return a new packet bundler
     * @since 1.15.2
     */
    public @NotNull PacketBundler createBundler() {
        return BetterModel.nms().createBundler(displayAmount + 1);
    }

    /**
     * Retrieves the channel handler for a specific player.
     *
     * @param uuid the UUID of the player
     * @return the channel handler, or null if not found
     * @since 1.15.2
     */
    public @Nullable PlayerChannelHandler channel(@NotNull UUID uuid) {
        var get = playerMap.get(uuid);
        return get != null ? get.handler : null;
    }

    /**
     * Creates a lazy packet bundler.
     *
     * @return a new lazy packet bundler
     * @since 1.15.2
     */
    public @NotNull PacketBundler createLazyBundler() {
        return BetterModel.nms().createLazyBundler();
    }

    /**
     * Creates a parallel packet bundler based on configuration.
     *
     * @return a new parallel packet bundler
     * @since 1.15.2
     */
    public @NotNull PacketBundler createParallelBundler() {
        var size = BetterModel.config().packetBundlingSize();
        return size <= 0 ? createBundler() : BetterModel.nms().createParallelBundler(size);
    }

    @Override
    public @NotNull BoneEventDispatcher eventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Adds a filter to restrict which players can view the model.
     *
     * @param filter the predicate to filter players
     * @since 1.15.2
     */
    public void viewFilter(@NotNull Predicate<Player> filter) {
        this.viewFilter = this.viewFilter.and(Objects.requireNonNull(filter));
    }

    /**
     * Adds a filter to determine if a player should be hidden from the model.
     *
     * @param filter the predicate to hide players
     * @since 1.15.2
     */
    public void hideFilter(@NotNull Predicate<Player> filter) {
        this.hideFilter = this.hideFilter.or(Objects.requireNonNull(filter));
    }

    /**
     * Adds a handler for spawn packets.
     *
     * @param spawnPacketHandler the consumer to handle spawn packets
     * @since 1.15.2
     */
    public void spawnPacketHandler(@NotNull Consumer<PacketBundler> spawnPacketHandler) {
        this.spawnPacketHandler = this.spawnPacketHandler.andThen(Objects.requireNonNull(spawnPacketHandler));
    }

    /**
     * Adds a handler for despawn packets.
     *
     * @param despawnPacketHandler the consumer to handle despawn packets
     * @since 1.15.2
     */
    public void despawnPacketHandler(@NotNull Consumer<PacketBundler> despawnPacketHandler) {
        this.despawnPacketHandler = this.despawnPacketHandler.andThen(Objects.requireNonNull(despawnPacketHandler));
    }

    /**
     * Adds a handler for hide packets.
     *
     * @param despawnPacketHandler the consumer to handle hide packets
     * @since 1.15.2
     */
    public void hidePacketHandler(@NotNull Consumer<PacketBundler> despawnPacketHandler) {
        this.hidePacketHandler = this.hidePacketHandler.andThen(Objects.requireNonNull(despawnPacketHandler));
    }

    /**
     * Adds a handler for show packets.
     *
     * @param despawnPacketHandler the consumer to handle show packets
     * @since 1.15.2
     */
    public void showPacketHandler(@NotNull Consumer<PacketBundler> despawnPacketHandler) {
        this.showPacketHandler = this.showPacketHandler.andThen(Objects.requireNonNull(despawnPacketHandler));
    }

    /**
     * Checks if the model is spawned for a specific player.
     *
     * @param uuid the UUID of the player
     * @return true if spawned, false otherwise
     * @since 1.15.2
     */
    public boolean isSpawned(@NotNull UUID uuid) {
        return playerMap.containsKey(uuid);
    }

    /**
     * Retrieves the currently running animation, if any.
     *
     * @return the running animation, or null if none
     * @since 1.15.2
     */
    public @Nullable RunningAnimation runningAnimation() {
        return firstNotNull(RenderedBone::runningAnimation);
    }

    /**
     * Returns the name of the model.
     *
     * @return the model name
     * @since 1.15.2
     */
    public @NotNull String name() {
        return parent.name();
    }

    /**
     * Despawns the model for all players and clears internal state.
     *
     * @since 1.15.2
     */
    public void despawn() {
        hitboxes().forEach(HitBox::removeHitBox);
        var bundler = createBundler();
        remove0(bundler);
        if (bundler.isNotEmpty()) allPlayer().forEach(bundler::send);
        playerMap.clear();
    }

    /**
     * Rotates the model to a new orientation.
     *
     * @param rotation the new rotation
     * @param bundler the packet bundler to use
     * @return true if the rotation changed, false otherwise
     * @since 1.15.2
     */
    public boolean rotate(@NotNull ModelRotation rotation, @NotNull PacketBundler bundler) {
        if (rotation.equals(this.rotation)) return false;
        this.rotation = rotation;
        return matchTree(b -> b.rotate(rotation, bundler));
    }

    /**
     * Ticks the model, updating animations and IK.
     *
     * @param bundler the packet bundler to use
     * @return true if any updates occurred
     * @since 1.15.2
     */
    public boolean tick(@NotNull PacketBundler bundler) {
        var match = matchTree(RenderedBone::tick);
        if (match) {
            ikSolver.solve();
            iterateTree(b -> b.sendTransformation(null, bundler));
        }
        return match;
    }

    /**
     * Ticks the model for a specific player (e.g., for per-player animations).
     *
     * @param uuid the UUID of the player
     * @param bundler the packet bundler to use
     * @return true if any updates occurred
     * @since 1.15.2
     */
    public boolean tick(@NotNull UUID uuid, @NotNull PacketBundler bundler) {
        var match = matchTree(b -> b.tick(uuid));
        if (match) {
            ikSolver.solve(uuid);
            iterateTree(b -> b.sendTransformation(uuid, bundler));
        }
        return match;
    }

    /**
     * Sets the default position modifier for all bones.
     *
     * @param movement the movement function
     * @since 1.15.2
     */
    public void defaultPosition(@NotNull Function<Vector3f, Vector3f> movement) {
        iterateTree(b -> b.defaultPosition(movement));
    }

    /**
     * Scales the model.
     *
     * @param scale the scale supplier
     * @since 1.15.2
     */
    public void scale(@NotNull FloatSupplier scale) {
        iterateTree(b -> b.scale(scale));
    }

    /**
     * Adds a rotation modifier to matching bones.
     *
     * @param predicate the predicate to select bones
     * @param mapper the rotation mapping function
     * @return true if any bones were modified
     * @since 1.15.2
     */
    public boolean addRotationModifier(@NotNull BonePredicate predicate, @NotNull Function<Quaternionf, Quaternionf> mapper) {
        return matchTree(predicate, (b, p) -> b.addRotationModifier(p, mapper));
    }

    /**
     * Adds a position modifier to matching bones.
     *
     * @param predicate the predicate to select bones
     * @param mapper the position mapping function
     * @return true if any bones were modified
     * @since 1.15.2
     */
    public boolean addPositionModifier(@NotNull BonePredicate predicate, @NotNull Function<Vector3f, Vector3f> mapper) {
        return matchTree(predicate, (b, p) -> b.addPositionModifier(p, mapper));
    }

    /**
     * Returns a collection of all bones in this pipeline.
     *
     * @return the collection of bones
     * @since 1.15.2
     */
    public @NotNull @Unmodifiable Collection<RenderedBone> bones() {
        return flattenBoneMap.values();
    }

    /**
     * Returns a stream of all hitboxes associated with this model.
     *
     * @return the stream of hitboxes
     * @since 1.15.2
     */
    public @NotNull Stream<HitBox> hitboxes() {
        return bones()
            .stream()
            .map(RenderedBone::getHitBox)
            .filter(Objects::nonNull);
    }

    /**
     * Retrieves a bone by its name.
     *
     * @param name the name of the bone
     * @return the rendered bone, or null if not found
     * @since 1.15.2
     */
    public @Nullable RenderedBone boneOf(@NotNull BoneName name) {
        return flattenBoneMap.get(name);
    }

    /**
     * Spawns the model for a player.
     *
     * @param player the player to spawn for
     * @param bundler the packet bundler to use
     * @param consumer a consumer for the spawned player object
     * @return true if spawned successfully
     * @since 1.15.2
     */
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

    /**
     * Removes the model for a player.
     *
     * @param player the player to remove for
     * @return true if removed successfully
     * @since 1.15.2
     */
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

    /**
     * Applies a mapper to bones matching a predicate.
     *
     * @param predicate the bone predicate
     * @param mapper the mapper function
     * @return true if any bones matched
     * @since 1.15.2
     */
    public boolean matchTree(@NotNull BonePredicate predicate, BiPredicate<RenderedBone, BonePredicate> mapper) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(mapper);
        var result = false;
        for (RenderedBone value : boneMap.values()) {
            if (value.matchTree(predicate, mapper)) result = true;
        }
        return result;
    }

    /**
     * Applies a mapper to bones matching an animation predicate.
     *
     * @param predicate the animation predicate
     * @param mapper the mapper function
     * @return true if any bones matched
     * @since 1.15.2
     */
    public boolean matchTree(@NotNull AnimationPredicate predicate, BiPredicate<RenderedBone, AnimationPredicate> mapper) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(mapper);
        var result = false;
        for (RenderedBone value : boneMap.values()) {
            if (value.matchTree(predicate, mapper)) result = true;
        }
        return result;
    }

    /**
     * Checks if any bones match a predicate.
     *
     * @param predicate the predicate
     * @return true if any bones matched
     * @since 1.15.2
     */
    public boolean matchTree(@NotNull Predicate<RenderedBone> predicate) {
        Objects.requireNonNull(predicate);
        var result = false;
        for (RenderedBone value : boneMap.values()) {
            if (value.matchTree(predicate)) result = true;
        }
        return result;
    }

    /**
     * Iterates over all bones in the tree.
     *
     * @param consumer the consumer to apply to each bone
     * @since 1.15.2
     */
    public void iterateTree(@NotNull Consumer<RenderedBone> consumer) {
        Objects.requireNonNull(consumer);
        for (RenderedBone value : boneMap.values()) {
            value.iterateTree(consumer);
        }
    }

    /**
     * Finds the first non-null result of applying a mapper to all bones.
     *
     * @param mapper the mapper function
     * @param <T> the result type
     * @return the first non-null result, or null
     * @since 1.15.2
     */
    public <T> @Nullable T firstNotNull(@NotNull Function<RenderedBone, T> mapper) {
        return bones()
            .stream()
            .map(mapper)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns the number of players currently viewing this model.
     *
     * @return the player count
     * @since 1.15.2
     */
    public int playerCount() {
        return playerMap.size();
    }

    /**
     * Returns a stream of all players viewing this model.
     *
     * @return the stream of players
     * @since 1.15.2
     */
    public @NotNull Stream<Player> allPlayer() {
        return playerMap.values()
            .stream()
            .map(spawned -> spawned.handler.player());
    }

    /**
     * Returns a stream of players who are not hidden and pass the view filter.
     *
     * @return the stream of visible players
     * @since 1.15.2
     */
    public @NotNull Stream<Player> nonHidePlayer() {
        return playerMap.values()
            .stream()
            .filter(spawned -> spawned.initialLoad)
            .map(spawned -> spawned.handler.player())
            .filter(viewFilter);
    }

    /**
     * Returns a stream of players who pass the view filter (regardless of hidden status).
     *
     * @return the stream of viewed players
     * @since 1.15.2
     */
    public @NotNull Stream<Player> viewedPlayer() {
        return allPlayer().filter(viewFilter);
    }

    /**
     * Hides the model from a specific player.
     *
     * @param player the player to hide from
     * @return true if the player was successfully hidden
     * @since 1.15.2
     */
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

    /**
     * Checks if the model is hidden from a specific player.
     *
     * @param player the player to check
     * @return true if hidden
     * @since 1.15.2
     */
    public boolean isHide(@NotNull Player player) {
        return hideFilter.test(player);
    }

    /**
     * Shows the model to a specific player (if previously hidden).
     *
     * @param player the player to show to
     * @return true if the player was successfully shown
     * @since 1.15.2
     */
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

    /**
     * Represents a player for whom the model has been spawned.
     *
     * @since 1.15.2
     */
    @RequiredArgsConstructor
    public class SpawnedPlayer {
        private final PlayerChannelHandler handler;
        private boolean initialLoad;

        /**
         * Loads the model for this player, sending initial packets.
         *
         * @since 1.15.2
         */
        public void load() {
            initialLoad = true;
            if (isHide(handler.player())) return;
            var b = createBundler();
            iterateTree(bone -> bone.forceUpdate(b));
            if (b.isNotEmpty()) b.send(handler.player());
        }
    }
}

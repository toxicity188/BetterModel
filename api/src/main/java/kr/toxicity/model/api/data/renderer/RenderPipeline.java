/*
 * This source file is part of BetterModel.
 * Copyright (c) 2024 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.renderer;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationOverrideState;
import kr.toxicity.model.api.animation.RunningAnimation;
import kr.toxicity.model.api.bone.*;
import kr.toxicity.model.api.manager.PlayerManager;
import kr.toxicity.model.api.nms.AnimationBundler;
import kr.toxicity.model.api.nms.HitBox;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.nms.PlayerChannelHandler;
import kr.toxicity.model.api.platform.PlatformPlayer;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.util.FunctionUtil;
import kr.toxicity.model.api.util.function.BonePredicate;
import kr.toxicity.model.api.util.function.FloatSupplier;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static kr.toxicity.model.api.util.CollectionUtil.associate;
import static kr.toxicity.model.api.util.CollectionUtil.associateSequenced;

/**
 * Represents the rendering pipeline for a specific model instance.
 * <p>
 * This class manages the hierarchy of {@link RenderedBone}s, handles player visibility and packet bundling,
 * and coordinates animation updates and inverse kinematics (IK) solving.
 * </p>
 *
 * @since 1.15.2
 */
public final class RenderPipeline implements BoneEventHandler, Iterable<RenderedBone> {

    @Getter
    private final ModelRenderer parent;
    @Getter
    private final RenderSource<?> source;

    private final RenderedBone[] bones;
    private final RenderedBone[] flattenBones;
    private final SequencedMap<BoneName, RenderedBone> byIdMap;

    private final int displayAmount;
    private final Map<UUID, SpawnedPlayer> playerMap = new ConcurrentHashMap<>();
    private final Set<UUID> hidePlayerSet = ConcurrentHashMap.newKeySet();

    private final BoneEventDispatcher eventDispatcher = new BoneEventDispatcher();
    private final BoneIKSolver ikSolver;

    private Predicate<PlatformPlayer> viewFilter = _ -> true;
    private Predicate<PlatformPlayer> hideFilter = p -> hidePlayerSet.contains(p.uuid());

    private Consumer<PacketBundler> spawnPacketHandler = _ -> {};
    private Consumer<PacketBundler> despawnPacketHandler = _ -> {};
    private Consumer<PacketBundler> hidePacketHandler = _ -> {};
    private Consumer<PacketBundler> showPacketHandler = _ -> {};

    @Getter
    private ModelRotation rotation = ModelRotation.INVALID;

    /**
     * Creates a new render pipeline for a model renderer and root bones.
     *
     * <pre>{@code
     * RenderPipeline pipeline = new RenderPipeline(modelRenderer, renderSource, rootBones);
     * }</pre>
     *
     * @param parent the parent model renderer
     * @param source the source of the rendering (entity or location)
     * @param bones the array of root bones
     * @throws NullPointerException if {@code parent}, {@code source}, or {@code bones} is null
     * @since 1.15.2
     */
    public RenderPipeline(
        @NotNull ModelRenderer parent,
        @NotNull RenderSource<?> source,
        @NotNull RenderedBone[] bones
    ) {
        this.parent = parent;
        this.source = source;
        this.bones = bones;
        // Bone
        flattenBones = Arrays.stream(bones).flatMap(RenderedBone::flatten).toArray(RenderedBone[]::new);
        byIdMap = associateSequenced(flattenBones, RenderedBone::name);
        ikSolver = new BoneIKSolver(associate(flattenBones, RenderedBone::uuid));
        // Setup
        displayAmount = (int) Arrays.stream(flattenBones)
            .peek(bone -> bone.extend(this))
            .peek(bone -> bone.locator(ikSolver))
            .filter(rb -> rb.getDisplay() != null)
            .count();
    }

    /**
     * Creates a packet bundler for this pipeline sized to hold all display entities.
     *
     * <pre>{@code
     * PacketBundler bundler = pipeline.createBundler();
     * }</pre>
     *
     * @return a new packet bundler
     * @since 1.15.2
     */
    public @NotNull PacketBundler createBundler() {
        return BetterModel.nms().createBundler(displayAmount + 1);
    }

    /**
     * Retrieves the network channel handler for a specific player.
     *
     * <pre>{@code
     * PlayerChannelHandler handler = pipeline.channel(player.uuid());
     * if (handler != null) {
     *     bundler.send(handler);
     * }
     * }</pre>
     *
     * @param uuid the UUID of the player
     * @return the channel handler, or {@code null} if the player is not spawned
     * @throws NullPointerException if {@code uuid} is null
     * @since 1.15.2
     */
    public @Nullable PlayerChannelHandler channel(@NotNull UUID uuid) {
        var get = playerMap.get(uuid);
        return get != null ? get.handler : null;
    }

    /**
     * Creates an animation packet bundler based on configuration.
     *
     * <pre>{@code
     * AnimationBundler bundler = pipeline.createAnimationBundler();
     * }</pre>
     *
     * @return a new animation packet bundler
     * @since 2.2.1
     */
    public @NotNull AnimationBundler createAnimationBundler() {
        var size = BetterModel.config().packetBundlingSize();
        var nms = BetterModel.nms();
        return new AnimationBundler(
            size <= 0 ? createBundler() : nms.createParallelBundler(size),
            nms.createModAnimationBuilder(displayAmount)
        );
    }

    /**
     * Returns the bone event dispatcher used to register and trigger bone lifecycle events.
     *
     * <pre>{@code
     * pipeline.eventDispatcher().handleCreateHitBox((bone, builder) -> {
     *     return builder;
     * });
     * }</pre>
     *
     * @return the bone event dispatcher
     * @since 3.6.0
     */
    @Override
    public @NotNull BoneEventDispatcher eventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Adds a filter to restrict which players can view the model.
     * <p>
     * The given predicate is combined with the existing view filter using logical AND.
     * </p>
     *
     * <pre>{@code
     * pipeline.viewFilter(player -> player.hasPermission("model.view"));
     * }</pre>
     *
     * @param filter the predicate to filter players
     * @throws NullPointerException if {@code filter} is null
     * @since 1.15.2
     */
    public void viewFilter(@NotNull Predicate<PlatformPlayer> filter) {
        this.viewFilter = this.viewFilter.and(Objects.requireNonNull(filter));
    }

    /**
     * Adds a filter to determine if a player should be hidden from the model.
     * <p>
     * The given predicate is combined with the existing hide filter using logical OR.
     * </p>
     *
     * <pre>{@code
     * pipeline.hideFilter(player -> player.isVanished());
     * }</pre>
     *
     * @param filter the predicate to hide players
     * @throws NullPointerException if {@code filter} is null
     * @since 1.15.2
     */
    public void hideFilter(@NotNull Predicate<PlatformPlayer> filter) {
        this.hideFilter = this.hideFilter.or(Objects.requireNonNull(filter));
    }

    /**
     * Adds a handler invoked when spawn packets are bundled.
     * <p>
     * Additional handlers are executed sequentially via {@link Consumer#andThen(Consumer)}.
     * </p>
     *
     * <pre>{@code
     * pipeline.spawnPacketHandler(bundler -> {
     *     // append custom packets to spawn bundler
     * });
     * }</pre>
     *
     * @param spawnPacketHandler the consumer to handle spawn packets
     * @throws NullPointerException if {@code spawnPacketHandler} is null
     * @since 1.15.2
     */
    public void spawnPacketHandler(@NotNull Consumer<PacketBundler> spawnPacketHandler) {
        this.spawnPacketHandler = this.spawnPacketHandler.andThen(Objects.requireNonNull(spawnPacketHandler));
    }

    /**
     * Adds a handler invoked when despawn packets are bundled.
     * <p>
     * Additional handlers are executed sequentially via {@link Consumer#andThen(Consumer)}.
     * </p>
     *
     * <pre>{@code
     * pipeline.despawnPacketHandler(bundler -> {
     *     // append custom packets to despawn bundler
     * });
     * }</pre>
     *
     * @param despawnPacketHandler the consumer to handle despawn packets
     * @throws NullPointerException if {@code despawnPacketHandler} is null
     * @since 1.15.2
     */
    public void despawnPacketHandler(@NotNull Consumer<PacketBundler> despawnPacketHandler) {
        this.despawnPacketHandler = this.despawnPacketHandler.andThen(Objects.requireNonNull(despawnPacketHandler));
    }

    /**
     * Adds a handler invoked when hide packets are bundled for a player.
     * <p>
     * Additional handlers are executed sequentially via {@link Consumer#andThen(Consumer)}.
     * </p>
     *
     * <pre>{@code
     * pipeline.hidePacketHandler(bundler -> {
     *     // append custom packets when model is hidden
     * });
     * }</pre>
     *
     * @param hidePacketHandler the consumer to handle hide packets
     * @throws NullPointerException if {@code hidePacketHandler} is null
     * @since 1.15.2
     */
    public void hidePacketHandler(@NotNull Consumer<PacketBundler> hidePacketHandler) {
        this.hidePacketHandler = this.hidePacketHandler.andThen(Objects.requireNonNull(hidePacketHandler));
    }

    /**
     * Adds a handler invoked when show packets are bundled for a player.
     * <p>
     * Additional handlers are executed sequentially via {@link Consumer#andThen(Consumer)}.
     * </p>
     *
     * <pre>{@code
     * pipeline.showPacketHandler(bundler -> {
     *     // append custom packets when model is shown
     * });
     * }</pre>
     *
     * @param showPacketHandler the consumer to handle show packets
     * @throws NullPointerException if {@code showPacketHandler} is null
     * @since 1.15.2
     */
    public void showPacketHandler(@NotNull Consumer<PacketBundler> showPacketHandler) {
        this.showPacketHandler = this.showPacketHandler.andThen(Objects.requireNonNull(showPacketHandler));
    }

    /**
     * Checks if the model is currently spawned for a specific player.
     *
     * <pre>{@code
     * if (pipeline.isSpawned(player.uuid())) {
     *     // player is currently tracked
     * }
     * }</pre>
     *
     * @param uuid the UUID of the player
     * @return {@code true} if spawned for the player, {@code false} otherwise
     * @throws NullPointerException if {@code uuid} is null
     * @since 1.15.2
     */
    public boolean isSpawned(@NotNull UUID uuid) {
        return playerMap.containsKey(uuid);
    }

    /**
     * Retrieves the currently running animation across bones, if any.
     *
     * <pre>{@code
     * RunningAnimation animation = pipeline.runningAnimation();
     * if (animation != null) {
     *     // query running animation details
     * }
     * }</pre>
     *
     * @return the running animation, or {@code null} if none is playing
     * @since 1.15.2
     */
    public @Nullable RunningAnimation runningAnimation() {
        return firstNotNull(RenderedBone::runningAnimation);
    }

    /**
     * Returns the name of the model.
     *
     * <pre>{@code
     * String name = pipeline.name();
     * }</pre>
     *
     * @return the model name
     * @since 1.15.2
     */
    public @NotNull String name() {
        return parent.name();
    }

    /**
     * Despawns the model for all viewing players, removes all hitboxes, and clears internal state.
     *
     * <pre>{@code
     * pipeline.despawn();
     * }</pre>
     *
     * @since 1.15.2
     */
    public void despawn() {
        hitboxes().forEach(HitBox::removeHitBox);
        var bundler = createBundler();
        remove0(bundler);
        if (bundler.isNotEmpty()) forEachAllPlayer(bundler::send);
        playerMap.clear();
    }

    /**
     * Rotates the model to a new orientation and bundles transformation packets.
     *
     * <pre>{@code
     * PacketBundler bundler = pipeline.createBundler();
     * if (pipeline.rotate(newRotation, bundler)) {
     *     pipeline.forEachAllPlayer(bundler::send);
     * }
     * }</pre>
     *
     * @param rotation the new model rotation
     * @param bundler the packet bundler to receive rotation packets
     * @return {@code true} if the rotation changed and packets were updated, {@code false} otherwise
     * @throws NullPointerException if {@code rotation} or {@code bundler} is null
     * @since 1.15.2
     */
    public boolean rotate(@NotNull ModelRotation rotation, @NotNull PacketBundler bundler) {
        if (rotation.equals(this.rotation)) return false;
        this.rotation = rotation;
        return matchTree(b -> b.rotate(rotation, bundler));
    }

    /**
     * Ticks the model globally, updating bone animations, solving IK, and bundling transformations.
     *
     * <pre>{@code
     * AnimationBundler bundler = pipeline.createAnimationBundler();
     * if (pipeline.tick(bundler)) {
     *     pipeline.forEachNonHidePlayer(bundler::send);
     * }
     * }</pre>
     *
     * @param bundler the animation packet bundler to use
     * @return {@code true} if any bone or animation updates occurred, {@code false} otherwise
     * @throws NullPointerException if {@code bundler} is null
     * @since 1.15.2
     */
    public boolean tick(@NotNull AnimationBundler bundler) {
        var match = matchTree(RenderedBone::tick);
        if (match) {
            ikSolver.solve();
            forEach(b -> b.sendTransformation(null, bundler));
        }
        return match;
    }

    /**
     * Ticks the model for a specific player, updating per-player animations, solving IK, and bundling transformations.
     *
     * <pre>{@code
     * AnimationBundler bundler = pipeline.createAnimationBundler();
     * if (pipeline.tick(playerUuid, bundler)) {
     *     PlayerChannelHandler handler = pipeline.channel(playerUuid);
     *     if (handler != null) bundler.send(handler);
     * }
     * }</pre>
     *
     * @param uuid the UUID of the player
     * @param bundler the animation packet bundler to use
     * @return {@code true} if any bone or animation updates occurred for the player, {@code false} otherwise
     * @throws NullPointerException if {@code uuid} or {@code bundler} is null
     * @since 1.15.2
     */
    public boolean tick(@NotNull UUID uuid, @NotNull AnimationBundler bundler) {
        var match = matchTree(b -> b.tick(uuid));
        if (match) {
            ikSolver.solve(uuid);
            forEach(b -> b.sendTransformation(uuid, bundler));
        }
        return match;
    }

    /**
     * Sets the default position modifier for all bones in the pipeline.
     *
     * <pre>{@code
     * pipeline.defaultPosition(pos -> pos.add(0, 1.0f, 0));
     * }</pre>
     *
     * @param movement the position transformation function
     * @throws NullPointerException if {@code movement} is null
     * @since 1.15.2
     */
    public void defaultPosition(@NotNull Function<Vector3f, Vector3f> movement) {
        var vec = new Vector3f();
        var supplier = FunctionUtil.throttleTick(() -> movement.apply(vec));
        forEach(b -> b.defaultPosition(supplier));
    }

    /**
     * Sets the scale supplier for all bones in the pipeline.
     *
     * <pre>{@code
     * pipeline.scale(() -> 1.5f);
     * }</pre>
     *
     * @param scale the scale supplier
     * @throws NullPointerException if {@code scale} is null
     * @since 1.15.2
     */
    public void scale(@NotNull FloatSupplier scale) {
        forEach(b -> b.scale(scale));
    }

    /**
     * Adds a local rotation modifier to bones matching the given predicate.
     *
     * <pre>{@code
     * pipeline.addLocalRotModifier(BonePredicate.name("head"), rot -> rot.rotateX(0.1f));
     * }</pre>
     *
     * @param predicate the predicate to select bones
     * @param mapper the local rotation mapping function
     * @return {@code true} if any matching bones were modified, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} or {@code mapper} is null
     * @since 3.0.0
     */
    public boolean addLocalRotModifier(@NotNull BonePredicate predicate, @NotNull Function<Quaternionf, Quaternionf> mapper) {
        return matchTree(predicate, (b, p) -> b.addLocalRotModifier(p, mapper));
    }

    /**
     * Adds a global rotation modifier to bones matching the given predicate.
     *
     * <pre>{@code
     * pipeline.addGlobalRotModifier(BonePredicate.TRUE, rot -> rot.rotateY(0.2f));
     * }</pre>
     *
     * @param predicate the predicate to select bones
     * @param mapper the global rotation mapping function
     * @return {@code true} if any matching bones were modified, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} or {@code mapper} is null
     * @since 3.0.0
     */
    public boolean addGlobalRotModifier(@NotNull BonePredicate predicate, @NotNull Function<Quaternionf, Quaternionf> mapper) {
        return matchTree(predicate, (b, p) -> b.addGlobalRotModifier(p, mapper));
    }

    /**
     * Adds a position modifier to bones matching the given predicate.
     *
     * <pre>{@code
     * pipeline.addPositionModifier(BonePredicate.name("root"), pos -> pos.add(0, 0.5f, 0));
     * }</pre>
     *
     * @param predicate the predicate to select bones
     * @param mapper the position mapping function
     * @return {@code true} if any matching bones were modified, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} or {@code mapper} is null
     * @since 1.15.2
     */
    public boolean addPositionModifier(@NotNull BonePredicate predicate, @NotNull Function<Vector3f, Vector3f> mapper) {
        return matchTree(predicate, (b, p) -> b.addPositionModifier(p, mapper));
    }

    /**
     * Returns an unmodifiable collection of all rendered bones in this pipeline.
     *
     * <pre>{@code
     * for (RenderedBone bone : pipeline.bones()) {
     *     // process rendered bone
     * }
     * }</pre>
     *
     * @return the unmodifiable collection of bones
     * @since 1.15.2
     */
    public @NotNull @Unmodifiable Collection<RenderedBone> bones() {
        return byIdMap.values();
    }

    /**
     * Returns a stream of all hitboxes associated with bones in this pipeline.
     *
     * <pre>{@code
     * pipeline.hitboxes().forEach(hitbox -> {
     *     // inspect hitbox
     * });
     * }</pre>
     *
     * @return the stream of non-null hitboxes
     * @since 1.15.2
     */
    public @NotNull Stream<HitBox> hitboxes() {
        return stream()
            .map(RenderedBone::getHitBox)
            .filter(Objects::nonNull);
    }

    /**
     * Retrieves a rendered bone by its name.
     *
     * <pre>{@code
     * RenderedBone bone = pipeline.boneOf(BoneName.of("head"));
     * }</pre>
     *
     * @param name the name of the bone
     * @return the rendered bone, or {@code null} if not found
     * @throws NullPointerException if {@code name} is null
     * @since 1.15.2
     */
    public @Nullable RenderedBone boneOf(@NotNull BoneName name) {
        return byIdMap.get(name);
    }

    /**
     * Spawns the model for a player and bundles initial packets.
     *
     * <pre>{@code
     * pipeline.spawn(player, bundler, spawned -> {
     *     spawned.load();
     * });
     * }</pre>
     *
     * @param player the player to spawn for
     * @param bundler the packet bundler to receive spawn packets
     * @param consumer a consumer accepting the newly created {@link SpawnedPlayer}
     * @return {@code true} if spawned successfully, {@code false} if the player is not registered in player manager
     * @throws NullPointerException if {@code player}, {@code bundler}, or {@code consumer} is null
     * @since 1.15.2
     */
    @ApiStatus.Internal
    public boolean spawn(@NotNull PlatformPlayer player, @NotNull PacketBundler bundler, @NotNull Consumer<SpawnedPlayer> consumer) {
        var get = BetterModel.platform().manager(PlayerManager.class).player(player.uuid());
        if (get == null) return false;
        var spawnedPlayer = new SpawnedPlayer(get);
        playerMap.put(player.uuid(), spawnedPlayer);
        spawnPacketHandler.accept(bundler);
        var hided = isHide(player);
        forEach(b -> b.spawn(hided, bundler));
        consumer.accept(spawnedPlayer);
        return true;
    }

    /**
     * Removes the model for a player and sends despawn packets immediately.
     *
     * <pre>{@code
     * boolean removed = pipeline.remove(player);
     * }</pre>
     *
     * @param player the player to remove the model for
     * @return {@code true} if removed successfully, {@code false} if the player was not spawned
     * @throws NullPointerException if {@code player} is null
     * @since 1.15.2
     */
    @ApiStatus.Internal
    public boolean remove(@NotNull PlatformPlayer player) {
        if (playerMap.remove(player.uuid()) == null) return false;
        var bundler = createBundler();
        remove0(bundler);
        bundler.send(player);
        return true;
    }

    @ApiStatus.Internal
    private void remove0(@NotNull PacketBundler bundler) {
        despawnPacketHandler.accept(bundler);
        forEach(b -> b.remove(bundler));
    }

    /**
     * Applies a matching predicate function to bones matching the given bone predicate.
     *
     * <pre>{@code
     * boolean matched = pipeline.matchTree(BonePredicate.name("head"), (bone, pred) -> {
     *     return true;
     * });
     * }</pre>
     *
     * @param predicate the bone predicate to filter bones
     * @param mapper the evaluation function applied to matching bones
     * @return {@code true} if any matching bone satisfied the mapper, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} or {@code mapper} is null
     * @since 1.15.2
     */
    public boolean matchTree(@NotNull BonePredicate predicate, @NotNull BiPredicate<RenderedBone, BonePredicate> mapper) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(mapper);
        if (predicate == BonePredicate.FALSE) return false;
        if (predicate.applyAtChildren() == BonePredicate.State.NOT_SET) return matchTree(b -> mapper.test(b, predicate));
        var result = false;
        for (RenderedBone value : bones) {
            if (value.matchTree(predicate, mapper)) result = true;
        }
        return result;
    }

    /**
     * Applies an animation override evaluation to all bone hierarchies.
     *
     * <pre>{@code
     * boolean matched = pipeline.matchAnimation((bone, state) -> {
     *     return true;
     * });
     * }</pre>
     *
     * @param mapper the evaluation function applied to bones and override states
     * @return {@code true} if any bone matched the animation predicate, {@code false} otherwise
     * @throws NullPointerException if {@code mapper} is null
     * @since 1.15.2
     */
    public boolean matchAnimation(@NotNull BiPredicate<RenderedBone, AnimationOverrideState> mapper) {
        Objects.requireNonNull(mapper);
        var result = false;
        for (RenderedBone value : bones) {
            if (value.matchAnimation(AnimationOverrideState.NOT_MATCHED, mapper)) result = true;
        }
        return result;
    }

    /**
     * Checks if any bone in the flattened hierarchy matches the given predicate.
     *
     * <pre>{@code
     * boolean hasDisplays = pipeline.matchTree(bone -> bone.getDisplay() != null);
     * }</pre>
     *
     * @param predicate the predicate to test each rendered bone
     * @return {@code true} if at least one bone matched the predicate, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     * @since 1.15.2
     */
    public boolean matchTree(@NotNull Predicate<RenderedBone> predicate) {
        Objects.requireNonNull(predicate);
        var result = false;
        for (RenderedBone value : flattenBones) {
            if (predicate.test(value)) result = true;
        }
        return result;
    }

    /**
     * Finds the first non-null result of applying a mapping function across all flattened bones.
     *
     * <pre>{@code
     * RunningAnimation anim = pipeline.firstNotNull(RenderedBone::runningAnimation);
     * }</pre>
     *
     * @param mapper the function to apply to each bone
     * @param <T> the result type
     * @return the first non-null result produced by {@code mapper}, or {@code null} if none
     * @throws NullPointerException if {@code mapper} is null
     * @since 1.15.2
     */
    public <T> @Nullable T firstNotNull(@NotNull Function<RenderedBone, T> mapper) {
        Objects.requireNonNull(mapper);
        for (RenderedBone value : flattenBones) {
            var t = mapper.apply(value);
            if (t != null) return t;
        }
        return null;
    }

    /**
     * Returns the number of players for whom the model is currently spawned.
     *
     * <pre>{@code
     * int count = pipeline.playerCount();
     * }</pre>
     *
     * @return the number of spawned players
     * @since 1.15.2
     */
    public int playerCount() {
        return playerMap.size();
    }

    /**
     * Returns a stream of channel handlers for all players for whom the model is spawned.
     *
     * <pre>{@code
     * pipeline.allPlayer().forEach(handler -> {
     *     // process player channel handler
     * });
     * }</pre>
     *
     * @return the stream of all spawned player channel handlers
     * @since 1.15.2
     */
    public @NotNull Stream<PlayerChannelHandler> allPlayer() {
        return playerMap.values()
            .stream()
            .map(spawned -> spawned.handler);
    }

    /**
     * Performs the given action for each player channel handler for whom the model is spawned.
     *
     * <pre>{@code
     * pipeline.forEachAllPlayer(handler -> bundler.send(handler));
     * }</pre>
     *
     * @param consumer the action to perform on each player channel handler
     * @throws NullPointerException if {@code consumer} is null
     * @since 3.6.0
     */
    public void forEachAllPlayer(@NotNull Consumer<PlayerChannelHandler> consumer) {
        Objects.requireNonNull(consumer);
        for (SpawnedPlayer value : playerMap.values()) {
            consumer.accept(value.handler);
        }
    }

    /**
     * Returns a stream of players who have finished initial load and are not hidden.
     * <p>
     * Note that this method checks {@link #isHide(PlatformPlayer)}, but does not evaluate the view filter.
     * </p>
     *
     * <pre>{@code
     * pipeline.nonHidePlayer().forEach(handler -> {
     *     bundler.send(handler);
     * });
     * }</pre>
     *
     * @return the stream of non-hidden, loaded player channel handlers
     * @since 1.15.2
     */
    public @NotNull Stream<PlayerChannelHandler> nonHidePlayer() {
        return playerMap.values()
            .stream()
            .filter(spawned -> spawned.initialLoad)
            .map(spawned -> spawned.handler)
            .filter(p -> !hideFilter.test(p.player()));
    }

    /**
     * Performs the given action for each player channel handler who has finished initial load and is not hidden.
     *
     * <pre>{@code
     * pipeline.forEachNonHidePlayer(handler -> bundler.send(handler));
     * }</pre>
     *
     * @param consumer the action to perform on each non-hidden player channel handler
     * @throws NullPointerException if {@code consumer} is null
     * @since 3.6.0
     */
    public void forEachNonHidePlayer(@NotNull Consumer<PlayerChannelHandler> consumer) {
        Objects.requireNonNull(consumer);
        for (SpawnedPlayer value : playerMap.values()) {
            if (!value.initialLoad) continue;
            var handler = value.handler;
            if (hideFilter.test(handler.player())) continue;
            consumer.accept(handler);
        }
    }

    /**
     * Returns a stream of player channel handlers who pass the view filter (regardless of hidden status).
     *
     * <pre>{@code
     * pipeline.viewedPlayer().forEach(handler -> {
     *     // process viewed player
     * });
     * }</pre>
     *
     * @return the stream of viewed player channel handlers
     * @since 1.15.2
     */
    public @NotNull Stream<PlayerChannelHandler> viewedPlayer() {
        return allPlayer().filter(channel -> viewFilter.test(channel.player()));
    }

    /**
     * Performs the given action for each player channel handler who passes the view filter.
     *
     * <pre>{@code
     * pipeline.forEachViewedPlayer(handler -> bundler.send(handler));
     * }</pre>
     *
     * @param consumer the action to perform on each viewed player channel handler
     * @throws NullPointerException if {@code consumer} is null
     * @since 3.6.0
     */
    public void forEachViewedPlayer(@NotNull Consumer<PlayerChannelHandler> consumer) {
        Objects.requireNonNull(consumer);
        for (SpawnedPlayer value : playerMap.values()) {
            var handler = value.handler;
            if (viewFilter.test(handler.player())) consumer.accept(handler);
        }
    }

    /**
     * Hides the model and its hitboxes from a specific player.
     *
     * <pre>{@code
     * boolean hidden = pipeline.hide(player);
     * }</pre>
     *
     * @param player the player to hide the model from
     * @return {@code true} if the player was newly hidden; {@code false} if already hidden
     * @throws NullPointerException if {@code player} is null
     * @since 1.15.2
     */
    public boolean hide(@NotNull PlatformPlayer player) {
        if (isHide(player) || !hidePlayerSet.add(player.uuid())) return false;
        if (isSpawned(player.uuid())) {
            var bundler = createBundler();
            forEach(b -> b.forceUpdate(false, bundler));
            hidePacketHandler.accept(bundler);
            if (bundler.isNotEmpty()) bundler.send(player);
        }
        player.task(() -> hitboxes().forEach(hb -> hb.hide(player)));
        return true;
    }

    /**
     * Checks if the model is hidden from a specific player.
     *
     * <pre>{@code
     * if (pipeline.isHide(player)) {
     *     // model is hidden for this player
     * }
     * }</pre>
     *
     * @param player the player to check
     * @return {@code true} if hidden, {@code false} otherwise
     * @throws NullPointerException if {@code player} is null
     * @since 1.15.2
     */
    public boolean isHide(@NotNull PlatformPlayer player) {
        return hideFilter.test(player);
    }

    /**
     * Shows the model and its hitboxes to a specific player if previously hidden.
     *
     * <pre>{@code
     * boolean shown = pipeline.show(player);
     * }</pre>
     *
     * @param player the player to show the model to
     * @return {@code true} if the player was successfully shown; {@code false} if not previously hidden
     * @throws NullPointerException if {@code player} is null
     * @since 1.15.2
     */
    public boolean show(@NotNull PlatformPlayer player) {
        if (!isHide(player) || !hidePlayerSet.remove(player.uuid())) return false;
        if (isSpawned(player.uuid())) {
            var bundler = createBundler();
            forEach(b -> b.forceUpdate(true, bundler));
            showPacketHandler.accept(bundler);
            if (bundler.isNotEmpty()) bundler.send(player);
        }
        player.task(() -> hitboxes().forEach(hb -> hb.show(player)));
        return true;
    }

    /**
     * Performs the given action for each rendered bone in this pipeline.
     *
     * <pre>{@code
     * pipeline.forEach(bone -> {
     *     // process rendered bone
     * });
     * }</pre>
     *
     * @param action the action to perform on each rendered bone
     * @throws NullPointerException if {@code action} is null
     * @since 1.15.2
     */
    @Override
    public void forEach(@NotNull Consumer<? super RenderedBone> action) {
        for (RenderedBone bone : flattenBones) {
            action.accept(bone);
        }
    }

    /**
     * Returns an iterator over the rendered bones in this pipeline.
     *
     * <pre>{@code
     * Iterator<RenderedBone> iterator = pipeline.iterator();
     * while (iterator.hasNext()) {
     *     RenderedBone bone = iterator.next();
     * }
     * }</pre>
     *
     * @return an iterator over the rendered bones
     * @since 1.15.2
     */
    @Override
    @NotNull
    public Iterator<RenderedBone> iterator() {
        return bones().iterator();
    }

    /**
     * Returns a spliterator over the flattened rendered bones in this pipeline.
     *
     * <pre>{@code
     * Spliterator<RenderedBone> spliterator = pipeline.spliterator();
     * }</pre>
     *
     * @return a spliterator over the flattened rendered bones
     * @since 1.15.2
     */
    @Override
    @NotNull
    public Spliterator<RenderedBone> spliterator() {
        return Arrays.spliterator(flattenBones);
    }

    /**
     * Returns a sequential {@code Stream} with the flattened bones as its source.
     *
     * <pre>{@code
     * Stream<RenderedBone> boneStream = pipeline.stream();
     * }</pre>
     *
     * @return a stream of all bones in this pipeline
     * @since 2.2.0
     */
    public @NotNull Stream<RenderedBone> stream() {
        return Arrays.stream(flattenBones);
    }

    /**
     * Represents a player for whom the model has been spawned.
     *
     * @since 1.15.2
     */
    public class SpawnedPlayer {
        private final PlayerChannelHandler handler;
        private boolean initialLoad;

        /**
         * Creates a new spawned player entry.
         *
         * <pre>{@code
         * SpawnedPlayer spawnedPlayer = new SpawnedPlayer(handler);
         * }</pre>
         *
         * @param handler the player channel handler
         * @throws NullPointerException if {@code handler} is null
         * @since 1.15.2
         */
        public SpawnedPlayer(@NotNull PlayerChannelHandler handler) {
            this.handler = Objects.requireNonNull(handler);
        }

        /**
         * Loads the model for this player, sending initial packets.
         *
         * <pre>{@code
         * spawnedPlayer.load();
         * }</pre>
         *
         * @since 1.15.2
         */
        public void load() {
            initialLoad = true;
            if (isHide(handler.player())) return;
            var b = createBundler();
            forEach(bone -> bone.forceUpdate(b));
            if (b.isNotEmpty()) b.send(handler);
        }
    }
}

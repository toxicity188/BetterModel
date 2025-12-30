/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTags;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.config.DebugConfig;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.event.*;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.script.TimeScript;
import kr.toxicity.model.api.util.EntityUtil;
import kr.toxicity.model.api.util.EventUtil;
import kr.toxicity.model.api.util.LogUtil;
import kr.toxicity.model.api.util.MathUtil;
import kr.toxicity.model.api.util.function.BonePredicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Represents the core controller for a specific model instance.
 * <p>
 * A Tracker manages the lifecycle, rendering, animation, and player interaction of a model.
 * It coordinates with the {@link RenderPipeline} to update bone positions and send packets to players.
 * </p>
 *
 * @since 1.15.2
 */
public abstract class Tracker implements AutoCloseable {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2, new ThreadFactory() {

        private final AtomicInteger integer = new AtomicInteger();

        @Override
        public Thread newThread(@NotNull Runnable r) {
            var thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("BetterModel-Worker-" + integer.getAndIncrement());
            thread.setUncaughtExceptionHandler((t, e) -> LogUtil.handleException("Exception has occurred in " + t.getName(), e));
            return thread;
        }
    });
    /**
     * The interval in milliseconds between tracker ticks.
     * @since 1.15.2
     */
    public static final int TRACKER_TICK_INTERVAL = 10;
    /**
     * The multiplier to convert tracker ticks to Minecraft ticks (50ms).
     * @since 1.15.2
     */
    public static final int MINECRAFT_TICK_MULTIPLIER = MathUtil.MINECRAFT_TICK_MILLS / TRACKER_TICK_INTERVAL;

    @Getter
    protected final RenderPipeline pipeline;
    private long frame = 0;
    private final Queue<Runnable> queuedTask = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean tickPause = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private final AtomicBoolean readyForForceUpdate = new AtomicBoolean();
    private final AtomicBoolean forRemoval = new AtomicBoolean();
    protected final TrackerModifier modifier;
    private final Runnable updater;
    private final BundlerSet bundlerSet;
    private final AnimationStateHandler<TimeScript> scriptProcessor = new AnimationStateHandler<>(
        TimeScript.EMPTY,
        (b, a) -> {
            if (b == null) return;
            if (b.isSync()) {
                BetterModel.plugin().scheduler().task(location(), () -> b.accept(this));
            } else b.accept(this);
        }
    );
    private ScheduledFuture<?> task;
    protected ModelRotator rotator = ModelRotator.YAW;
    protected ModelScaler scaler = ModelScaler.entity();
    private Supplier<ModelRotation> rotationSupplier = () -> ModelRotation.EMPTY;
    private BiConsumer<Tracker, CloseReason> closeEventHandler = (t, r) -> EventUtil.call(new CloseTrackerEvent(t, r));

    private ScheduledPacketHandler handler = (t, s) -> {
        if (!tickPause.get()) {
            scriptProcessor.tick();
            t.pipeline.tick(s.getViewBundler());
        }
    };
    private BiConsumer<Tracker, Player> perPlayerHandler = null;

    /**
     * Creates a new tracker.
     *
     * @param pipeline the render pipeline
     * @param modifier the tracker modifier
     * @since 1.15.2
     */
    public Tracker(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier) {
        this.pipeline = pipeline;
        this.modifier = modifier;
        bundlerSet = new BundlerSet();
        updater = () -> {
            try {
                if (frame % MINECRAFT_TICK_MULTIPLIER == 0) {
                    Runnable task;
                    while ((task = queuedTask.poll()) != null) task.run();
                }
                handler.handle(this, bundlerSet);
                bundlerSet.send();
            } catch (Throwable throwable) {
                LogUtil.handleException("Ticking this tracker has been failed: " + name(), throwable);
            }
        };
        if (modifier.sightTrace()) pipeline.viewFilter(p -> EntityUtil.canSee(p.getEyeLocation(), location()));
        frame((t, s) -> {
            if (readyForForceUpdate.compareAndSet(true, false)) t.pipeline.iterateTree(b -> b.dirtyUpdate(s.dataBundler));
        });
        tick((t, s) -> pipeline.rotate(
            t.rotation(),
            s.tickBundler
        ));
        tick((t, s) -> {
            var perPlayer = perPlayerHandler;
            if (perPlayer != null) pipeline.nonHidePlayer().forEach(p -> perPlayer.accept(t, p));
        });
        pipeline.spawnPacketHandler(p -> start());
        pipeline.eventDispatcher().handleStateCreate((bone, uuid) -> bundlerSet.perPlayerViewBundler
            .computeIfAbsent(uuid, PerPlayerCache::new)
            .add());
        pipeline.eventDispatcher().handleStateRemove((bone, uuid) -> {
            var get = bundlerSet.perPlayerViewBundler.get(uuid);
            if (get != null) get.remove();
        });
        LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> getClass().getSimpleName() + " tracker created: " + name());
        animate("idle", AnimationModifier.builder().start(6).type(AnimationIterator.Type.LOOP).build());
        pipeline.getSource().completeContext().thenAccept(context -> {
            if (pipeline.matchTree(bone -> bone.updateItem(context))) forceUpdate(true);
        });
    }

    /**
     * Checks if the tracker's update task is currently scheduled.
     *
     * @return true if scheduled, false otherwise
     * @since 1.15.2
     */
    public boolean isScheduled() {
        return task != null && !task.isCancelled();
    }

    private void start() {
        if (isScheduled()) return;
        synchronized (this) {
            if (isScheduled()) return;
            updater.run();
            task = EXECUTOR.scheduleAtFixedRate(() -> {
                if (playerCount() == 0 && !forRemoval.get()) {
                    shutdown();
                    return;
                }
                frame++;
                updater.run();
            }, TRACKER_TICK_INTERVAL, TRACKER_TICK_INTERVAL, TimeUnit.MILLISECONDS);
            LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> getClass().getSimpleName() + " scheduler started: " + name());
        }
    }

    private void shutdown() {
        if (!isScheduled()) return;
        synchronized (this) {
            if (!isScheduled()) return;
            task.cancel(true);
            task = null;
            frame = 0;
            LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> getClass().getSimpleName() + " scheduler shutdown: " + name());
        }
    }

    /**
     * Returns the current rotation of the model.
     *
     * @return the model rotation
     * @since 1.15.2
     */
    public @NotNull ModelRotation rotation() {
        return rotator.apply(this, rotationSupplier.get());
    }

    /**
     * Sets the supplier for the base model rotation.
     *
     * @param supplier the rotation supplier
     * @since 1.15.2
     */
    public final void rotation(@NotNull Supplier<ModelRotation> supplier) {
        this.rotationSupplier = Objects.requireNonNull(supplier);
    }

    /**
     * Sets the model rotator strategy.
     *
     * @param rotator the rotator strategy
     * @since 1.15.2
     */
    public final void rotator(@NotNull ModelRotator rotator) {
        this.rotator = Objects.requireNonNull(rotator);
    }

    /**
     * Returns the model scaler.
     *
     * @return the scaler
     * @since 1.15.2
     */
    public @NotNull ModelScaler scaler() {
        return scaler;
    }

    /**
     * Sets the model scaler.
     *
     * @param scaler the new scaler
     * @since 1.15.2
     */
    public void scaler(@NotNull ModelScaler scaler) {
        this.scaler = Objects.requireNonNull(scaler);
    }

    /**
     * Schedules a task to run on the next tracker tick.
     *
     * @param runnable the task to run
     * @since 1.15.2
     */
    public void task(@NotNull Runnable runnable) {
        queuedTask.add(Objects.requireNonNull(runnable));
    }

    /**
     * Registers a handler to run every frame (tracker tick).
     *
     * @param handler the packet handler
     * @since 1.15.2
     */
    public synchronized void frame(@NotNull ScheduledPacketHandler handler) {
        this.handler = this.handler.then(Objects.requireNonNull(handler));
    }
    /**
     * Registers a handler to run every Minecraft tick (50ms).
     *
     * @param handler the packet handler
     * @since 1.15.2
     */
    public void tick(@NotNull ScheduledPacketHandler handler) {
        tick(1, handler);
    }
    /**
     * Registers a handler to run every N Minecraft ticks.
     *
     * @param tick the interval in Minecraft ticks
     * @param handler the packet handler
     * @since 1.15.2
     */
    public void tick(long tick, @NotNull ScheduledPacketHandler handler) {
        schedule(MINECRAFT_TICK_MULTIPLIER * tick, handler);
    }

    /**
     * Registers a handler to run every tick for each visible player.
     *
     * @param perPlayerHandler the per-player handler
     * @since 1.15.2
     */
    public synchronized void perPlayerTick(@NotNull BiConsumer<Tracker, Player> perPlayerHandler) {
        var previous = this.perPlayerHandler;
        this.perPlayerHandler = previous == null ? perPlayerHandler : previous.andThen(perPlayerHandler);
    }

    /**
     * Schedules a handler to run periodically.
     *
     * @param period the period in tracker ticks
     * @param handler the packet handler
     * @since 1.15.2
     */
    public void schedule(long period, @NotNull ScheduledPacketHandler handler) {
        Objects.requireNonNull(handler);
        if (period <= 0) throw new RuntimeException("period cannot be <= 0");
        frame(period == 1 ? handler : (t, s) -> {
            if (frame % period == 0) handler.handle(t, s);
        });
    }

    protected void update() {
        updater.run();
    }

    /**
     * Returns the name of the model being tracked.
     *
     * @return the model name
     * @since 1.15.2
     */
    public @NotNull String name() {
        return pipeline.name();
    }

    /**
     * Calculates the height of the model based on its head bone position.
     *
     * @return the height
     * @since 1.15.2
     */
    public double height() {
        return bones()
            .stream()
            .filter(bone -> bone.name().tagged(BoneTags.HEAD, BoneTags.HEAD_WITH_CHILDREN))
            .mapToDouble(bone -> bone.hitBoxPosition().y)
            .max()
            .orElse(0F);
    }

    /**
     * Checks if the tracker has been closed.
     *
     * @return true if closed, false otherwise
     * @since 1.15.2
     */
    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public void close() {
        close(CloseReason.REMOVE);
    }

    protected void close(@NotNull CloseReason reason) {
        if (isClosed.compareAndSet(false, true)) {
            closeEventHandler.accept(this, reason);
            shutdown();
            pipeline.despawn();
            LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> getClass().getSimpleName() + " closed: " + name());
        }
    }

    /**
     * Despawns the model for all players without closing the tracker completely.
     *
     * @since 1.15.2
     */
    public void despawn() {
        if (!isClosed()) {
            pipeline.despawn();
            LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> getClass().getSimpleName() + " despawned: " + name());
        }
    }

    /**
     * Returns the tracker modifier.
     *
     * @return the modifier
     * @since 1.15.2
     */
    public @NotNull TrackerModifier modifier() {
        return modifier;
    }

    /**
     * Pauses or resumes the tracker's ticking.
     *
     * @param pause true to pause, false to resume
     * @return true if the state changed, false otherwise
     * @since 1.15.2
     */
    public boolean pause(boolean pause) {
        return tickPause.compareAndSet(!pause, pause);
    }

    /**
     * Flags the tracker for a forced update on the next tick.
     *
     * @param force true to force update
     * @return true if the state changed
     * @since 1.15.2
     */
    public boolean forceUpdate(boolean force) {
        return readyForForceUpdate.compareAndSet(!force, force);
    }

    /**
     * Spawns the model for a specific player.
     *
     * @param player the target player
     * @param bundler the packet bundler
     * @return true if spawned successfully
     * @since 1.15.2
     */
    protected boolean spawn(@NotNull Player player, @NotNull PacketBundler bundler) {
        if (isClosed()) return false;
        if (!EventUtil.call(new ModelSpawnAtPlayerEvent(player, this))) return false;
        return pipeline.spawn(player, bundler, spawned -> {
            LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> getClass().getSimpleName() + " is spawned at player " + player.getName() + ": " + name());
            task(spawned::load);
        });
    }

    /**
     * Removes the model for a specific player.
     *
     * @param player the target player
     * @return true if removed successfully
     * @since 1.15.2
     */
    public boolean remove(@NotNull Player player) {
        if (isClosed()) return false;
        EventUtil.call(new ModelDespawnAtPlayerEvent(player, this));
        var result = pipeline.remove(player);
        if (result) LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> getClass().getSimpleName() + " is despawned at player " + player.getName() + ": " + name());
        return result;
    }

    /**
     * Returns the number of players currently viewing the model.
     *
     * @return the player count
     * @since 1.15.2
     */
    public int playerCount() {
        return pipeline.playerCount();
    }

    /**
     * Returns a stream of players currently viewing the model.
     *
     * @return the stream of players
     * @since 1.15.2
     */
    public @NotNull Stream<Player> viewedPlayer() {
        return pipeline.viewedPlayer();
    }

    /**
     * Returns the current location of the model.
     *
     * @return the location
     * @since 1.15.2
     */
    public abstract @NotNull Location location();

    /**
     * Plays an animation by name with default settings.
     *
     * @param animation the animation name
     * @return true if the animation started
     * @since 1.15.2
     */
    public boolean animate(@NotNull String animation) {
        return animate(animation, AnimationModifier.DEFAULT);
    }

    /**
     * Plays an animation by name with a modifier.
     *
     * @param animation the animation name
     * @param modifier the animation modifier
     * @return true if the animation started
     * @since 1.15.2
     */
    public boolean animate(@NotNull String animation, @NotNull AnimationModifier modifier) {
        return animate(animation, modifier, () -> {});
    }

    /**
     * Plays an animation by name with a modifier and a completion task.
     *
     * @param animation the animation name
     * @param modifier the animation modifier
     * @param removeTask the task to run when the animation ends
     * @return true if the animation started
     * @since 1.15.2
     */
    public boolean animate(@NotNull String animation, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        return animate(b -> true, animation, modifier, removeTask);
    }

    /**
     * Plays a blueprint animation with a modifier.
     *
     * @param animation the blueprint animation
     * @param modifier the animation modifier
     * @return true if the animation started
     * @since 1.15.2
     */
    public boolean animate(@NotNull BlueprintAnimation animation, @NotNull AnimationModifier modifier) {
        return animate(animation, modifier, () -> {});
    }

    /**
     * Plays a blueprint animation with a modifier and a completion task.
     *
     * @param animation the blueprint animation
     * @param modifier the animation modifier
     * @param removeTask the task to run when the animation ends
     * @return true if the animation started
     * @since 1.15.2
     */
    public boolean animate(@NotNull BlueprintAnimation animation, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        return animate(b -> true, animation, modifier, removeTask);
    }

    /**
     * Plays an animation on filtered bones.
     *
     * @param filter the bone filter
     * @param animation the animation name
     * @param modifier the animation modifier
     * @param removeTask the task to run when the animation ends
     * @return true if the animation started
     * @since 1.15.2
     */
    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        return animate(filter, animation, modifier, AnimationEventHandler.start().onAnimationRemove(removeTask));
    }

    /**
     * Plays an animation on filtered bones with an event handler.
     *
     * @param filter the bone filter
     * @param animation the animation name
     * @param modifier the animation modifier
     * @param eventHandler the animation event handler
     * @return true if the animation started
     * @since 1.15.2
     */
    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, @NotNull AnimationModifier modifier, @NotNull AnimationEventHandler eventHandler) {
        return renderer().animation(animation)
            .map(get -> animate(filter, get, modifier, eventHandler))
            .orElse(false);
    }

    /**
     * Plays a blueprint animation on filtered bones.
     *
     * @param filter the bone filter
     * @param animation the blueprint animation
     * @param modifier the animation modifier
     * @param removeTask the task to run when the animation ends
     * @return true if the animation started
     * @since 1.15.2
     */
    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull BlueprintAnimation animation, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        return animate(filter, animation, modifier, AnimationEventHandler.start().onAnimationRemove(removeTask));
    }

    /**
     * Plays a blueprint animation on filtered bones with an event handler.
     *
     * @param filter the bone filter
     * @param animation the blueprint animation
     * @param modifier the animation modifier
     * @param eventHandler the animation event handler
     * @return true if the animation started
     * @since 1.15.2
     */
    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull BlueprintAnimation animation, @NotNull AnimationModifier modifier, @NotNull AnimationEventHandler eventHandler) {
        var script = animation.script(modifier);
        if (script != null) scriptProcessor.addAnimation(animation.name(), script.iterator(modifier), modifier, AnimationEventHandler.start());
        return pipeline.matchTree(AnimationPredicate.of(filter), (b, a) -> b.addAnimation(a, animation, modifier, eventHandler));
    }

    /**
     * Stops an animation by name.
     *
     * @param animation the animation name
     * @return true if the animation was stopped
     * @since 1.15.2
     */
    public boolean stopAnimation(@NotNull String animation) {
        return stopAnimation(e -> true, animation);
    }

    /**
     * Stops an animation on filtered bones.
     *
     * @param filter the bone filter
     * @param animation the animation name
     * @return true if the animation was stopped
     * @since 1.15.2
     */
    public boolean stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String animation) {
        return stopAnimation(filter, animation, null);
    }

    /**
     * Stops an animation on filtered bones for a specific player (optional).
     *
     * @param filter the bone filter
     * @param animation the animation name
     * @param player the player (can be null)
     * @return true if the animation was stopped
     * @since 1.15.2
     */
    public boolean stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, @Nullable Player player) {
        var script = scriptProcessor.stopAnimation(animation);
        return pipeline.matchTree(b -> b.stopAnimation(filter, animation, player)) || script;
    }

    /**
     * Replaces a running animation with a new one.
     *
     * @param target the name of the animation to replace
     * @param animation the name of the new animation
     * @param modifier the modifier for the new animation
     * @return true if the replacement occurred
     * @since 1.15.2
     */
    public boolean replace(@NotNull String target, @NotNull String animation, @NotNull AnimationModifier modifier) {
        return replace(t -> true, target, animation, modifier);
    }

    /**
     * Replaces a running animation on filtered bones.
     *
     * @param filter the bone filter
     * @param target the name of the animation to replace
     * @param animation the name of the new animation
     * @param modifier the modifier for the new animation
     * @return true if the replacement occurred
     * @since 1.15.2
     */
    public boolean replace(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull String animation, @NotNull AnimationModifier modifier) {
        return renderer().animation(animation)
            .map(get -> replace(filter, target, get, modifier))
            .orElse(false);
    }

    /**
     * Replaces a running animation on filtered bones with a blueprint animation.
     *
     * @param filter the bone filter
     * @param target the name of the animation to replace
     * @param animation the new blueprint animation
     * @param modifier the modifier for the new animation
     * @return true if the replacement occurred
     * @since 1.15.2
     */
    public boolean replace(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull BlueprintAnimation animation, @NotNull AnimationModifier modifier) {
        var script = animation.script(modifier);
        if (script != null) scriptProcessor.replaceAnimation(target, script.iterator(modifier), modifier);
        return pipeline.matchTree(AnimationPredicate.of(filter), (b, a) -> b.replaceAnimation(a, target, animation, modifier));
    }

    //--- Update action ---

    /**
     * Creates a hitbox for bones matching a predicate.
     *
     * @param entity the source entity for the hitbox
     * @param listener the hitbox listener
     * @param predicate the bone predicate
     * @return true if any hitboxes were created
     * @since 1.15.2
     */
    public boolean createHitBox(@NotNull BaseEntity entity, @Nullable HitBoxListener listener, @NotNull BonePredicate predicate) {
        return tryUpdate((b, p) -> b.createHitBox(entity, p, listener), predicate);
    }

    /**
     * Retrieves or creates a hitbox for a specific bone.
     *
     * @param entity the source entity
     * @param listener the hitbox listener
     * @param predicate the bone predicate
     * @return the hitbox, or null if not found/created
     * @since 1.15.2
     */
    public @Nullable HitBox hitbox(@NotNull BaseEntity entity, @Nullable HitBoxListener listener, @NotNull Predicate<RenderedBone> predicate) {
        return pipeline.firstNotNull(bone -> {
            if (predicate.test(bone)) {
                if (bone.getHitBox() == null) bone.createHitBox(entity, BonePredicate.TRUE, listener);
                return bone.getHitBox();
            } else return null;
        });
    }

    /**
     * Creates a nametag for bones matching a predicate.
     *
     * @param predicate the bone predicate
     * @param consumer a consumer to configure the nametag
     * @return true if any nametags were created
     * @since 1.15.2
     */
    public boolean createNametag(@NotNull BonePredicate predicate, @NotNull BiConsumer<RenderedBone, ModelNametag> consumer) {
        return tryUpdate((b, p) -> b.createNametag(p, tag -> {
            consumer.accept(b, tag);
            perPlayerTick((tracker, player) -> {
                if (pipeline.getSource() instanceof RenderSource.Entity entity && entity.entity().uuid().equals(player.getUniqueId())) return;
                tag.teleport(tracker.location());
                tag.send(player);
            });
        }), predicate);
    }

    /**
     * Forces an update action on all bones.
     *
     * @param action the update action
     * @param <T> the action type
     * @since 1.15.2
     */
    public <T extends TrackerUpdateAction> void update(@NotNull T action) {
        update(action, BonePredicate.TRUE);
    }

    /**
     * Forces an update action on filtered bones.
     *
     * @param action the update action
     * @param predicate the bone predicate
     * @param <T> the action type
     * @since 1.15.2
     */
    public <T extends TrackerUpdateAction> void update(@NotNull T action, @NotNull Predicate<RenderedBone> predicate) {
        update(action, BonePredicate.from(predicate));
    }

    /**
     * Forces an update action on filtered bones.
     *
     * @param action the update action
     * @param predicate the bone predicate
     * @param <T> the action type
     * @since 1.15.2
     */
    public <T extends TrackerUpdateAction> void update(@NotNull T action, @NotNull BonePredicate predicate) {
        if (tryUpdate(action, predicate)) forceUpdate(true);
    }

    /**
     * Tries to apply an update action to bones matching a predicate.
     *
     * @param action the update action
     * @param predicate the bone predicate
     * @return true if any bones were updated
     * @since 1.15.2
     */
    public boolean tryUpdate(@NotNull BiPredicate<RenderedBone, BonePredicate> action, @NotNull BonePredicate predicate) {
        return pipeline.matchTree(predicate, action);
    }

    /**
     * Retrieves a bone by name.
     *
     * @param name the bone name
     * @return the bone, or null if not found
     * @since 1.15.2
     */
    public @Nullable RenderedBone bone(@NotNull BoneName name) {
        return pipeline.boneOf(name);
    }

    /**
     * Retrieves a bone by name string.
     *
     * @param name the bone name
     * @return the bone, or null if not found
     * @since 1.15.2
     */
    public @Nullable RenderedBone bone(@NotNull String name) {
        return bone(BonePredicate.name(name));
    }

    /**
     * Retrieves the first bone matching a predicate.
     *
     * @param predicate the bone predicate
     * @return the bone, or null if not found
     * @since 1.15.2
     */
    public @Nullable RenderedBone bone(@NotNull Predicate<RenderedBone> predicate) {
        return bones().stream()
            .filter(predicate)
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns a collection of all bones in the model.
     *
     * @return the bones
     * @since 1.15.2
     */
    public @NotNull @Unmodifiable Collection<RenderedBone> bones() {
        return pipeline.bones();
    }

    /**
     * Returns a stream of all model displays.
     *
     * @return the displays
     * @since 1.15.2
     */
    public @NotNull Stream<ModelDisplay> displays() {
        return bones().stream()
            .map(RenderedBone::getDisplay)
            .filter(Objects::nonNull);
    }

    /**
     * Hides the tracker from a specific player.
     *
     * @param player the target player
     * @return true if hidden successfully
     * @since 1.15.2
     */
    public boolean hide(@NotNull Player player) {
        return EventUtil.call(new PlayerHideTrackerEvent(this, player)) && pipeline.hide(player);
    }

    /**
     * Checks if the tracker is hidden from a specific player.
     *
     * @param player the target player
     * @return true if hidden
     * @since 1.15.2
     */
    public boolean isHide(@NotNull Player player) {
        return pipeline.isHide(player);
    }

    /**
     * Shows the tracker to a specific player.
     *
     * @param player the target player
     * @return true if shown successfully
     * @since 1.15.2
     */
    public boolean show(@NotNull Player player) {
        return EventUtil.call(new PlayerShowTrackerEvent(this, player)) && pipeline.show(player);
    }

    /**
     * Registers a handler for the tracker close event.
     *
     * @param consumer the handler
     * @since 1.15.2
     */
    public void handleCloseEvent(@NotNull BiConsumer<Tracker, CloseReason> consumer) {
        closeEventHandler = closeEventHandler.andThen(Objects.requireNonNull(consumer));
    }

    /**
     * Checks if the model is spawned for a player (by UUID).
     *
     * @param uuid the player UUID
     * @return true if spawned
     * @since 1.15.2
     */
    public boolean isSpawned(@NotNull UUID uuid) {
        return pipeline.isSpawned(uuid);
    }

    /**
     * Checks if the model is spawned for a player.
     *
     * @param player the player
     * @return true if spawned
     * @since 1.15.2
     */
    public boolean isSpawned(@NotNull Player player) {
        return isSpawned(player.getUniqueId());
    }

    /**
     * Returns the renderer associated with this tracker.
     *
     * @return the renderer
     * @since 1.15.2
     */
    public @NotNull ModelRenderer renderer() {
        return pipeline.getParent();
    }

    /**
     * Marks the tracker for removal.
     *
     * @param removal true to mark for removal
     * @since 1.15.2
     */
    @ApiStatus.Internal
    public void forRemoval(boolean removal) {
        forRemoval.set(removal);
    }

    /**
     * Checks if the tracker is marked for removal.
     *
     * @return true if marked for removal
     * @since 1.15.2
     */
    @ApiStatus.Internal
    public boolean forRemoval() {
        return forRemoval.get();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Tracker tracker)) return false;
        return name().equals(tracker.name());
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    /**
     * Functional interface for handling scheduled packets.
     *
     * @since 1.15.2
     */
    @FunctionalInterface
    public interface ScheduledPacketHandler {
        /**
         * Handles packets for a tracker.
         *
         * @param tracker the tracker
         * @param bundlerSet the set of packet bundlers
         * @since 1.15.2
         */
        void handle(@NotNull Tracker tracker, @NotNull BundlerSet bundlerSet);

        /**
         * Chains this handler with another.
         *
         * @param other the other handler
         * @return the combined handler
         * @since 1.15.2
         */
        default @NotNull ScheduledPacketHandler then(@NotNull ScheduledPacketHandler other) {
            return (t, s) -> {
                handle(t, s);
                other.handle(t, s);
            };
        }
    }

    /**
     * Holds different types of packet bundlers for a tracker tick.
     *
     * @since 1.15.2
     */
    public class BundlerSet {
        @Getter
        private PacketBundler tickBundler = pipeline.createBundler();
        @Getter
        private PacketBundler dataBundler = pipeline.createLazyBundler();
        @Getter
        private PacketBundler viewBundler = pipeline.createParallelBundler();

        private final Map<UUID, PerPlayerCache> perPlayerViewBundler = new ConcurrentHashMap<>();

        /**
         * Private initializer
         */
        private BundlerSet() {
        }

        private void send() {
            globalSend();
            perPlayerSend();
        }

        private void perPlayerSend() {
            perPlayerViewBundler.values().forEach(PerPlayerCache::send);
        }

        private void globalSend() {
            if (tickBundler.isNotEmpty()) {
                pipeline.allPlayer().forEach(tickBundler::send);
                tickBundler = pipeline.createBundler();
            }
            if (dataBundler.isNotEmpty()) {
                pipeline.nonHidePlayer().forEach(dataBundler::send);
                dataBundler = pipeline.createLazyBundler();
            }
            if (viewBundler.isNotEmpty()) {
                pipeline.viewedPlayer().filter(p -> !perPlayerViewBundler.containsKey(p.getUniqueId())).forEach(viewBundler::send);
                viewBundler = pipeline.createParallelBundler();
            }
        }
    }

    @RequiredArgsConstructor
    private class PerPlayerCache {
        private final UUID uuid;
        private final AtomicInteger counter = new AtomicInteger();
        private PacketBundler bundler = pipeline.createParallelBundler();

        private @NotNull Optional<PlayerChannelHandler> channel() {
            return Optional.ofNullable(pipeline.channel(uuid));
        }

        public void add() {
            if (counter.getAndIncrement() == 0) {
                channel().ifPresent(handler -> EventUtil.call(new PlayerPerAnimationStartEvent(Tracker.this, handler.player())));
            }
        }

        public void remove() {
            if (counter.decrementAndGet() == 0) {
                bundlerSet.perPlayerViewBundler.remove(uuid);
                channel().ifPresent(handler -> {
                    var bundler = pipeline.createBundler();
                    pipeline.iterateTree(bone -> bone.forceTransformation(bundler));
                    bundler.send(handler.player());
                    EventUtil.call(new PlayerPerAnimationEndEvent(Tracker.this, handler.player()));
                });
            }
        }

        private void send() {
            if (pipeline.tick(uuid, bundler) && bundler.isNotEmpty()) {
                channel().ifPresent(handler -> bundler.send(handler.player()));
                bundler = pipeline.createParallelBundler();
            }
        }
    }

    @Override
    public String toString() {
        return name();
    }

    /**
     * Reason for closing a tracker.
     *
     * @since 1.15.2
     */
    @RequiredArgsConstructor
    public enum CloseReason {
        /**
         * The tracker was manually removed.
         * @since 1.15.2
         */
        REMOVE(false),
        /**
         * The plugin is being disabled.
         * @since 1.15.2
         */
        PLUGIN_DISABLE(true),
        /**
         * The entity or tracker was despawned.
         * @since 1.15.2
         */
        DESPAWN(true)
        ;
        private final boolean save;

        /**
         * Checks if the tracker state should be saved.
         *
         * @return true if it should be saved
         * @since 1.15.2
         */
        public boolean shouldBeSave() {
            return save;
        }
    }
}

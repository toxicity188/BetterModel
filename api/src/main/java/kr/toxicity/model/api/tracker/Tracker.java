package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationEventHandler;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.animation.AnimationStateHandler;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.BoneTags;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.config.DebugConfig;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.event.*;
import kr.toxicity.model.api.nms.*;
import kr.toxicity.model.api.script.AnimationScript;
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
import java.util.function.*;
import java.util.stream.Stream;

/**
 * Tracker of a model.
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
     * Tracker tick interval
     */
    public static final int TRACKER_TICK_INTERVAL = 10;
    /**
     * Multiplier value for convert tracker tick to minecraft tick
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
            (a, s, t) -> s == AnimationStateHandler.MappingState.PROGRESS ? a.time(t) : AnimationScript.EMPTY.time(t),
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
     * Creates tracker
     * @param pipeline target instance
     * @param modifier modifier
     */
    public Tracker(@NotNull RenderPipeline pipeline, @NotNull TrackerModifier modifier) {
        this.pipeline = pipeline;
        this.modifier = modifier;
        bundlerSet = new BundlerSet();
        updater = () -> {
            if (frame % MINECRAFT_TICK_MULTIPLIER == 0) {
                Runnable task;
                while ((task = queuedTask.poll()) != null) task.run();
            }
            handler.handle(this, bundlerSet);
            bundlerSet.send();
        };
        if (modifier.sightTrace()) pipeline.viewFilter(p -> EntityUtil.canSee(p.getEyeLocation(), location()));
        frame((t, s) -> {
            if (readyForForceUpdate.compareAndSet(true, false)) t.pipeline.iterateTree(b -> b.forceUpdate(s.dataBundler));
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
    }

    /**
     * Returns this model is being scheduled.
     * @return is scheduled
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
                updater.run();
                frame++;
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
     * Gets model rotation.
     * @return rotation
     */
    public @NotNull ModelRotation rotation() {
        return rotator.apply(this, rotationSupplier.get());
    }

    public final void rotation(@NotNull Supplier<ModelRotation> supplier) {
        this.rotationSupplier = Objects.requireNonNull(supplier);
    }

    public final void rotator(@NotNull ModelRotator rotator) {
        this.rotator = Objects.requireNonNull(rotator);
    }

    public @NotNull ModelScaler scaler() {
        return scaler;
    }

    public void scaler(@NotNull ModelScaler scaler) {
        this.scaler = Objects.requireNonNull(scaler);
    }

    /**
     * Register this task on next tick
     * @param runnable task
     */
    public void task(@NotNull Runnable runnable) {
        queuedTask.add(Objects.requireNonNull(runnable));
    }

    /**
     * Runs consumer on frame.
     * @param handler handler
     */
    public synchronized void frame(@NotNull ScheduledPacketHandler handler) {
        this.handler = this.handler.then(Objects.requireNonNull(handler));
    }
    /**
     * Runs consumer on tick.
     * @param handler handler
     */
    public void tick(@NotNull ScheduledPacketHandler handler) {
        tick(1, handler);
    }
    /**
     * Runs consumer on tick.
     * @param tick tick
     * @param handler handler
     */
    public void tick(long tick, @NotNull ScheduledPacketHandler handler) {
        schedule(MINECRAFT_TICK_MULTIPLIER * tick, handler);
    }

    /**
     * Runs consumer on tick per player.
     * @param perPlayerHandler player
     */
    public synchronized void perPlayerTick(@NotNull BiConsumer<Tracker, Player> perPlayerHandler) {
        var previous = this.perPlayerHandler;
        this.perPlayerHandler = previous == null ? perPlayerHandler : previous.andThen(perPlayerHandler);
    }

    /**
     * Schedules some task.
     * @param period period
     * @param handler handler
     */
    public void schedule(long period, @NotNull ScheduledPacketHandler handler) {
        Objects.requireNonNull(handler);
        if (period <= 0) throw new RuntimeException("period cannot be <= 0");
        frame((t, s) -> {
            if (frame % period == 0) handler.handle(t, s);
        });
    }

    protected void update() {
        updater.run();
    }

    /**
     * Gets tracker name
     * @return name
     */
    public @NotNull String name() {
        return pipeline.name();
    }

    /**
     * Gets tracker model's height
     * @return height
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
     * Checks this tracker is closed
     * @return is closed
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
     * Despawns this tracker to all players
     */
    public void despawn() {
        if (!isClosed()) {
            pipeline.despawn();
            LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> getClass().getSimpleName() + " despawned: " + name());
        }
    }

    /**
     * Gets tracker modifier
     * @return tracker modifier
     */
    public @NotNull TrackerModifier modifier() {
        return modifier;
    }

    /**
     * Pauses this tracker's tick
     * @param pause pause
     * @return success
     */
    public boolean pause(boolean pause) {
        return tickPause.compareAndSet(!pause, pause);
    }

    /**
     * Forces packet update.
     * @param force force
     * @return success
     */
    public boolean forceUpdate(boolean force) {
        return readyForForceUpdate.compareAndSet(!force, force);
    }

    /**
     * Creates model spawn packet and registers player.
     * @param player target player
     * @param bundler bundler
     * @return success
     */
    protected boolean spawn(@NotNull Player player, @NotNull PacketBundler bundler) {
        if (isClosed()) return false;
        if (!EventUtil.call(new ModelSpawnAtPlayerEvent(player, this))) return false;
        var result = pipeline.spawn(player, bundler);
        if (result) LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> getClass().getSimpleName() + " is spawned at player " + player.getName() + ": " + name());
        return result;
    }

    /**
     * Removes model from player
     * @param player player
     * @return success
     */
    public boolean remove(@NotNull Player player) {
        if (isClosed()) return false;
        EventUtil.call(new ModelDespawnAtPlayerEvent(player, this));
        var result = pipeline.remove(player);
        if (result) LogUtil.debug(DebugConfig.DebugOption.TRACKER, () -> getClass().getSimpleName() + " is despawned at player " + player.getName() + ": " + name());
        return result;
    }

    /**
     * Gets number of viewed players.
     * @return viewed player amount
     */
    public int playerCount() {
        return pipeline.playerCount();
    }

    /**
     * Gets viewed players.
     * @return viewed players list
     */
    public @NotNull Stream<Player> viewedPlayer() {
        return pipeline.viewedPlayer();
    }

    /**
     * Gets location of a model.
     * @return location
     */
    public abstract @NotNull Location location();

    /**
     * Players this animation by once
     * @param animation animation's name
     * @return success
     */
    public boolean animate(@NotNull String animation) {
        return animate(animation, AnimationModifier.DEFAULT);
    }

    /**
     * Players this animation by once
     * @param animation animation's name
     * @param modifier modifier
     * @return success
     */
    public boolean animate(@NotNull String animation, AnimationModifier modifier) {
        return animate(animation, modifier, () -> {});
    }

    /**
     * Players this animation by once
     * @param animation animation's name
     * @param modifier modifier
     * @param removeTask remove task
     * @return success
     */
    public boolean animate(@NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        return animate(b -> true, animation, modifier, removeTask);
    }

    /**
     * Players this animation by once
     * @param filter bone predicate
     * @param animation animation's name
     * @param modifier modifier
     * @param removeTask remove task
     * @return success
     */
    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        return animate(filter, animation, modifier, AnimationEventHandler.start().onAnimationRemove(removeTask));
    }

    /**
     * Players this animation by once
     * @param filter bone predicate
     * @param animation animation's name
     * @param modifier modifier
     * @param eventHandler event handler
     * @return success
     */
    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, @NotNull AnimationModifier modifier, @NotNull AnimationEventHandler eventHandler) {
        return pipeline.getParent().animation(animation)
                .map(get -> animate(filter, get, modifier, eventHandler))
                .orElse(false);
    }

    /**
     * Players this animation by once
     * @param filter bone predicate
     * @param animation animation
     * @param modifier modifier
     * @param removeTask remove task
     * @return success
     */
    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull BlueprintAnimation animation, @NotNull AnimationModifier modifier, @NotNull Runnable removeTask) {
        return animate(filter, animation, modifier, AnimationEventHandler.start().onAnimationRemove(removeTask));
    }

    /**
     * Players this animation by once
     * @param filter bone predicate
     * @param animation animation
     * @param modifier modifier
     * @param eventHandler event handler
     * @return success
     */
    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull BlueprintAnimation animation, @NotNull AnimationModifier modifier, @NotNull AnimationEventHandler eventHandler) {
        var script = animation.script(modifier);
        if (script != null) scriptProcessor.addAnimation(animation.name(), script.iterator(), modifier, AnimationEventHandler.start());
        return pipeline.animate(filter, animation, modifier, eventHandler);
    }

    /**
     * Stops some animation
     * @param animation animation's name
     * @return success
     */
    public boolean stopAnimation(@NotNull String animation) {
        return stopAnimation(e -> true, animation);
    }

    /**
     * Stops some animation
     * @param filter bone predicate
     * @param animation animation's name
     * @return success
     */
    public boolean stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String animation) {
        return stopAnimation(filter, animation, null);
    }

    /**
     * Stops some animation
     * @param filter bone predicate
     * @param animation animation's name
     * @param player player
     * @return success
     */
    public boolean stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, @Nullable Player player) {
        var script = scriptProcessor.stopAnimation(animation);
        return pipeline.stopAnimation(filter, animation, player) || script;
    }

    /**
     * Replaces some animation by loop
     * @param target old animation's name
     * @param animation new animation's name
     * @param modifier modifier
     * @return success
     */
    public boolean replace(@NotNull String target, @NotNull String animation, @NotNull AnimationModifier modifier) {
        return replace(t -> true, target, animation, modifier);
    }

    /**
     * Replaces some animation by loop
     * @param filter bone predicate
     * @param target old animation's name
     * @param animation new animation's name
     * @param modifier modifier
     * @return success
     */
    public boolean replace(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull String animation, @NotNull AnimationModifier modifier) {
        return pipeline.getParent().animation(animation)
                .map(get -> replace(filter, target, get, modifier))
                .orElse(false);
    }

    /**
     * Replaces some animation by loop
     * @param filter bone predicate
     * @param target old animation's name
     * @param animation new animation
     * @param modifier modifier
     * @return success
     */
    public boolean replace(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull BlueprintAnimation animation, @NotNull AnimationModifier modifier) {
        var script = animation.script(modifier);
        if (script != null) scriptProcessor.replaceAnimation(target, script.iterator(), modifier);
        return pipeline.replace(filter, target, animation, modifier);
    }

    //--- Update action ---

    /**
     * Toggles red tint of a model.
     * @param rgb toggle
     */
    public void tint(int rgb) {
        if (tint(BonePredicate.TRUE, rgb)) forceUpdate(true);
    }

    /**
     * Toggles red tint of a model.
     * @param predicate predicate
     * @param rgb toggle
     * @return success
     */
    public boolean tint(@NotNull BonePredicate predicate, int rgb) {
        return tryUpdate(TrackerUpdateAction.tint(rgb), predicate);
    }

    /**
     * Creates hitbox based on some entity
     * @param entity entity base
     * @param listener listener
     * @param predicate predicate
     * @return success
     */
    public boolean createHitBox(@NotNull EntityAdapter entity, @Nullable HitBoxListener listener, @NotNull BonePredicate predicate) {
        return tryUpdate((b, p) -> b.createHitBox(entity, p, listener), predicate);
    }

    /**
     * Get or creates model's hitbox
     * @param entity entity
     * @param predicate predicate
     * @param listener listener
     * @return hitbox or null
     */
    public @Nullable HitBox hitbox(@NotNull EntityAdapter entity, @Nullable HitBoxListener listener, @NotNull Predicate<RenderedBone> predicate) {
        return pipeline.firstNotNull(bone -> {
            if (predicate.test(bone)) {
                if (bone.getHitBox() == null) bone.createHitBox(entity, BonePredicate.TRUE, listener);
                return bone.getHitBox();
            } else return null;
        });
    }

    /**
     * Creates nametag
     * @param predicate predicate
     * @param consumer nametag consumer
     * @return success
     */
    public boolean createNametag(@NotNull BonePredicate predicate, @NotNull BiConsumer<RenderedBone, ModelNametag> consumer) {
        return tryUpdate((b, p) -> b.createNametag(p, tag -> {
            consumer.accept(b, tag);
            perPlayerTick((tracker, player) -> {
                if (pipeline.getSource() instanceof RenderSource.BasePlayer(Player entity) && entity == player) return;
                tag.teleport(tracker.location());
                tag.send(player);
            });
        }), predicate);
    }

    /**
     * Forces update of this tracker.
     * @param action action
     * @param <T> action type
     */
    public <T extends TrackerUpdateAction> void update(@NotNull T action) {
        update(action, BonePredicate.TRUE);
    }

    /**
     * Forces update of this tracker.
     * @param action action
     * @param predicate predicate
     * @param <T> action type
     */
    public <T extends TrackerUpdateAction> void update(@NotNull T action, @NotNull Predicate<RenderedBone> predicate) {
        update(action, BonePredicate.from(predicate));
    }

    /**
     * Forces update of this tracker.
     * @param action action
     * @param predicate predicate
     * @param <T> action type
     */
    public <T extends TrackerUpdateAction> void update(@NotNull T action, @NotNull BonePredicate predicate) {
        if (tryUpdate(action, predicate)) forceUpdate(true);
    }

    /**
     * Update data of this tracker.
     * @param action action
     * @param predicate predicate
     * @return success
     */
    public boolean tryUpdate(@NotNull BiPredicate<RenderedBone, BonePredicate> action, @NotNull BonePredicate predicate) {
        return pipeline.matchTree(predicate, action);
    }

    /**
     * Gets bone by bone's name
     * @param name bone's name
     * @return bone or null
     */
    public @Nullable RenderedBone bone(@NotNull BoneName name) {
        return pipeline.boneOf(name);
    }

    /**
     * Gets bone by bone's name
     * @param name bone's name
     * @return bone or null
     */
    public @Nullable RenderedBone bone(@NotNull String name) {
        return bone(b -> b.name().name().equals(name));
    }

    /**
     * Gets bone by bone's name
     * @param predicate bone's predicate
     * @return bone or null
     */
    public @Nullable RenderedBone bone(@NotNull Predicate<RenderedBone> predicate) {
        return pipeline.boneOf(predicate);
    }

    /**
     * Gets all bones
     * @return all bones
     */
    public @NotNull @Unmodifiable Collection<RenderedBone> bones() {
        return pipeline.bones();
    }

    /**
     * Gets all model displays
     * @return all model displays
     */
    public @NotNull Stream<ModelDisplay> displays() {
        return bones().stream()
                .map(RenderedBone::getDisplay)
                .filter(Objects::nonNull);
    }

    /**
     * Hides this tracker to some player
     * @param player target player
     * @return success
     */
    public boolean hide(@NotNull Player player) {
        return EventUtil.call(new PlayerHideTrackerEvent(this, player)) && pipeline.hide(player);
    }

    /**
     * Checks this player is marked as hide
     * @param player target player
     * @return hide
     */
    public boolean isHide(@NotNull Player player) {
        return pipeline.isHide(player);
    }

    /**
     * Shows this tracker to some player
     * @param player target player
     * @return success
     */
    public boolean show(@NotNull Player player) {
        return EventUtil.call(new PlayerShowTrackerEvent(this, player)) && pipeline.show(player);
    }

    public void handleCloseEvent(@NotNull BiConsumer<Tracker, CloseReason> consumer) {
        closeEventHandler = closeEventHandler.andThen(Objects.requireNonNull(consumer));
    }

    /**
     * Checks this player is spawned in this tracker
     * @param uuid uuid
     * @return is spawned
     */
    public boolean isSpawned(@NotNull UUID uuid) {
        return pipeline.isSpawned(uuid);
    }

    /**
     * Checks this player is spawned in this tracker
     * @param player player
     * @return is spawned
     */
    public boolean isSpawned(@NotNull Player player) {
        return isSpawned(player.getUniqueId());
    }

    /**
     * Gets the renderer of this tracker
     * @return renderer
     */
    public @NotNull ModelRenderer renderer() {
        return pipeline.getParent();
    }

    /**
     * Marks future will remove this tracker
     * @param removal removal
     */
    @ApiStatus.Internal
    public void forRemoval(boolean removal) {
        forRemoval.set(removal);
    }

    /**
     * Checks this tracker is for removal
     * @return for removal
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
     * Scheduled packet handler
     */
    @FunctionalInterface
    public interface ScheduledPacketHandler {
        /**
         * Handles packet
         * @param tracker tracker
         * @param bundlerSet bundler set
         */
        void handle(@NotNull Tracker tracker, @NotNull BundlerSet bundlerSet);

        /**
         * Plus another handler
         * @param other other
         * @return merged handler
         */
        default @NotNull ScheduledPacketHandler then(@NotNull ScheduledPacketHandler other) {
            return (t, s) -> {
                handle(t, s);
                other.handle(t, s);
            };
        }
    }

    /**
     * Bundler set
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

    @RequiredArgsConstructor
    public enum CloseReason {
        REMOVE(false),
        DESPAWN(true)
        ;
        private final boolean save;

        public boolean shouldBeSave() {
            return save;
        }
    }
}

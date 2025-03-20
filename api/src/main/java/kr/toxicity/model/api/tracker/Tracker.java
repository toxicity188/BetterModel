package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.AnimationModifier;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.entity.RenderedEntity;
import kr.toxicity.model.api.entity.TrackerMovement;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.util.EntityUtil;
import kr.toxicity.model.api.util.FunctionUtil;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Tracker of model.
 */
public abstract class Tracker implements AutoCloseable {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(256);
    /**
     * Tracker's namespace.
     */
    public static final NamespacedKey TRACKING_ID = Objects.requireNonNull(NamespacedKey.fromString("bettermodel_tracker"));

    protected final RenderInstance instance;
    private final ScheduledFuture<?> task;
    private final AtomicBoolean runningSingle = new AtomicBoolean();
    private final AtomicLong frame = new AtomicLong();
    private final AtomicBoolean readyForForceUpdate = new AtomicBoolean();

    private final TrackerModifier modifier;

    private ModelRotation rotation = ModelRotation.EMPTY;
    @Getter
    private Supplier<TrackerMovement> movement;

    private Consumer<Tracker> consumer = t -> {};

    /**
     * Tracker
     * @param instance target instance
     */
    public Tracker(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        this.instance = instance;
        this.modifier = modifier;
        this.movement = FunctionUtil.memoizeTick(() -> new TrackerMovement(new Vector3f(), new Vector3f(modifier.scale()), new Vector3f()));
        task = EXECUTOR.scheduleAtFixedRate(() -> {
            consumer.accept(this);
            var bundle = BetterModel.inst().nms().createBundler();
            if (readyForForceUpdate.get()) {
                instance.forceUpdate(bundle);
                readyForForceUpdate.set(false);
            }
            instance.move(
                    frame.incrementAndGet() % 5 == 0 ? (isRunningSingleAnimation() && BetterModel.inst().configManager().lockOnPlayAnimation()) ? rotation : (rotation = rotation()) : null,
                    movement.get(),
                    bundle
            );
            if (!bundle.isEmpty()) instance.viewedPlayer().forEach(bundle::send);
        }, 10, 10, TimeUnit.MILLISECONDS);
        tint(0xFFFFFF);
        if (modifier.sightTrace()) instance.filter(p -> EntityUtil.canSee(p.getEyeLocation(), location()));
        tick(t -> t.instance.getScriptProcessor().tick());
    }

    public abstract @NotNull ModelRotation rotation();

    /**
     * Runs consumer on frame.
     * @param consumer consumer
     */
    public void frame(@NotNull Consumer<Tracker> consumer) {
        this.consumer = this.consumer.andThen(consumer);
    }
    /**
     * Runs consumer on tick.
     * @param consumer consumer
     */
    public void tick(@NotNull Consumer<Tracker> consumer) {
        frame(t -> {
            if (frame.get() % 5 == 0) consumer.accept(t);
        });
    }

    /**
     * Gets tracker name
     * @return name
     */
    public @NotNull String name() {
        return instance.getParent().name();
    }

    /**
     * Gets tracker model's height
     * @return height
     */
    public double height() {
        return instance.height();
    }

    @Override
    public void close() throws Exception {
        task.cancel(true);
        instance.close();
    }

    /**
     * Sets tracker movement.
     * @param movement movement
     */
    public void setMovement(Supplier<TrackerMovement> movement) {
        instance.lastMovement(movement.get());
        this.movement = FunctionUtil.memoizeTick(movement);
    }

    public @NotNull TrackerModifier modifier() {
        return modifier;
    }


    /**
     * Forces packet update.
     */
    public void forceUpdate(boolean force) {
        readyForForceUpdate.set(force);
    }

    /**
     * Gets whether this model is playing single animation.
     * @return whether to playing single.
     */
    public boolean isRunningSingleAnimation() {
        return runningSingle.get();
    }

    /**
     * Creates model spawn packet and registers player.
     * @param player target player
     * @param bundler bundler
     */

    protected void spawn(@NotNull Player player, @NotNull PacketBundler bundler) {
        instance.spawn(player, bundler);
    }

    /**
     * Removes model from player
     * @param player player
     */
    public void remove(@NotNull Player player) {
        instance.remove(player);
    }

    /**
     * Gets amount of viewed players.
     * @return viewed players amount
     */
    public int viewedPlayerSize() {
        return instance.viewedPlayerSize();
    }

    /**
     * Gets viewed players.
     * @return viewed players list
     */
    public @NotNull Stream<Player> viewedPlayer() {
        return instance.viewedPlayer();
    }

    /**
     * Toggles red tint of model.
     * @param rgb toggle
     */
    public void tint(int rgb) {
        instance.tint(rgb);
    }

    /**
     * Gets location of model.
     * @return location
     */
    public abstract @NotNull Location location();

    /**
     * Gets uuid of model.
     * @return uuid
     */
    public abstract @NotNull UUID uuid();

    public boolean animateLoop(@NotNull String animation) {
        return animateLoop(animation, AnimationModifier.DEFAULT_LOOP);
    }

    public boolean animateLoop(@NotNull String animation, AnimationModifier modifier) {
        return animateLoop(animation, modifier, () -> {});
    }

    public boolean animateLoop(@NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        return animateLoop(e -> true, animation, modifier, removeTask);
    }

    public boolean animateLoop(@NotNull Predicate<RenderedEntity> filter, @NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        return instance.animateLoop(filter, animation, modifier, removeTask);
    }

    public boolean animateSingle(@NotNull String animation) {
        return animateSingle(animation, AnimationModifier.DEFAULT);
    }

    public boolean animateSingle(@NotNull String animation, AnimationModifier modifier) {
        return animateSingle(animation, modifier, () -> {});
    }

    public boolean animateSingle(@NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        return animateSingle(e -> true, animation, modifier, removeTask);
    }

    public boolean animateSingle(@NotNull Predicate<RenderedEntity> filter, @NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        var success = instance.animateSingle(filter, animation, modifier, wrapToSingle(removeTask));
        if (success) runningSingle.set(true);
        return success;
    }

    public void stopAnimation(@NotNull String animation) {
        stopAnimation(e -> true, animation);
    }
    public void stopAnimation(@NotNull Predicate<RenderedEntity> filter, @NotNull String animation) {
        instance.stopAnimation(filter, animation);
    }

    private Runnable wrapToSingle(@NotNull Runnable runnable) {
        return () -> {
            runnable.run();
            runningSingle.set(false);
        };
    }


    public boolean replaceLoop(@NotNull String target, @NotNull String animation) {
        return replaceLoop(e -> true, target, animation);
    }
    public boolean replaceSingle(@NotNull String target, @NotNull String animation) {
        return replaceSingle(e -> true, target, animation);
    }

    public boolean replaceLoop(@NotNull Predicate<RenderedEntity> filter, @NotNull String target, @NotNull String animation) {
        return instance.replaceLoop(filter, target, animation);
    }

    public boolean replaceSingle(@NotNull Predicate<RenderedEntity> filter, @NotNull String target, @NotNull String animation) {
        var success = instance.replaceSingle(filter, target, animation);
        if (success) runningSingle.set(true);
        return success;
    }

    public void togglePart(@NotNull Predicate<RenderedEntity> predicate, boolean toggle) {
        instance.togglePart(predicate, toggle);
    }

    public @Nullable RenderedEntity entity(@NotNull String name) {
        return instance.renderers().stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    public @NotNull List<RenderedEntity> entity() {
        return instance.renderers();
    }

    public @NotNull List<ModelDisplay> displays() {
        return instance.renderers().stream()
                .map(RenderedEntity::getDisplay)
                .filter(Objects::nonNull)
                .toList();
    }
}

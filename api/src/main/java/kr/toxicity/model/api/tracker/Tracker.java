package kr.toxicity.model.api.tracker;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.event.ModelDespawnAtPlayerEvent;
import kr.toxicity.model.api.event.ModelSpawnAtPlayerEvent;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.util.*;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Tracker of a model.
 */
public abstract class Tracker implements AutoCloseable {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(256);

    /**
     * Tracker's namespace.
     */
    public static final NamespacedKey TRACKING_ID = Objects.requireNonNull(NamespacedKey.fromString("bettermodel_tracker"));

    @Getter
    protected final RenderInstance instance;
    private final ScheduledFuture<?> task;
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private final AtomicBoolean runningSingle = new AtomicBoolean();
    private final AtomicBoolean readyForForceUpdate = new AtomicBoolean();
    private final TrackerModifier modifier;
    private final Runnable updater;
    private long frame = 0;
    private Supplier<ModelRotation> rotationSupplier = () -> ModelRotation.EMPTY;

    private BiConsumer<Tracker, PacketBundler> consumer = (t, b) -> {};

    /**
     * Creates tracker
     * @param instance target instance
     * @param modifier modifier
     */
    public Tracker(@NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        this.instance = instance;
        this.modifier = modifier;
        updater = () -> {
            var bundle = BetterModel.inst().nms().createBundler();
            var moveAccepted = instance.move(
                    frame % 5 == 0 ? (isRunningSingleAnimation() && BetterModel.inst().configManager().lockOnPlayAnimation()) ? instance.getRotation() : rotation() : null,
                    bundle
            );
            if (readyForForceUpdate.compareAndSet(true, false) && !moveAccepted) instance.forceUpdate(bundle);
            consumer.accept(this, bundle);
            if (!bundle.isEmpty()) instance.viewedPlayer().forEach(bundle::send);
        };
        task = EXECUTOR.scheduleAtFixedRate(() -> {
            if (playerCount() > 0 || isRunningSingleAnimation()) updater.run();
            frame++;
        }, 10, 10, TimeUnit.MILLISECONDS);
        tint(0xFFFFFF);
        if (modifier.sightTrace()) instance.viewFilter(p -> EntityUtil.canSee(p.getEyeLocation(), location()));
        tick((t, b) -> t.instance.getScriptProcessor().tick());
    }

    /**
     * Gets model rotation.
     * @return rotation
     */
    public final @NotNull ModelRotation rotation() {
        return rotationSupplier.get();
    }

    public final void rotation(@NotNull Supplier<ModelRotation> rotationSupplier) {
        this.rotationSupplier = rotationSupplier;
    }

    /**
     * Runs consumer on frame.
     * @param consumer consumer
     */
    public void frame(@NotNull BiConsumer<Tracker, PacketBundler> consumer) {
        this.consumer = this.consumer.andThen(consumer);
    }
    /**
     * Runs consumer on tick.
     * @param consumer consumer
     */
    public void tick(@NotNull BiConsumer<Tracker, PacketBundler> consumer) {
        schedule(5, consumer);
    }

    /**
     * Schedules some task.
     * @param period period
     * @param consumer consumer
     */
    public void schedule(long period, @NotNull BiConsumer<Tracker, PacketBundler> consumer) {
        if (period <= 0) throw new RuntimeException("period cannot be <= 0");
        frame((t, b) -> {
            if (frame % period == 0) consumer.accept(t, b);
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
        return instance.getParent().name();
    }

    /**
     * Gets tracker model's height
     * @return height
     */
    public double height() {
        return instance.height();
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
        if (isClosed.compareAndSet(false, true)) {
            task.cancel(true);
            instance.despawn();
        }
    }

    /**
     * Despawns this tracker to all players
     */
    public void despawn() {
        if (!isClosed()) instance.despawn();
    }

    public @NotNull TrackerModifier modifier() {
        return modifier;
    }


    /**
     * Forces packet update.
     * @param force force
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
     * @return success
     */
    protected boolean spawn(@NotNull Player player, @NotNull PacketBundler bundler) {
        if (isClosed()) return false;
        if (!EventUtil.call(new ModelSpawnAtPlayerEvent(player, this))) return false;
        instance.spawn(player, bundler);
        return true;
    }

    /**
     * Removes model from player
     * @param player player
     * @return success
     */
    public boolean remove(@NotNull Player player) {
        if (isClosed()) return false;
        EventUtil.call(new ModelDespawnAtPlayerEvent(player, this));
        instance.remove(player);
        return true;
    }

    /**
     * Gets number of viewed players.
     * @return viewed player amount
     */
    public int playerCount() {
        return instance.playerCount();
    }

    /**
     * Gets viewed players.
     * @return viewed players list
     */
    public @NotNull Stream<Player> viewedPlayer() {
        return instance.viewedPlayer();
    }

    /**
     * Toggles red tint of a model.
     * @param rgb toggle
     */
    public void tint(int rgb) {
        tint(BonePredicate.TRUE, rgb);
    }

    /**
     * Toggles red tint of a model.
     * @param predicate predicate
     * @param rgb toggle
     */
    public void tint(@NotNull BonePredicate predicate, int rgb) {
        if (instance.tint(predicate, rgb)) forceUpdate(true);
    }

    /**
     * Gets location of a model.
     * @return location
     */
    public abstract @NotNull Location location();

    /**
     * Gets uuid of a model.
     * @return uuid
     */
    public abstract @NotNull UUID uuid();

    /**
     * Players this animation by loop
     * @param animation animation's name
     * @return success
     */
    public boolean animateLoop(@NotNull String animation) {
        return animateLoop(animation, AnimationModifier.DEFAULT_LOOP);
    }

    /**
     * Players this animation by loop
     * @param animation animation's name
     * @param modifier modifier
     * @return success
     */
    public boolean animateLoop(@NotNull String animation, AnimationModifier modifier) {
        return animateLoop(animation, modifier, () -> {});
    }

    /**
     * Players this animation by loop
     * @param animation animation's name
     * @param modifier modifier
     * @param removeTask remove task
     * @return success
     */
    public boolean animateLoop(@NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        return animateLoop(e -> true, animation, modifier, removeTask);
    }

    /**
     * Players this animation by loop
     * @param filter bone predicate
     * @param animation animation's name
     * @param modifier modifier
     * @param removeTask remove task
     * @return success
     */
    public boolean animateLoop(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        return instance.animateLoop(filter, animation, modifier, removeTask);
    }

    /**
     * Players this animation by once
     * @param animation animation's name
     * @return success
     */
    public boolean animateSingle(@NotNull String animation) {
        return animateSingle(animation, AnimationModifier.DEFAULT);
    }

    /**
     * Players this animation by once
     * @param animation animation's name
     * @param modifier modifier
     * @return success
     */
    public boolean animateSingle(@NotNull String animation, AnimationModifier modifier) {
        return animateSingle(animation, modifier, () -> {});
    }

    /**
     * Players this animation by once
     * @param animation animation's name
     * @param modifier modifier
     * @param removeTask remove task
     * @return success
     */
    public boolean animateSingle(@NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        return animateSingle(e -> true, animation, modifier, removeTask);
    }

    /**
     * Players this animation by once
     * @param filter bone predicate
     * @param animation animation's name
     * @param modifier modifier
     * @param removeTask remove task
     * @return success
     */
    public boolean animateSingle(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        var success = instance.animateSingle(filter, animation, modifier, wrapToSingle(removeTask));
        if (success) runningSingle.set(true);
        return success;
    }

    /**
     * Stops some animation
     * @param animation animation's name
     */
    public void stopAnimation(@NotNull String animation) {
        stopAnimation(e -> true, animation);
    }

    /**
     * Stops some animation
     * @param filter bone predicate
     * @param animation animation's name
     */
    public void stopAnimation(@NotNull Predicate<RenderedBone> filter, @NotNull String animation) {
        instance.stopAnimation(filter, animation);
    }

    private Runnable wrapToSingle(@NotNull Runnable runnable) {
        return () -> {
            runnable.run();
            runningSingle.set(false);
        };
    }

    /**
     * Replaces some animation by loop
     * @param target old animation's name
     * @param animation new animation's name
     * @return success
     */
    public boolean replaceLoop(@NotNull String target, @NotNull String animation) {
        return replaceLoop(e -> true, target, animation);
    }

    /**
     * Replaces some animation by once
     * @param target old animation's name
     * @param animation new animation's name
     * @return success
     */
    public boolean replaceSingle(@NotNull String target, @NotNull String animation) {
        return replaceSingle(e -> true, target, animation);
    }

    /**
     * Replaces some animation by loop
     * @param filter bone predicate
     * @param target old animation's name
     * @param animation new animation's name
     * @return success
     */
    public boolean replaceLoop(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull String animation) {
        return instance.replaceLoop(filter, target, animation);
    }

    /**
     * Replaces some animation by once
     * @param filter bone predicate
     * @param target old animation's name
     * @param animation new animation's name
     * @return success
     */
    public boolean replaceSingle(@NotNull Predicate<RenderedBone> filter, @NotNull String target, @NotNull String animation) {
        var success = instance.replaceSingle(filter, target, animation);
        if (success) runningSingle.set(true);
        return success;
    }

    /**
     * Toggles some part
     * @param predicate predicate
     * @param toggle toggle
     * @return success
     */
    public boolean togglePart(@NotNull BonePredicate predicate, boolean toggle) {
        return instance.togglePart(predicate, toggle);
    }

    /**
     * Sets item of some model part
     * @param predicate predicate
     * @param itemStack item
     * @return success
     */
    public boolean itemStack(@NotNull BonePredicate predicate, @NotNull TransformedItemStack itemStack) {
        return instance.itemStack(predicate, itemStack);
    }

    /**
     * Sets glow of some model part
     * @param glow glow
     * @param glowColor glowColor
     * @return success
     */
    public boolean glow(@NotNull BonePredicate predicate, boolean glow, int glowColor) {
        return instance.glow(predicate, glow, glowColor);
    }

    /**
     * Sets enchantment of some model part
     * @param predicate predicate
     * @param enchant should enchant
     * @return success
     */
    public boolean enchant(@NotNull BonePredicate predicate, boolean enchant) {
        return instance.enchant(predicate, enchant);
    }

    /**
     * Sets brightness of some model part
     * @param predicate predicate
     * @param block block light
     * @param sky skylight
     * @return success
     */
    public boolean brightness(@NotNull BonePredicate predicate, int block, int sky) {
        return instance.brightness(predicate, block, sky);
    }

    /**
     * Gets bone by bone's name
     * @param name bone's name
     * @return bone or null
     */
    public @Nullable RenderedBone bone(@NotNull BoneName name) {
        return bone(b -> b.getName().equals(name));
    }

    /**
     * Gets bone by bone's name
     * @param name bone's name
     * @return bone or null
     */
    public @Nullable RenderedBone bone(@NotNull String name) {
        return bone(b -> b.getName().name().equals(name));
    }

    /**
     * Gets bone by bone's name
     * @param predicate bone's predicate
     * @return bone or null
     */
    public @Nullable RenderedBone bone(@NotNull Predicate<RenderedBone> predicate) {
        return instance.boneOf(predicate);
    }

    /**
     * Gets all bones
     * @return all bones
     */
    public @NotNull List<RenderedBone> bones() {
        return instance.bones();
    }

    /**
     * Gets all model displays
     * @return all model displays
     */
    public @NotNull List<ModelDisplay> displays() {
        return bones().stream()
                .map(RenderedBone::getDisplay)
                .filter(Objects::nonNull)
                .toList();
    }
}

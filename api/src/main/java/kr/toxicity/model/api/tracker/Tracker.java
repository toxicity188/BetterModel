package kr.toxicity.model.api.tracker;

import com.google.gson.*;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.data.renderer.RenderInstance;
import kr.toxicity.model.api.data.renderer.RenderSource;
import kr.toxicity.model.api.event.*;
import kr.toxicity.model.api.event.PlayerHideTrackerEvent;
import kr.toxicity.model.api.event.PlayerShowTrackerEvent;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.PacketBundler;
import kr.toxicity.model.api.util.function.BonePredicate;
import kr.toxicity.model.api.util.EntityUtil;
import kr.toxicity.model.api.util.EventUtil;
import kr.toxicity.model.api.util.TransformedItemStack;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
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

    public static final Gson PARSER = new GsonBuilder()
            .registerTypeAdapter(ModelScaler.class, (JsonDeserializer<ModelScaler>) (json, typeOfT, context) -> json.isJsonObject() ? ModelScaler.deserialize(json.getAsJsonObject()) : ModelScaler.defaultScaler())
            .registerTypeAdapter(ModelScaler.class, (JsonSerializer<ModelScaler>) (src, typeOfSrc, context) -> src.serialize())
            .create();

    @Getter
    protected final RenderInstance instance;
    private final ScheduledFuture<?> task;
    @Getter
    private final RenderSource source;
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private final AtomicBoolean readyForForceUpdate = new AtomicBoolean();
    private final AtomicBoolean forRemoval = new AtomicBoolean();
    private final TrackerModifier modifier;
    private final Runnable updater;
    @Getter
    private final TrackerData trackerData;
    private PacketBundler viewBundler, dataBundler;
    private long frame = 0;
    private ModelRotator rotator = ModelRotator.EMPTY;

    private BiConsumer<Tracker, PacketBundler> consumer = (t, b) -> {};

    /**
     * Creates tracker
     * @param source render source
     * @param instance target instance
     * @param modifier modifier
     */
    public Tracker(@NotNull RenderSource source, @NotNull RenderInstance instance, @NotNull TrackerModifier modifier) {
        this.instance = instance;
        this.source = source;
        this.modifier = modifier;
        this.trackerData = new TrackerData(instance.name(), modifier);
        viewBundler = instance.createBundler();
        dataBundler = instance.createBundler();
        var config = BetterModel.plugin().configManager();
        updater = () -> {
            instance.move(
                    frame % 5 == 0 ? (isRunningSingleAnimation() && config.lockOnPlayAnimation()) ? instance.getRotation() : rotation() : null,
                    viewBundler
            );
            consumer.accept(this, viewBundler);
            if (readyForForceUpdate.compareAndSet(true, false)) instance.forceUpdate(dataBundler);
            if (!dataBundler.isEmpty()) {
                instance.nonHidePlayer().forEach(dataBundler::send);
                dataBundler = instance.createBundler();
            }
            if (!viewBundler.isEmpty()) {
                instance.viewedPlayer().forEach(viewBundler::send);
                viewBundler = instance.createBundler();
            }
        };
        task = EXECUTOR.scheduleAtFixedRate(() -> {
            if (playerCount() > 0 || forRemoval.get()) updater.run();
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
        return rotator.get();
    }

    public final void rotation(@NotNull ModelRotator newRotator) {
        this.rotator = newRotator;
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
        tick(1, consumer);
    }
    /**
     * Runs consumer on tick.
     * @param tick tick
     * @param consumer consumer
     */
    public void tick(long tick, @NotNull BiConsumer<Tracker, PacketBundler> consumer) {
        schedule(5 * tick, consumer);
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
            EventUtil.call(new CloseTrackerEvent(this));
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
     * @return success
     */
    public boolean forceUpdate(boolean force) {
        var get = readyForForceUpdate.get();
        return readyForForceUpdate.compareAndSet(get, force);
    }

    /**
     * Gets whether this model is playing single animation.
     * @return whether to playing single.
     */
    public boolean isRunningSingleAnimation() {
        var runningAnimation = instance.runningAnimation();
        return runningAnimation != null && runningAnimation.type() == AnimationIterator.Type.PLAY_ONCE;
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
        return animate(e -> true, animation, modifier, removeTask);
    }

    /**
     * Players this animation by once
     * @param filter bone predicate
     * @param animation animation's name
     * @param modifier modifier
     * @param removeTask remove task
     * @return success
     */
    public boolean animate(@NotNull Predicate<RenderedBone> filter, @NotNull String animation, AnimationModifier modifier, Runnable removeTask) {
        return instance.animate(filter, animation, modifier, removeTask);
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
        return instance.replace(filter, target, animation, modifier);
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
     * Updates item
     * @param predicate predicate
     * @return success
     */
    public boolean updateItem(@NotNull BonePredicate predicate) {
        return instance.updateItem(predicate);
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

    /**
     * Hides this tracker to some player
     * @param player target player
     * @return success
     */
    public boolean hide(@NotNull Player player) {
        return EventUtil.call(new PlayerHideTrackerEvent(this, player)) && instance.hide(player);
    }

    /**
     * Checks this player is marked as hide
     * @param player target player
     * @return hide
     */
    public boolean isHide(@NotNull Player player) {
        return instance.isHide(player);
    }

    /**
     * Shows this tracker to some player
     * @param player target player
     * @return success
     */
    public boolean show(@NotNull Player player) {
        return EventUtil.call(new PlayerShowTrackerEvent(this, player)) && instance.show(player);
    }

    /**
     * Gets the renderer of this tracker
     * @return renderer
     */
    public @NotNull ModelRenderer renderer() {
        return instance.getParent();
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
}

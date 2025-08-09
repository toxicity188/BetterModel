package kr.toxicity.model.test;

import com.google.gson.JsonObject;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.event.PluginStartReloadEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static java.lang.Math.*;

public final class FightTester implements ModelTester, Listener {

    @NotNull
    private static final NamespacedKey KNIGHT_SWORD_KEY = Objects.requireNonNull(NamespacedKey.fromString("knight_sword"));

    private final Set<UUID> playerSet = ConcurrentHashMap.newKeySet();
    private ItemStack lineItem;
    private BetterModelTest test;

    @Override
    public void start(@NotNull BetterModelTest test) {
        this.test = test;
        this.lineItem = createLine();
        Bukkit.getPluginManager().registerEvents(this, test);
        var command = test.getCommand("knightsword");
        if (command != null) command.setExecutor((sender, command1, label, args) -> {
            if (sender instanceof Player player) giveKnightSword(player);
            return true;
        });
    }

    @Override
    public void end(@NotNull BetterModelTest test) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void start(@NotNull PluginStartReloadEvent event) {
        var path = event.getPackZipper()
                .modern()
                .bettermodel();
        path.models().resolve("class_item").add("knight_sword.json", test.asByte("knight_sword.json"));
        path.models().resolve("class_item").add("knight_line.json", test.asByte("knight_line.json"));
        path.textures().add("knight_sword.png", test.asByte("knight_sword.png"));
        path.textures().add("knight_line.png", test.asByte("knight_line.png"));

        var model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", "bettermodel:class_item/knight_sword");
        var json = new JsonObject();
        json.add("model", model);

        var line = new JsonObject();
        line.addProperty("type", "minecraft:model");
        line.addProperty("model", "bettermodel:class_item/knight_line");
        var lineBuilder = new JsonObject();
        lineBuilder.add("model", line);

        path.items().add("knight_sword.json", () -> json.toString().getBytes(StandardCharsets.UTF_8));
        path.items().add("knight_line.json", () -> lineBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    @EventHandler
    public void rightClick(@NotNull PlayerInteractEvent event) {
        var player = event.getPlayer();
        var uuid = player.getUniqueId();
        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> {}
            default -> {
                return;
            }
        }
        if (playerSet.contains(uuid)) return;
        if (!player.getInventory().getItemInMainHand().getPersistentDataContainer().has(KNIGHT_SWORD_KEY)) return;
        BetterModel.limb("knight")
                .map(limb -> limb.getOrCreate(player))
                .ifPresent(tracker -> {
                    var drawer = tracker.bone("sword_point");
                    if (drawer == null) {
                        tracker.close();
                        return;
                    }
                    var line = new LineDrawer(player, drawer, 30);
                    Runnable cancel = () -> {
                        tracker.close();
                        line.cancel();
                        playerSet.remove(player.getUniqueId());
                    };
                    if (!tracker.animate("left_attack_1", AnimationModifier.DEFAULT, cancel) || !playerSet.add(uuid)) {
                        cancel.run();
                    }
                });
    }

    private void giveKnightSword(@NotNull Player player) {
        var sword = new ItemStack(Material.NETHERITE_SWORD);
        sword.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gradient:#FF6A00:#FFD800><b>Knight Sword"));
            meta.setUnbreakable(true);
            meta.setItemModel(new NamespacedKey(
                    BetterModel.plugin(),
                    "knight_sword"
            ));
            meta.addItemFlags(ItemFlag.values());
            meta.getPersistentDataContainer().set(KNIGHT_SWORD_KEY, PersistentDataType.BOOLEAN, true);
        });
        player.getInventory().addItem(sword);
    }

    private static @NotNull ItemStack createLine() {
        var line = new ItemStack(Material.PAPER);
        line.editMeta(meta -> meta.setItemModel(new NamespacedKey(
                BetterModel.plugin(),
                "knight_line"
        )));
        return line;
    }

    private static @NotNull Vector3f toDeltaVector(@NotNull Location before, @NotNull Location after, float yRot) {
        var rd = after.toVector().subtract(before.toVector()).rotateAroundY(yRot);
        return new Vector3f((float) rd.getX(), (float) rd.getY(), (float) rd.getZ());
    }

    private record DrawerFrame(
            float yaw,
            @NotNull Location location,
            Vector3f vector
    ) {
    }

    private class LineDrawer {
        private final List<Player> players;
        private DrawerFrame after;
        private final AtomicInteger counter = new AtomicInteger();
        private final List<BooleanSupplier> queuedTask = new ArrayList<>();
        private final ScheduledTask task;

        LineDrawer(@NotNull Player player, @NotNull RenderedBone bone, int count) {
            players = Stream.concat(
                    Stream.of(player),
                    player.getTrackedBy().stream()
            ).toList();
            task = Bukkit.getAsyncScheduler().runAtFixedRate(BetterModel.plugin(), task -> {
                queuedTask.removeIf(BooleanSupplier::getAsBoolean);
                var c = counter.incrementAndGet();
                if (c >= count) return;
                var before = after;
                after = new DrawerFrame(
                        bone.rotation().radianY(),
                        player.getLocation(),
                        bone.hitBoxPosition().rotateY(bone.rotation().radianY())
                );
                if (before == null) return;
                var delta = toDeltaVector(
                        relativeLocation(before.location, before.vector, before.yaw),
                        relativeLocation(after.location, after.vector, after.yaw),
                        (float) toRadians(after.location.getYaw())
                );
                var yaw = atan2(delta.x, delta.z);
                var pitch = atan2(-delta.y, sqrt(fma(delta.x, delta.x, delta.z * delta.z)));
                createDisplay(relativeLocation(before.location, before.vector, before.yaw), delta.length(), new Quaternionf()
                        .rotateLocalX((float) pitch)
                        .rotateLocalY((float) yaw));
            }, 50, 10, TimeUnit.MILLISECONDS);
        }

        void cancel() {
            task.cancel();
            queuedTask.removeIf(BooleanSupplier::getAsBoolean);
        }

        @NotNull Location relativeLocation(@NotNull Location location, @NotNull Vector3f vector3f, float originYaw) {
            var loc = location.clone();
            loc.setPitch(0);
            loc.add(new Vector(vector3f.x, vector3f.y, vector3f.z).rotateAroundY(-originYaw));
            return loc;
        }

        void createDisplay(@NotNull Location start, float length, @NotNull Quaternionf quaternionf) {
            if (length <= 0.1) return;
            start.getWorld().spawnParticle(
                    Particle.DUST,
                    start,
                    3,
                    0.2,
                    0.2,
                    0.2,
                    0,
                    new Particle.DustOptions(Color.YELLOW, 1)
            );
            var display = BetterModel.plugin().nms().create(start, 0, d -> {
                d.item(lineItem);
                d.brightness(15, 15);
            });
            var transformer = display.createTransformer();
            var bundler = BetterModel.plugin().nms().createBundler(2);
            display.spawn(bundler);
            transformer.transform(
                    0,
                    new Vector3f(),
                    new Vector3f(1, 1, length),
                    quaternionf,
                    bundler
            );
            players.forEach(bundler::send);
            var displayCounter = new AtomicInteger();
            queuedTask.add(() -> {
                if (displayCounter.incrementAndGet() >= 20 || task.isCancelled()) {
                    var removeBundler = BetterModel.plugin().nms().createBundler(1);
                    display.remove(removeBundler);
                    players.forEach(removeBundler::send);
                    return true;
                }
                return false;
            });
        }
    }
}

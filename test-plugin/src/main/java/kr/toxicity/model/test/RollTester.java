/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.test;

import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.TrackerModifier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RollTester implements ModelTester, Listener {

    private final Set<UUID> coolTimeSet = ConcurrentHashMap.newKeySet();
    private final Set<UUID> invulnerableSet = ConcurrentHashMap.newKeySet();

    @Override
    public void start(@NotNull BetterModelTest test) {
        Bukkit.getPluginManager().registerEvents(this, test);
        var command = test.getCommand("rollinfo");
        if (command != null) command.setExecutor((sender, command1, label, args) -> sendRollTime(sender));
    }

    @Override
    public void end(@NotNull BetterModelTest test) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void swap(@NotNull PlayerSwapHandItemsEvent event) {
        var player = event.getPlayer();
        event.setCancelled(true);
        var block = underBlock(player);
        if (block.isEmpty()) return;
        if (coolTimeSet.contains(player.getUniqueId())) return;
        var loc = player.getLocation();
        var data = block.getBlockData();
        loc.getWorld().spawnParticle(
                Particle.BLOCK,
                loc,
                20,
                1,
                0.25,
                1,
                0.2,
                data
        );
        loc.getWorld().playSound(loc, data.getSoundGroup().getBreakSound(), 0.5F, 1.0F);
        playRoll(player);
    }

    @EventHandler
    public void hit(@NotNull ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player player && invulnerableSet.contains(player.getUniqueId())) event.setCancelled(true);
    }
    @EventHandler
    public void quit(@NotNull PlayerQuitEvent event) {
        var get = event.getPlayer().getUniqueId();
        invulnerableSet.remove(get);
        coolTimeSet.remove(get);
    }
    @EventHandler
    public void death(@NotNull PlayerDeathEvent event) {
        var get = event.getPlayer().getUniqueId();
        invulnerableSet.remove(get);
        coolTimeSet.remove(get);
    }
    @EventHandler
    public void damage(@NotNull EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player && invulnerableSet.contains(player.getUniqueId())) event.setCancelled(true);
    }
    @EventHandler
    public void push(@NotNull EntityPushedByEntityAttackEvent event) {
        if (event.getEntity() instanceof Player player && invulnerableSet.contains(player.getUniqueId())) event.setCancelled(true);
    }

    private static @NotNull Block underBlock(@NotNull Player player) {
        return player.getLocation().add(0, -1, 0).getBlock();
    }

    private static boolean sendRollTime(@NotNull Audience audience) {
        return BetterModel.limb("steve")
                .flatMap(r -> r.animation("roll"))
                .map(animation -> {
                    audience.sendMessage(Component.text()
                            .append(Component.text("Loop mode: " + animation.loop()))
                            .appendNewline()
                            .append(Component.text("Length: " + animation.length() + " second")));
                    return audience;
                })
                .isPresent();
    }

    private void playRoll(@NotNull Player player) {
        var input = inputToYaw(player);
        BetterModel.limb("steve")
                .map(r -> r.getOrCreate(player, TrackerModifier.DEFAULT, t -> t.rotation(() -> new ModelRotation(player.getPitch(), packDegree(input + t.registry().entity().bodyYaw())))))
                .ifPresent(t -> {
                    if (t.animate(b -> true, "roll", AnimationModifier.DEFAULT_WITH_PLAY_ONCE, () -> {
                        BetterModel.plugin().scheduler().asyncTaskLater(3, () -> coolTimeSet.remove(player.getUniqueId()));
                        t.close();
                    })) {
                        if (coolTimeSet.add(player.getUniqueId()) && invulnerableSet.add(player.getUniqueId())) {
                            player.addPotionEffect(new PotionEffect(
                                    PotionEffectType.LUCK,
                                    8,
                                    5,
                                    true,
                                    false
                            ));
                            BetterModel.plugin().scheduler().asyncTaskLater(8, () -> invulnerableSet.remove(player.getUniqueId()));
                            player.setVelocity(player.getVelocity()
                                    .add(new Vector(0, 0, 0.75).rotateAroundY(-Math.toRadians(input + t.registry().entity().bodyYaw())))
                                    .setY(0.15));
                        }
                    } else t.close();
                });
    }

    private static float inputToYaw(@NotNull Player player) {
        var input = player.getCurrentInput();
        var leftRightDegree = switch (TriState.of(input.isLeft(), input.isRight())) {
            case LEFT -> 270F;
            case RIGHT -> 90F;
            case NOT_FOUND -> -1F;
        };
        var forwardBackwardDegree = switch (TriState.of(input.isForward(), input.isBackward())) {
            case LEFT -> 0F;
            case RIGHT -> 180F;
            case NOT_FOUND -> -1F;
        };
        if (forwardBackwardDegree < 0) return Math.max(leftRightDegree, 0);
        else if (leftRightDegree < 0) return forwardBackwardDegree;
        if (leftRightDegree - forwardBackwardDegree > 180) forwardBackwardDegree += 360;
        return (forwardBackwardDegree + leftRightDegree) / 2;
    }

    private static float packDegree(float degree) {
        return degree > 180 ? degree - 360 : degree;
    }


    private enum TriState {
        LEFT,
        RIGHT,
        NOT_FOUND
        ;
        static @NotNull TriState of(boolean left, boolean right) {
            return left ? LEFT : right ? RIGHT : NOT_FOUND;
        }
    }
}

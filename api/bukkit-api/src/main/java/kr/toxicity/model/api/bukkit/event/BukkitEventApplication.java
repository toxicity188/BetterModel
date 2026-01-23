/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.api.bukkit.event;

import kr.toxicity.model.api.event.ModelEventApplication;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

/**
 * An implementation of {@link ModelEventApplication} for Bukkit plugins.
 * <p>
 * This record holds a weak reference to a Bukkit plugin to prevent memory leaks
 * and checks if the plugin is enabled.
 * </p>
 *
 * @param name the name of the plugin
 * @param pluginRef a weak reference to the plugin instance
 * @since 2.0.0
 */
public record BukkitEventApplication(@NotNull String name, @NotNull WeakReference<Plugin> pluginRef) implements ModelEventApplication {

    /**
     * Creates a new BukkitEventApplication for the given plugin.
     *
     * @param plugin the Bukkit plugin
     * @return the event application wrapper
     * @since 2.0.0
     */
    public static @NotNull BukkitEventApplication of(@NotNull Plugin plugin) {
        return new BukkitEventApplication(plugin.getName(), new WeakReference<>(plugin));
    }

    @Override
    public boolean isEnabled() {
        var get = pluginRef().get();
        return get != null && get.isEnabled();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BukkitEventApplication that)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

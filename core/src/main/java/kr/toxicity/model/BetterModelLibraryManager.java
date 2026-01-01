/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model;

import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.classloader.URLClassLoaderHelper;
import net.byteflux.libby.logging.adapters.JDKLogAdapter;
import org.bukkit.plugin.Plugin;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;

final class BetterModelLibraryManager extends LibraryManager {

    private final URLClassLoaderHelper classLoader;

    BetterModelLibraryManager(Plugin plugin) {
        super(new JDKLogAdapter(Objects.requireNonNull(plugin, "plugin").getLogger()), plugin.getDataFolder().toPath(), ".libs");
        var pluginLoader = plugin.getClass().getClassLoader();
        URLClassLoader loader;
        try {
            var field = pluginLoader.getClass().getDeclaredField("libraryLoader");
            field.setAccessible(true);
            loader = (URLClassLoader) field.get(pluginLoader);
        } catch (Exception ignored) {
            loader = (URLClassLoader) pluginLoader;
        }
        this.classLoader = new URLClassLoaderHelper(loader, this);
    }

    protected void addToClasspath(Path file) {
        this.classLoader.addToClasspath(file);
    }
}

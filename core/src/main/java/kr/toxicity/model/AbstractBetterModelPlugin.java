/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelLogger;
import kr.toxicity.model.api.BetterModelPlugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public abstract class AbstractBetterModelPlugin extends JavaPlugin implements BetterModelPlugin {

    protected boolean skipInitialReload;
    protected final AtomicBoolean onReload = new AtomicBoolean();
    protected final AtomicBoolean firstLoad = new AtomicBoolean();
    protected final BetterModelLogger logger = new BetterModelLogger() {

        private final Logger internalLogger = getLogger();

        @Override
        public void info(@NotNull String... message) {
            synchronized (internalLogger) {
                for (String s : message) {
                    internalLogger.info(s);
                }
            }
        }

        @Override
        public void warn(@NotNull String... message) {
            synchronized (internalLogger) {
                for (String s : message) {
                    internalLogger.warning(s);
                }
            }
        }
    };
    private @Nullable Attributes attributes;

    @Override
    public void onLoad() {
        new BetterModelLibrary().load(this);
        BetterModel.register(this);
    }

    public void skipInitialReload() {
        this.skipInitialReload = true;
    }

    public @NotNull Attributes attributes() {
        if (attributes != null) return attributes;
        synchronized (this) {
            if (attributes != null) return attributes;
            try (
                    var jar = new JarFile(getFile());
                    var stream = jar.getInputStream(new JarEntry("META-INF/MANIFEST.MF"))
            ) {
                return attributes = new Manifest(stream).getMainAttributes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

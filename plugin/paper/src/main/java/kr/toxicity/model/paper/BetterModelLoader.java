/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class BetterModelLoader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        var lib = new MavenLibraryResolver();
        lib.addRepository(new RemoteRepository.Builder(
                null,
                "default",
                "https://maven-central.storage-download.googleapis.com/maven2"
        ).build());
        try (
                var stream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("paper-library"));
                var streamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                var reader = new BufferedReader(streamReader)
        ) {
            String next;
            while ((next = reader.readLine()) != null) {
                lib.addDependency(new Dependency(new DefaultArtifact(next), null));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        classpathBuilder.addLibrary(lib);
    }
}
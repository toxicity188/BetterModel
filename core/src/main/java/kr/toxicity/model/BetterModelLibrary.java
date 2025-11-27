/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2025 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.util.function.BooleanConstantSupplier;
import net.byteflux.libby.Library;
import net.byteflux.libby.relocation.Relocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public final class BetterModelLibrary {

    private static final String KOTLIN_RELOCATED = "_kotlin".substring(1);
    private static final List<LibraryData> LIBRARY_DATA = new ArrayList<>();

    public static final LibraryData KOTLIN = register(
            "org{}jetbrains{}kotlin",
            KOTLIN_RELOCATED + "-stdlib",
            builder -> builder.relocation(KOTLIN_RELOCATED)
    );
    public static final LibraryData BSTATS = register(
            "org{}bstats",
            "bstats-bukkit",
            builder -> builder.relocation("org{}bstats")
                    .subModules(
                            "bstats-base"
                    )
    );
//    public static final LibraryData CLOUD = register( TODO add this when cloud-paper 2.0.0-beta.14 is released
//            "org{}incendo",
//            "cloud-paper",
//            builder -> builder
//                    .subModules(
//                            "cloud-brigadier",
//                            "cloud-bukkit"
//                    )
//                    .relocation("org{}incendo{}cloud")
//    );
//    public static final LibraryData CLOUD_CORE = register(
//            "org{}incendo",
//            "cloud-core",
//            builder -> builder
//                    .subModules(
//                            "cloud-services"
//                    )
//                    .relocation("org{}incendo{}cloud")
//    );
    public static final LibraryData GEANTYREF = register(
            "io{}leangen{}geantyref",
            "geantyref",
            builder -> builder.predicate(BooleanConstantSupplier.of(!BetterModel.IS_PAPER))
    );
    public static final LibraryData MOLANG_COMPILER = register(
            "gg{}moonflower",
            "molang-compiler",
            builder -> builder
    );
    public static final LibraryData ADVENTURE_API = register(
            "net{}kyori",
            "adventure-api",
            builder -> builder
                    .subModules(
                            "adventure-key",
                            "adventure-text-logger-slf4j",
                            "adventure-text-serializer-legacy",
                            "adventure-nbt",
                            "adventure-text-serializer-gson",
                            "adventure-text-serializer-gson-legacy-impl",
                            "adventure-text-serializer-json",
                            "adventure-text-serializer-json-legacy-impl"
                    )
                    .predicate(BooleanConstantSupplier.of(!BetterModel.IS_PAPER))
    );
    public static final LibraryData EXAMINATION_API = register(
            "net{}kyori",
            "examination-api",
            builder -> builder
                    .subModules(
                            "examination-string"
                    )
                    .predicate(BooleanConstantSupplier.of(!BetterModel.IS_PAPER))
    );
    public static final LibraryData OPTION = register(
            "net{}kyori",
            "option",
            builder -> builder.predicate(BooleanConstantSupplier.of(!BetterModel.IS_PAPER))
    );
    public static final LibraryData ADVENTURE_PLATFORM = register(
            "net{}kyori",
            "adventure-platform-bukkit",
            builder -> builder
                    .subModules(
                            "adventure-platform-api",
                            "adventure-platform-facet",
                            "adventure-platform-viaversion",
                            "adventure-text-serializer-bungeecord"
                    )
                    .predicate(BooleanConstantSupplier.of(!BetterModel.IS_PAPER))
    );

    public void load(@NotNull AbstractBetterModelPlugin plugin) {
        var manager = new BetterModelLibraryManager(plugin);
        manager.addRepository("https://maven-central.storage-download.googleapis.com/maven2/");
        manager.addRepository("https://maven.blamejared.com/");
        manager.addMavenCentral();
        LIBRARY_DATA.stream()
                .filter(LibraryData::isLoaded)
                .flatMap(library -> library.toLibby(plugin))
                .forEach(manager::loadLibrary);
    }

    private static @NotNull LibraryData register(@NotNull String group, @NotNull String artifact, @NotNull Function<LibraryData.Builder, LibraryData.Builder> function) {
        var build = function.apply(new LibraryData.Builder(group, artifact)).build();
        LIBRARY_DATA.add(build);
        return build;
    }

    public record LibraryData(
            @NotNull String group,
            @NotNull String artifact,
            @Nullable String relocation,
            @NotNull String versionRef,
            @NotNull @Unmodifiable Set<String> subModules,
            @NotNull BooleanSupplier predicate
    ) {

        private static class Builder {
            final String group;
            final String artifact;
            @Nullable String relocation;
            @NotNull String versionRef;
            @NotNull @Unmodifiable Set<String> subModules = Collections.emptySet();
            @NotNull BooleanSupplier predicate = BooleanConstantSupplier.TRUE;

            Builder(@NotNull String group, @NotNull String artifact) {
                this.group = group;
                this.artifact = this.versionRef = artifact;
            }

            @NotNull Builder predicate(@NotNull BooleanSupplier predicate) {
                this.predicate = Objects.requireNonNull(predicate);
                return this;
            }

            @NotNull Builder subModules(@NotNull String... subModules) {
                this.subModules = Set.of(Objects.requireNonNull(subModules));
                return this;
            }

            @NotNull Builder relocation(@Nullable String relocation) {
                this.relocation = Objects.requireNonNull(relocation);
                return this;
            }

            @NotNull Builder versionRef(@NotNull String versionRef) {
                this.versionRef = Objects.requireNonNull(versionRef);
                return this;
            }

            @NotNull LibraryData build() {
                return new LibraryData(
                        group,
                        artifact,
                        relocation,
                        versionRef,
                        subModules,
                        predicate
                );
            }
        }

        public boolean isLoaded() {
            return predicate.getAsBoolean();
        }

        private @NotNull Stream<Library> toLibby(@NotNull AbstractBetterModelPlugin plugin) {
            var version = plugin.attributes().getValue("library-" + versionRef);
            return Stream.concat(
                    Stream.of(artifact),
                    subModules.stream()
            ).map(name -> {
                var libs = Library.builder()
                        .groupId(group)
                        .artifactId(name)
                        .version(version);
                if (relocation != null) libs.relocate(new Relocation(relocation, "kr{}toxicity{}model{}shaded{}" + relocation));
                return libs.build();
            });
        }
    }
}

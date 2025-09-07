package kr.toxicity.model;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.util.function.BooleanConstantSupplier;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.byteflux.libby.relocation.Relocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public final class BetterModelLibrary {

    private static final String KOTLIN_RELOCATED = "_kotlin".substring(1);

    public static final LibraryData KOTLIN = new LibraryData(
            "org{}jetbrains{}kotlin",
            KOTLIN_RELOCATED + "-stdlib",
            KOTLIN_RELOCATED
    );
    public static final LibraryData BSTATS = new LibraryData(
            "org{}bstats",
            "bstats-bukkit",
            "org{}bstats",
            Set.of(
                    "bstats-base"
            )
    );
    public static final LibraryData MOLANG_COMPILER = new LibraryData(
            "gg{}moonflower",
            "molang-compiler",
            null
    );
    public static final LibraryData ADVENTURE_API = new LibraryData(
            "net{}kyori",
            "adventure-api",
            null,
            Set.of(
                    "adventure-key",
                    "adventure-text-serializer-legacy",
                    "adventure-nbt",
                    "adventure-text-serializer-gson",
                    "adventure-text-serializer-gson-legacy-impl",
                    "adventure-text-serializer-json",
                    "adventure-text-serializer-json-legacy-impl"
            ),
            BooleanConstantSupplier.of(!BetterModel.IS_PAPER)
    );
    public static final LibraryData EXAMINATION_API = new LibraryData(
            "net{}kyori",
            "examination-api",
            null,
            Set.of(
                    "examination-string"
            ),
            BooleanConstantSupplier.of(!BetterModel.IS_PAPER)
    );
    public static final LibraryData OPTION = new LibraryData(
            "net{}kyori",
            "option",
            null,
            BooleanConstantSupplier.of(!BetterModel.IS_PAPER)
    );
    public static final LibraryData ADVENTURE_PLATFORM = new LibraryData(
            "net{}kyori",
            "adventure-platform-bukkit",
            null,
            Set.of(
                    "adventure-platform-api",
                    "adventure-platform-facet",
                    "adventure-platform-viaversion",
                    "adventure-text-serializer-bungeecord"
            ),
            BooleanConstantSupplier.of(!BetterModel.IS_PAPER)
    );

    private static final List<LibraryData> LIBRARY_DATA = List.of(
            KOTLIN,
            BSTATS,
            MOLANG_COMPILER,
            ADVENTURE_API,
            EXAMINATION_API,
            OPTION,
            ADVENTURE_PLATFORM
    );

    public void load(@NotNull AbstractBetterModelPlugin plugin) {
        var manager = new BukkitLibraryManager(plugin, ".libs");
        manager.addRepository("https://maven-central.storage-download.googleapis.com/maven2/");
        manager.addRepository("https://maven.blamejared.com/");
        manager.addMavenCentral();
        LIBRARY_DATA.stream()
                .filter(LibraryData::isLoaded)
                .flatMap(library -> library.toLibby(plugin))
                .forEach(manager::loadLibrary);
    }

    public record LibraryData(
            @NotNull String group,
            @NotNull String artifact,
            @Nullable String relocation,
            @NotNull @Unmodifiable Set<String> subModules,
            @NotNull BooleanSupplier predicate
    ) {
        public LibraryData(
                @NotNull String group,
                @NotNull String artifact,
                @Nullable String relocation
        ) {
            this(group, artifact, relocation, Collections.emptySet(), BooleanConstantSupplier.TRUE);
        }
        public LibraryData(
                @NotNull String group,
                @NotNull String artifact,
                @Nullable String relocation,
                @NotNull BooleanSupplier predicate
        ) {
            this(group, artifact, relocation, Collections.emptySet(), predicate);
        }
        public LibraryData(
                @NotNull String group,
                @NotNull String artifact,
                @Nullable String relocation,
                @NotNull @Unmodifiable Set<String> subModules
        ) {
            this(group, artifact, relocation, subModules, BooleanConstantSupplier.TRUE);
        }

        public boolean isLoaded() {
            return predicate.getAsBoolean();
        }

        private @NotNull Stream<Library> toLibby(@NotNull AbstractBetterModelPlugin plugin) {
            var version = plugin.attributes().getValue("library-" + artifact);
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

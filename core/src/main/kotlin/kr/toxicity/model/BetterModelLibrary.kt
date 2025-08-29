package kr.toxicity.model

import kr.toxicity.model.api.BetterModel
import net.byteflux.libby.BukkitLibraryManager
import net.byteflux.libby.Library

object BetterModelLibrary {

    val molangCompiler = LibraryData(
        "gg{}moonflower",
        "molang-compiler"
    )
    val dynamicUV = LibraryData(
        "io{}github{}toxicity188",
        "dynamicuv"
    )

    val adventureApi = LibraryData(
        "net{}kyori",
        "adventure-api",
        setOf(
            "adventure-key",
            "adventure-text-serializer-legacy",
            "adventure-nbt",
            "adventure-text-serializer-gson",
            "adventure-text-serializer-gson-legacy-impl",
            "adventure-text-serializer-json",
            "adventure-text-serializer-json-legacy-impl"
        )
    ) {
        !BetterModel.IS_PAPER
    }
    val examinationApi = LibraryData(
        "net{}kyori",
        "examination-api",
        setOf(
            "examination-string"
        )
    ) {
        !BetterModel.IS_PAPER
    }
    val option = LibraryData(
        "net{}kyori",
        "option",
    ) {
        !BetterModel.IS_PAPER
    }
    val adventurePlatform = LibraryData(
        "net{}kyori",
        "adventure-platform-bukkit",
        setOf(
            "adventure-platform-api",
            "adventure-platform-facet",
            "adventure-platform-viaversion",
            "adventure-text-serializer-bungeecord",
        )
    ) {
        !BetterModel.IS_PAPER
    }

    private val libraries = listOf(
        molangCompiler,
        dynamicUV,
        adventureApi,
        examinationApi,
        option,
        adventurePlatform
    )

    internal fun load(plugin: BetterModelPluginImpl) {
        BukkitLibraryManager(plugin, ".libs").apply {
            addMavenCentral()
            addRepository("https://maven.blamejared.com/")
        }.run {
            libraries
                .asSequence()
                .filter { it.isLoaded }
                .flatMap { it.toLibby(plugin) }
                .forEach { loadLibrary(it) }
        }
    }

    data class LibraryData(
        val group: String,
        val artifact: String,
        val subModules: Set<String> = emptySet(),
        private val predicate: () -> Boolean = { true }
    ) {
        val isLoaded get() = predicate()
        fun toLibby(plugin: BetterModelPluginImpl): List<Library> = (subModules + artifact).map {
            Library.builder()
                .groupId(group)
                .artifactId(it)
                .version(plugin.attributes.getValue("library-$artifact"))
                .build()
        }
    }
}
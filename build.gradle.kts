import io.papermc.hangarpublishplugin.model.Platforms
import xyz.jpenilla.resourcefactory.bukkit.Permission
import java.time.LocalDateTime

plugins {
    `java-library`
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.dokka") version "2.0.0"
    id("io.github.goooler.shadow") version "8.1.8"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.16" apply false
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.2.0"
    id("com.modrinth.minotaur") version "2.+"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
}

val minecraft = "1.21.4"
val targetJavaVersion = 21
val buildNumber: String? = System.getenv("BUILD_NUMBER")

allprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")
    group = "kr.toxicity.model"
    version = "1.4.3" + (buildNumber?.let { "-SNAPSHOT-$it" } ?: "")
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.citizensnpcs.co/repo/")
        maven("https://mvn.lumine.io/repository/maven-public/")
    }
    dependencies {
        testImplementation(kotlin("test"))
        implementation("dev.jorel:commandapi-bukkit-shade:9.7.0")
        implementation("org.bstats:bstats-bukkit:3.1.0")
        compileOnly("net.citizensnpcs:citizens-main:2.0.37-SNAPSHOT")
        compileOnly("io.lumine:Mythic-Dist:5.8.1")
    }
    tasks {
        test {
            useJUnitPlatform()
        }
        compileJava {
            options.encoding = Charsets.UTF_8.name()
        }
    }
    java {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
    kotlin {
        jvmToolchain(targetJavaVersion)
    }
    dokka {
        moduleName = project.name
        dokkaSourceSets.configureEach {
            displayName = project.name
        }
    }
}


fun Project.dependency(any: Any) = also { project ->
    if (any is Collection<*>) {
        any.forEach {
            if (it == null) return@forEach
            project.dependencies {
                compileOnly(it)
                testImplementation(it)
            }
        }
    } else {
        project.dependencies {
            compileOnly(any)
            testImplementation(any)
        }
    }
}

fun Project.paper() = dependency("io.papermc.paper:paper-api:$minecraft-R0.1-SNAPSHOT")

val api = project("api").paper()
val nms = project("nms").subprojects.map {
    it.dependency(api)
        .also { project ->
            project.apply(plugin = "io.papermc.paperweight.userdev")
        }
}
val core = project("core")
    .paper()
    .dependency(api)
    .dependency(nms)

dependencies {
    implementation(api)
    implementation(core)
    nms.forEach {
        implementation(project("nms:${it.name}", configuration = "reobf"))
    }
    fun searchAll(target: Project) {
        val sub = target.subprojects
        if (sub.isNotEmpty()) sub.forEach {
            searchAll(it)
        } else dokka(target)
    }
    searchAll(rootProject)
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(tasks.classes)
    fun getProjectSource(project: Project): Array<File> {
        return if (project.subprojects.isEmpty()) project.sourceSets.main.get().allSource.srcDirs.toTypedArray() else ArrayList<File>().apply {
            project.subprojects.forEach {
                addAll(getProjectSource(it))
            }
        }.toTypedArray()
    }
    archiveClassifier = "sources"
    from(*getProjectSource(project))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGenerate)
    archiveClassifier = "javadoc"
    from(layout.buildDirectory.dir("dokka/html").orNull?.asFile)
}

val groupString = group.toString()

tasks {
    runServer {
        pluginJars(fileTree("plugins"))
        version(minecraft)
        downloadPlugins {
            hangar("ViaVersion", "5.2.1")
            hangar("ViaBackwards", "5.2.1")
            hangar("Skript", "2.10.2")
        }
    }
    jar {
        finalizedBy(shadowJar)
    }
    shadowJar {
        manifest {
            attributes(
                "paperweight-mappings-namespace" to "spigot",
                "Dev-Build" to (buildNumber != null),
                "Version" to version,
                "Author" to "toxicity188",
                "Url" to "https://github.com/toxicity188/BetterModel",
                "Created-By" to "Gradle $gradle",
                "Build-Jdk" to "${System.getProperty("java.vendor")} ${System.getProperty("java.version")}",
                "Build-OS" to "${System.getProperty("os.arch")} ${System.getProperty("os.name")}",
                "Build-Date" to LocalDateTime.now().toString()
            )
        }
        archiveClassifier = ""
        dependencies {
            exclude(dependency("org.jetbrains:annotations:13.0"))
        }
        fun prefix(pattern: String) {
            relocate(pattern, "$groupString.shaded.$pattern")
        }
        exclude("LICENSE")
        prefix("kotlin")
        prefix("dev.jorel.commandapi")
        prefix("org.bstats")
    }
    build {
        finalizedBy(
            sourcesJar,
            javadocJar
        )
    }
}

tasks.modrinth {
    dependsOn(tasks.modrinthSyncBody)
}

bukkitPluginYaml {
    main = "${project.group}.BetterModelPluginImpl"
    version = project.version.toString()
    name = rootProject.name
    foliaSupported = true
    apiVersion = "1.19"
    author = "toxicity"
    description = "Modern lightweight Minecraft model implementation for Paper, Folia"
    softDepend = listOf(
        "MythicMobs",
        "Citizens"
    )
    permissions.create("bettermodel") {
        default = Permission.Default.OP
        description = "Accesses to command."
        children = mapOf(
            "reload" to true,
            "spawn" to true,
            "disguise" to true,
            "undisguise" to true,
            "play" to true,
            "limb" to true
        )
    }
}

val supportedVersion = listOf(
    "1.19.4",
    "1.20",
    "1.20.1",
    "1.20.2",
    "1.20.3",
    "1.20.4",
    "1.20.5",
    "1.20.6",
    "1.21",
    "1.21.1",
    "1.21.2",
    "1.21.3",
    "1.21.4",
    //"1.21.5", TODO - Add this line when 1.21.5 paper is out and can be compatible
)

hangarPublish {
    publications.register("plugin") {
        version = project.version as String
        id = "BetterModel"
        apiKey = System.getenv("HANGAR_API_TOKEN")
        val log = System.getenv("COMMIT_MESSAGE")
        if (log != null) {
            changelog = log
            channel = "Snapshot"
        } else {
            changelog = rootProject.file("changelog/${project.version}.md").readText()
            channel = "Release"
        }
        platforms {
            register(Platforms.PAPER) {
                jar = tasks.shadowJar.flatMap { it.archiveFile }
                platformVersions = supportedVersion
            }
        }
    }
}

modrinth {
    token = System.getenv("MODRINTH_API_TOKEN")
    projectId = "bettermodel"
    syncBodyFrom = rootProject.file("README.md").readText()
    val log = System.getenv("COMMIT_MESSAGE")
    if (log != null) {
        versionType = "beta"
        changelog = log
    } else {
        versionType = "release"
        changelog = rootProject.file("changelog/${project.version}.md").readText()
    }
    uploadFile = tasks.shadowJar.get()
    additionalFiles = listOf(
        sourcesJar.get(),
        javadocJar.get()
    )
    versionName = "BetterModel ${project.version}"
    versionNumber = project.version as String
    gameVersions = supportedVersion
    loaders = listOf("bukkit", "spigot", "paper", "purpur", "folia")
}
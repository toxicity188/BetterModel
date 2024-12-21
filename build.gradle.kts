plugins {
    `java-library`
    kotlin("jvm") version "2.1.0"
    id("io.github.goooler.shadow") version "8.1.8"
    id("io.papermc.paperweight.userdev") version "1.7.6" apply false
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version("1.2.0")
}

val minecraft = "1.21.4"
val targetJavaVersion = 21

allprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    group = "kr.toxicity.model"
    version = "1.0"
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
    dependencies {
        testImplementation(kotlin("test"))
        implementation("dev.jorel:commandapi-bukkit-shade:9.7.0")
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
}

tasks {
    runServer {
        version(minecraft)
    }
    jar {
        finalizedBy(shadowJar)
    }
    shadowJar {
        manifest {
            attributes["paperweight-mappings-namespace"] = "spigot"
        }
        archiveClassifier = ""
        dependencies {
            exclude(dependency("org.jetbrains:annotations:13.0"))
        }
        fun prefix(pattern: String) {
            relocate(pattern, "${project.group}.shaded.$pattern")
        }
        prefix("kotlin")
        prefix("dev.jorel.commandapi")
    }
}

bukkitPluginYaml {
    main = "${project.group}.ModelRendererImpl"
    version = project.version.toString()
    name = rootProject.name
    apiVersion = "1.19"
    author = "toxicity"
    description = "Simple Model API."
}
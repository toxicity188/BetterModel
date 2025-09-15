plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.github.hierynomus.license")
}

group = "kr.toxicity.model"
version = property("plugin_version").toString() + (BUILD_NUMBER?.let { "-SNAPSHOT-$it" } ?: "")

val shade = configurations.create("shade")

configurations.implementation {
    extendsFrom(shade)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://libraries.minecraft.net/")
    maven("https://jitpack.io")
    maven("https://repo.alessiodp.com/releases/")
    maven("https://maven.blamejared.com/")
}

dependencies {
    testImplementation(kotlin("test"))

    compileOnly(libs.bundles.library)
    testImplementation(libs.bundles.library)
    compileOnly(libs.bundles.manifestLibrary)
    testImplementation(libs.bundles.manifestLibrary)
}

tasks {
    test {
        useJUnitPlatform()
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

license {
    header = rootProject.file("LICENSE_HEADER")
    includes(setOf(
        "**/*.java",
        "**/*.kt"
    ))
    strictCheck = true
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(JAVA_VERSION)
}

kotlin {
    jvmToolchain(JAVA_VERSION)
}

dokka {
    moduleName = project.name
    dokkaSourceSets.configureEach {
        displayName = project.name
    }
}
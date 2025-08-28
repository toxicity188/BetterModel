plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

group = "kr.toxicity.model"
version = property("plugin_version").toString() + (BUILD_NUMBER?.let { "-SNAPSHOT-$it" } ?: "")

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
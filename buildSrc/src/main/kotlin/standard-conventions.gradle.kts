plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

group = "kr.toxicity.model"
version = "1.6.2" + (BUILD_NUMBER?.let { "-SNAPSHOT-$it" } ?: "")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.citizensnpcs.co/repo/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.hibiscusmc.com/releases")
    maven("https://libraries.minecraft.net/")
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))

    compileOnly(libs.bundles.library)
    testImplementation(libs.bundles.library)
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
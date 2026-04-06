plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("dev.yumi.gradle.licenser")
}

group = "kr.toxicity.model"
version = property("project_version").toString() + (BUILD_NUMBER?.let { "-SNAPSHOT-$it" } ?: "")

val shade = configurations.create("shade")

configurations.implementation {
    extendsFrom(shade)
}

rootProject.dependencies.dokka(project)

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

license {
    rule(rootProject.file("LICENSE_HEADER"))
    include("**/*.java", "**/*.kt")
    exclude("**/*.properties")
}

java {
    disableAutoTargetJvm()
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

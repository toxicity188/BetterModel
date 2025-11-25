plugins {
    id("standard-conventions")
}

repositories {
    maven("https://maven-prs.papermc.io/Paper/pr13194") {
        name = "Maven for PR #13194" // https://github.com/PaperMC/Paper/pull/13194
        mavenContent {
            includeModule("io.papermc.paper", "dev-bundle")
            includeModule("io.papermc.paper", "paper-api")
        }
    }
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(libs.bundles.shadedLibrary)
}
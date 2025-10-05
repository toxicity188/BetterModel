plugins {
    alias(libs.plugins.convention.bukkit)
    alias(libs.plugins.paperweight) apply false
}

repositories {
    maven("https://maven.citizensnpcs.co/repo/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.nexomc.com/releases/")
}

dependencies {
    shade(project(":api"))
    shade(project(":purpur"))
    rootProject.project("nms").subprojects.forEach {
        compileOnly(it)
    }
    rootProject.project("authlib").subprojects.forEach {
        shade(it)
    }
    shade(libs.bundles.shadedLibrary) {
        exclude("net.kyori")
        exclude("org.ow2.asm")
    }
    compileOnly(libs.commandapi.bukkit)

    compileOnly(libs.bundles.manifestLibrary)
    testImplementation(libs.bundles.manifestLibrary)

    compileOnly("net.citizensnpcs:citizens-main:2.0.40-SNAPSHOT") {
        exclude("net.byteflux")
    }
    compileOnly("net.skinsrestorer:skinsrestorer-api:15.8.1")
    compileOnly("io.lumine:Mythic-Dist:5.10.1")
    compileOnly("com.nexomc:nexo:1.12.1-dev")
}
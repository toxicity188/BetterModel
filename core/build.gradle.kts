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
    shade(libs.bundles.shadedLibrary) {
        exclude("net.kyori")
        exclude("org.ow2.asm")
        exclude("io.leangen.geantyref")
        exclude("org.incendo", "cloud-core")
    }

    compileOnly(libs.bundles.manifestLibrary)
    testImplementation(libs.bundles.manifestLibrary)

    compileOnly("net.citizensnpcs:citizens-main:2.0.40-SNAPSHOT") {
        exclude("net.byteflux")
    }
    compileOnly("net.skinsrestorer:skinsrestorer-api:15.9.0")
    compileOnly("io.lumine:Mythic-Dist:5.10.1")
    compileOnly("com.nexomc:nexo:1.15.0")
}

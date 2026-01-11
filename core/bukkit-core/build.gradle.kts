plugins {
    alias(libs.plugins.convention.bukkit)
}

dependencies {
    shade(project(":api"))
    shade(project(":api:bukkit-api"))
    shade(project(":core"))

    shade(project(":purpur"))
    rootProject.project("nms").subprojects.forEach {
        compileOnly(it)
    }

    shade(libs.bundles.shadedLibrary) {
        exclude("net.kyori")
        exclude("org.ow2.asm")
        exclude("io.leangen.geantyref")
    }

    compileOnly(libs.bundles.manifestLibrary)

    compileOnly("net.citizensnpcs:citizens-main:2.0.41-SNAPSHOT") {
        exclude("net.byteflux")
    }
    compileOnly("net.skinsrestorer:skinsrestorer-api:15.9.3")
    compileOnly("io.lumine:Mythic-Dist:5.11.1")
    compileOnly("com.nexomc:nexo:1.17.0")
}

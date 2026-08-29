plugins {
    alias(libs.plugins.convention.bukkit)
}

dependencies {
    shade(project(":bettermodel-api")) { isTransitive = false }
    shade(project(":bettermodel-api:bettermodel-bukkit-api")) { isTransitive = false }
    shade(project(":bettermodel-core")) { isTransitive = false }

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

    compileOnly("net.citizensnpcs:citizens-main:2.0.43-SNAPSHOT") {
        exclude("net.byteflux")
    }
    compileOnly("net.skinsrestorer:skinsrestorer-api:15.12.5")
    compileOnly("io.lumine:Mythic-Dist:5.13.0")
    compileOnly("com.nexomc:nexo:1.27.0")
}

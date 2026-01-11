plugins {
    alias(libs.plugins.convention.standard)
}

dependencies {
    compileOnly(libs.bundles.minecraft)
    compileOnly("com.mojang:authlib:7.0.61")

    compileOnly(project(":api"))

    compileOnly(libs.bundles.shadedLibrary) {
        exclude("net.kyori")
        exclude("org.ow2.asm")
        exclude("io.leangen.geantyref")
    }

    compileOnly(libs.bundles.manifestLibrary)
}

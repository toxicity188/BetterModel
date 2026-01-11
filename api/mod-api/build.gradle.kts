plugins {
    alias(libs.plugins.convention.mod)
    id("net.neoforged.moddev")
}

dependencies {
    compileOnly(project(":api"))
}

neoForge {
    enable {
        neoFormVersion = libs.versions.neoform.get()
    }
}

plugins {
    alias(libs.plugins.convention.mod)
    id("net.neoforged.moddev")
}

dependencies {
    shade(project(":api"))
    shade(project(":api:mod-api"))
    shade(project(":core"))
}

neoForge {
    enable {
        neoFormVersion = libs.versions.neoform.get()
    }
}

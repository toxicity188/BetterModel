plugins {
    alias(libs.plugins.convention.publish)
    id("net.neoforged.moddev")
}

dependencies {
    api(project(":bettermodel-api"))
}

neoForge {
    enable {
        neoFormVersion = libs.versions.neoform.get()
    }
}

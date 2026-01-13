plugins {
    alias(libs.plugins.convention.mod)
    id("net.neoforged.moddev")
}

dependencies {
    compileOnly(project(":api"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

neoForge {
    enable {
        neoFormVersion = libs.versions.neoform.get()
    }
}

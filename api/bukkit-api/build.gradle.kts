plugins {
    alias(libs.plugins.convention.bukkit)
}

dependencies {
    compileOnly(project(":api"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

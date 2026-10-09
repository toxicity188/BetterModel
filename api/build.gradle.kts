plugins {
    alias(libs.plugins.convention.publish)
}

dependencies {
    compileOnly(libs.bundles.minecraft)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.library)
    testImplementation(libs.bundles.minecraft)
}

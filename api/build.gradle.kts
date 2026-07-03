plugins {
    alias(libs.plugins.convention.publish)
}

dependencies {
    compileOnly(libs.bundles.minecraft)
    compileOnly(libs.concurrentutil)
}

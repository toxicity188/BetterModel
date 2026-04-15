plugins {
    alias(libs.plugins.convention.publish)
    alias(libs.plugins.convention.paperweight)
}

dependencies {
    paperweight.paperDevBundle("26.1.1.build.+")
}

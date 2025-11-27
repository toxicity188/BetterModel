plugins {
    id("standard-conventions")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(libs.bundles.shadedLibrary)
}
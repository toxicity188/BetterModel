plugins {
    id("standard-conventions")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":api:bukkit-api"))
}

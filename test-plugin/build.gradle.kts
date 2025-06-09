plugins {
    alias(libs.plugins.bukkitConvention)
    alias(libs.plugins.resourceFactoryBukkit)
}

dependencies {
    compileOnly(project(":api"))
}

val pluginName = "${rootProject.name}-TestPlugin"

tasks.jar {
    archiveBaseName = pluginName
}

bukkitPluginYaml {
    main = "$group.test.BetterModelTest"
    version = project.version.toString()
    name = pluginName
    foliaSupported = true
    apiVersion = "1.20"
    author = "toxicity"
    description = "BetterModel's test plugin"
    depend = listOf(
        "BetterModel"
    )
}
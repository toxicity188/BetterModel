plugins {
    id("standard-conventions")
    id("xyz.jpenilla.resource-factory-fabric-convention")
}

tasks.jar {
    from(rootProject.layout.projectDirectory.file("LICENSE.md"))
    from(rootProject.layout.projectDirectory.file(".idea/icon.png")) {
        rename { "assets/icon.png" }
    }
}

fabricModJson {
    authors = listOf(
        person("toxicity188")
    )
    contributors = listOf(
        person("Kouvali (Fabric Port)")
    )
    contact {
        sources = "https://github.com/toxicity188/BetterModel/"
        issues = "https://github.com/toxicity188/BetterModel/issues"
    }
    icon("assets/icon.png")
    mitLicense()
}

# ⚡ BetterModel
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.toxicity188/bettermodel?style=flat-square&logo=sonatype)
[![CodeFactor](https://www.codefactor.io/repository/github/toxicity188/bettermodel/badge?style=flat-square)](https://www.codefactor.io/repository/github/toxicity188/bettermodel)
[![Total line](https://tokei.rs/b1/github/toxicity188/BetterModel?category=code&style=flat-square)](https://github.com/toxicity188/BetterModel)
[![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/toxicity188/BetterModel?style=flat-square&logo=github)](https://github.com/toxicity188/BetterModel/issues)
[![](https://img.shields.io/github/actions/workflow/status/toxicity188/BetterModel/publish.yml?style=flat-square)](https://modrinth.com/plugin/bettermodel/versions)

* * *
![](https://github.com/user-attachments/assets/5a6c1a8c-6fe2-4a67-a10e-e63e40825d35)
![](https://github.com/user-attachments/assets/ff515577-6a72-48ba-9943-81f00dddb375)
* * *

This plugin implements **fully server-side 3D model** by using an item display entity packet.

- Importing Generic BlockBench model `.bbmodel`
- Auto-generating resource pack
- Playing animation
- Syncing with base entity
- Custom hit box
- Supports 12-limb player animation

## 🔧Build info

#### Build
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/gradle_vector.svg)](https://gradle.org/)

`./gradlew build`: Builds all jars  
`./gradlew shadowJar`: Builds plugin jar  
`./gradlew javadocJar`: Builds javadoc jar

> [!NOTE]\
> It requires Java 21

#### Library
- [Kotlin stdlib](https://github.com/JetBrains/kotlin): modern functional programming
- [semver4j](https://github.com/vdurmont/semver4j): semver parser
- [CommandAPI](https://github.com/CommandAPI/CommandAPI): command
- [adventure](https://github.com/KyoriPowered/adventure): component
- [stable player display](https://github.com/bradleyq/stable_player_display): player animation
- [caffeine](https://github.com/ben-manes/caffeine): concurrent map cache
- [DynamicUV](https://github.com/toxicity188/DynamicUV): player model-uv
- [molang-compiler](https://github.com/Ocelot5836/molang-compiler): compiling and evaluating molang expression
- [libby](https://github.com/AlessioDP/libby): runtime library downloader


## 📗 Wiki
[![](https://img.shields.io/badge/Github%20Wiki-181717?logo=github&logoColor=white)](https://github.com/toxicity188/BetterModel/wiki)
[![](https://deepwiki.com/badge.svg)](https://deepwiki.com/toxicity188/BetterModel)

## 💻 API

![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/maven-central_vector.svg)

You can see an API examples in [here](https://github.com/toxicity188/BetterModel/wiki/API-example). 

#### Release
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.toxicity188:bettermodel:VERSION")
}
```
#### Snapshot
```kotlin
repositories {
    maven("https://maven.pkg.github.com/toxicity188/BetterModel") {
        credentials {
            username = YOUR_GITHUB_USERNAME
            password = YOUR_GITHUB_TOKEN
        }
    }
}

dependencies {
    compileOnly("io.github.toxicity188:bettermodel:VERSION-SNAPSHOT")
}
```

## 🌈 Community
[![](https://discord.com/api/guilds/1012718460297551943/widget.png?style=banner2)](https://discord.com/invite/rePyFESDbk)

## 💖 Support
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/buymeacoffee-singular_vector.svg)](https://buymeacoffee.com/toxicity188)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/ghsponsors-singular_vector.svg)](https://github.com/sponsors/toxicity188)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/paypal-singular_vector.svg)](https://www.paypal.com/paypalme/toxicity188?country.x=KR&locale.x=en_US)
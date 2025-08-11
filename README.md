## ⚡Lightweight BlockBench model engine for Bukkit
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.toxicity188/bettermodel?style=flat-square&logo=sonatype)
[![CodeFactor](https://www.codefactor.io/repository/github/toxicity188/bettermodel/badge?style=flat-square)](https://www.codefactor.io/repository/github/toxicity188/bettermodel)
[![Total line](https://tokei.rs/b1/github/toxicity188/BetterModel?category=code&style=flat-square)](https://github.com/toxicity188/BetterModel)
[![Discord](https://img.shields.io/badge/Discord-%235865F2.svg?style=flat-square&logo=discord&logoColor=white)](https://discord.com/invite/rePyFESDbk)
[![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/toxicity188/BetterModel?style=flat-square&logo=github)](https://github.com/toxicity188/BetterModel/issues)
[![bStats Servers](https://img.shields.io/bstats/servers/24237?style=flat-square&logo=minecraft&label=bStats&color=0%2C150%2C136%2C0)](https://bstats.org/plugin/bukkit/BetterModel/24237)

* * *
![](https://github.com/user-attachments/assets/5a6c1a8c-6fe2-4a67-a10e-e63e40825d35)
![](https://github.com/user-attachments/assets/ff515577-6a72-48ba-9943-81f00dddb375)
* * *

This plugin implements the **fully server-side 3D model** by using an item-display packet.

- Importing Generic BlockBench model `.bbmodel`
- Auto-generating resource pack
- Playing animation
- Syncing with base entity
- Custom hit box
- Supports 12-limb player animation

## 🔧Build info
#### Library
- [Kotlin stdlib](https://github.com/JetBrains/kotlin): modern functional programming
- [semver4j](https://github.com/vdurmont/semver4j): semver parser
- [CommandAPI](https://github.com/CommandAPI/CommandAPI): command
- [adventure](https://github.com/KyoriPowered/adventure): component
- [stable player display](https://github.com/bradleyq/stable_player_display): player animation
- [expiringmap](https://github.com/jhalterman/expiringmap): concurrent map cache
- [DynamicUV](https://github.com/toxicity188/DynamicUV): player model-uv
- [molang-compiler](https://github.com/Ocelot5836/molang-compiler): compiling and evaluating molang expression

#### Build
`./gradlew build`
> [!NOTE]\
> It requires Java 21

#### API
We are using maven central and gradle packages.  
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
    maven("https://maven.pkg.github.com/toxicity188/BetterModel")
}

dependencies {
    compileOnly("io.github.toxicity188:bettermodel:VERSION-SNAPSHOT")
}
```
* * *
[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://buymeacoffee.com/toxicity188)
<div align="center">

![](https://github.com/user-attachments/assets/89e191ba-ed4f-44ab-bb98-634cfe568dca)

# BetterModel
*- Modern Bedrock model engine for Minecraft Java Edition -*

[![](https://img.shields.io/maven-central/v/io.github.toxicity188/bettermodel-api?style=flat-square&logo=sonatype)](https://central.sonatype.com/artifact/io.github.toxicity188/bettermodel-api)
[![](https://www.codefactor.io/repository/github/toxicity188/bettermodel/badge?style=flat-square)](https://www.codefactor.io/repository/github/toxicity188/bettermodel)
[![](https://img.shields.io/github/actions/workflow/status/toxicity188/BetterModel/publish.yml?style=flat-square)](https://modrinth.com/plugin/bettermodel/versions)
[![](https://img.shields.io/github/issues/toxicity188/BetterModel?style=flat-square&logo=github)](https://github.com/toxicity188/BetterModel/issues)
[![](https://img.shields.io/bstats/servers/24237?style=flat-square)](https://bstats.org/plugin/bukkit/BetterModel/24237)

</div>

* * *
![](https://github.com/user-attachments/assets/5a6c1a8c-6fe2-4a67-a10e-e63e40825d35)
![](https://github.com/user-attachments/assets/ff515577-6a72-48ba-9943-81f00dddb375)

* * *
<sub>(In BlockBench / In Minecraft)</sub>

# ✨ Introduction

**BetterModel** is a server-based engine that provides runtime BlockBench model rendering & animating for Minecraft Java Edition.

It implements **fully server-side 3D models** by using an item display entity packet.

- Importing Generic BlockBench model `.bbmodel`
- Auto-generating resource pack
- Playing animation
- Syncing with base entity
- Custom hit box
- 12-limb player animation

<details>
<summary>In-Game Screenshots</summary>

![](https://github.com/user-attachments/assets/b4e69aef-a446-4ac3-b84e-eb42fe4f069d)  
![](https://github.com/user-attachments/assets/94aee9ed-9c2f-4975-92c4-3ea84ae31d24)  
![](https://github.com/user-attachments/assets/eb2d64ef-7b6e-4306-8c31-d92d0266dbac)  
![](https://github.com/user-attachments/assets/034dd64c-6889-4a01-961d-e69679b1c71b)
</details>

## 🚀 Key Features & Focus
BetterModel aims to be a reliable engine that provides stable, high-quality animations for Paper-based high-traffic servers.

- **Stability First**: We take a conservative approach to feature expansion. By avoiding the implementation of features that are difficult to maintain or have limited use cases, we focus on providing a stable API and ensuring overall operational safety.
- **Performance Optimized**: Our goal is to minimize runtime computation, memory footprint, and network overhead. Through asynchronous design and optimized packet handling, we ensure the engine runs efficiently even under heavy server loads.
- **Tailored for Large-scale Servers**: We provide essential features specifically designed for high-population servers and MMORPG content creation.
  - **Per-player Animation**: Individual animation control tailored to each player's perspective.
  - **Player Model Animation**: Support for sophisticated 12-limb animations based on player models.

## 📚 Wiki
[![](https://img.shields.io/badge/GitHub%20Wiki-181717?logo=github&logoColor=white)](https://github.com/toxicity188/BetterModel/wiki)
[![](https://deepwiki.com/badge.svg)](https://deepwiki.com/toxicity188/BetterModel)

## 🛠️ Build info

[![](https://img.shields.io/badge/minecraft-1.21%7E1.21.11-8FCA5C)](https://www.minecraft.net/en-us/download/server)
[![](https://img.shields.io/badge/java-21%7E-ED8B00)](https://adoptium.net/)

#### Build
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/gradle_vector.svg)](https://gradle.org/)

`./gradlew build`: Builds all jars  
`./gradlew shadowJar`: Builds plugin jar  
`./gradlew javadocJar`: Builds javadoc jar  
`./gradlew runServer`: Runs Paper test server with test plugin

#### Library
- [Kotlin stdlib](https://github.com/JetBrains/kotlin): modern functional programming
- [semver4j](https://github.com/vdurmont/semver4j): semver parser
- [cloud](https://github.com/Incendo/cloud-minecraft): command
- [adventure](https://github.com/KyoriPowered/adventure): component
- [stable player display](https://github.com/bradleyq/stable_player_display): player animation
- [caffeine](https://github.com/ben-manes/caffeine): concurrent map cache
- [DynamicUV](https://github.com/toxicity188/DynamicUV): player model
- [ArmorModel](https://github.com/toxicity188/ArmorModel): armor in player model
- [molang-compiler](https://github.com/Ocelot5836/molang-compiler): compiling and evaluating molang expression
- [libby](https://github.com/AlessioDP/libby): runtime library downloader

#### Tested Bukkit Server Platform
- [Paper](https://papermc.io/downloads/paper)
- [Purpur](https://purpurmc.org/download/purpur)
- [Spigot](https://www.spigotmc.org/)
- [Folia](https://papermc.io/downloads/folia)
- [Leaf](https://www.leafmc.one/download)
- [Canvas](https://canvasmc.io/downloads/canvas)

#### Tested Mod Server Platform
- [Fabric Loader](https://fabricmc.net/)

## 💻 API

[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/maven-central_vector.svg)](https://central.sonatype.com/artifact/io.github.toxicity188/bettermodel)

> [!NOTE]\
> For more detailed API specifications, please refer to our [GitHub Wiki](https://github.com/toxicity188/BetterModel/wiki/API-example).

<details open>
<summary>Gradle (Kotlin)</summary>

#### Release
```kotlin
repositories {
    mavenCentral()
    maven("https://maven.blamejared.com/") // For transitive dependency in bettermodel-fabric
    maven("https://maven.nucleoid.xyz/") // For transitive dependency in bettermodel-fabric
}

dependencies {
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:VERSION") // bukkit(spigot, paper, etc) api
    //modApi("io.github.toxicity188:bettermodel-fabric:VERSION") // mod(fabric)
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
    maven("https://maven.blamejared.com/") // For transitive dependency in bettermodel-fabric
    maven("https://maven.nucleoid.xyz/") // For transitive dependency in bettermodel-fabric
}

dependencies {
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:VERSION-SNAPSHOT") // bukkit(spigot, paper, etc) api
    //modApi("io.github.toxicity188:bettermodel-fabric:VERSION-SNAPSHOT") // mod(fabric)
}
```
</details>

<details>
<summary>Gradle (Groovy)</summary>

#### Release
```groovy
repositories {
    mavenCentral()
    maven 'https://maven.blamejared.com/' // For transitive dependency in bettermodel-fabric
    maven 'https://maven.nucleoid.xyz/' // For transitive dependency in bettermodel-fabric
}

dependencies {
    compileOnly 'io.github.toxicity188:bettermodel-bukkit-api:VERSION' // bukkit(spigot, paper, etc) api
    //modApi 'io.github.toxicity188:bettermodel-fabric:VERSION' // mod(fabric)
}
```

#### Snapshot
```groovy
repositories {
    maven {
        url "https://maven.pkg.github.com/toxicity188/BetterModel"
        credentials {
            username = YOUR_GITHUB_USERNAME
            password = YOUR_GITHUB_TOKEN
        }
    }
    maven 'https://maven.blamejared.com/' // For transitive dependency in bettermodel-fabric
    maven 'https://maven.nucleoid.xyz/' // For transitive dependency in bettermodel-fabric
}

dependencies {
    compileOnly 'io.github.toxicity188:bettermodel-bukkit-api:VERSION-SNAPSHOT' // bukkit(spigot, paper, etc) api
    //modApi 'io.github.toxicity188:bettermodel-fabric:VERSION-SNAPSHOT' // mod(fabric)
}
```
</details>

<details>
<summary>Maven</summary>

#### Release
```xml
<repositories>
    <repository>
        <id>central</id>
        <url>https://repo.maven.apache.org/maven2</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.toxicity188</groupId>
        <artifactId>bettermodel-bukkit-api</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### Snapshot
```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/toxicity188/BetterModel</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.toxicity188</groupId>
        <artifactId>bettermodel-api</artifactId>
        <version>VERSION-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.github.toxicity188</groupId>
        <artifactId>bettermodel-bukkit-api</artifactId>
        <version>VERSION-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```
</details>

<details>
<summary>Example code</summary>

#### Gets some model or limb
```java
BetterModel.model("demon_knight"); //A model file in BetterModel/models (for general model with saving)
BetterModel.limb("steve"); //A model file in BetterModel/players (for player model with no saveing)

BetterModel.modelOrNull("demon_knight"); //general model or null
BetterModel.limbOrNull("steve"); //player model or null
```

#### Creates model (entity)
```java
EntityTracker tracker = BetterModel.model("demon_knight")
    .map(r -> r.getOrCreate(BukkitAdapter.adapt(entity))) //Gets or creates entity tracker by this renderer to some entity.
    .orElse(null);
```
```java
EntityTracker tracker = BetterModel.model("demon_knight")
    .map(r -> r.create(BukkitAdapter.adapt(entity), TrackerModifier.DEFAULT, t -> t.update(TrackerUpdateAction.tint(0x0026FF)))) //Creates entity tracker with pre-spawn task.
    .orElse(null);
```

#### Creates model (dummy)
```java
DummyTracker tracker = BetterModel.model("demon_knight")
    .map(r -> r.create(BukkitAdapter.adapt(location))) //Creates some dummy tracker to this location.
    .orElse(null);
```
```java
DummyTracker tracker = BetterModel.limb("steve")
    .map(r -> r.create(BukkitAdapter.adapt(location), ModelProfile.of(BukkitAdapter.adapt(player)))) //Creates some dummy tracker to this location and player's skin profile.
    .orElse(null);
```

#### Update some tracker's display data
```java
BetterModel.model("demon_knight")
    .map(r -> r.create(BukkitAdapter.adapt(entity), TrackerModifier.DEFAULT, t -> {
        t.update(TrackerUpdateAction.tint(rgb)); //Tint
        t.update(TrackerUpdateAction.enchant(true), bone -> true); //Enchant with predicate
    }))
    .ifPresent(tracker -> tracker.update(TrackerUpdateAction.composite( //Composite
        TrackerUpdateAction.brightness(15, 15)  //Brightness
        TrackerUpdateAction.billboard(Display.Billboard.CENTER) //Billboard
    )));
}
```

</details>

## 💬 Community
[![](https://discord.com/api/guilds/1012718460297551943/widget.png?style=banner2)](https://discord.com/invite/rePyFESDbk)

## 💖 Support
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/buymeacoffee-singular_vector.svg)](https://buymeacoffee.com/toxicity188)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/ghsponsors-singular_vector.svg)](https://github.com/sponsors/toxicity188)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/paypal-singular_vector.svg)](https://www.paypal.com/paypalme/toxicity188?country.x=KR&locale.x=en_US)

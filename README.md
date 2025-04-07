<div align="center">  

![0](https://github.com/user-attachments/assets/d9b46080-e7ab-4e12-a395-174c94e63cea)

Welcome to BetterModel!

[SpigotMC](https://www.spigotmc.org/resources/121561/) | [GitHub](https://github.com/toxicity188/BetterModel) | [Modrinth](https://modrinth.com/plugin/bettermodel) | [Hangar](https://hangar.papermc.io/toxicity188/BetterModel)

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.toxicity188/BetterModel?style=for-the-badge)
[![GitHub Release](https://img.shields.io/github/v/release/toxicity188/BetterModel?display_name=release&style=for-the-badge&logo=kotlin)](https://github.com/toxicity188/BetterModel/releases/latest)
[![Discord](https://img.shields.io/badge/Discord-%235865F2.svg?style=for-the-badge&logo=discord&logoColor=white)](https://discord.com/invite/rePyFESDbk)
[![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/toxicity188/BetterModel?style=for-the-badge&logo=github)](https://github.com/toxicity188/BetterModel/issues)
[![bStats Servers](https://img.shields.io/bstats/servers/24237?style=for-the-badge&logo=minecraft&label=bStats&color=0%2C150%2C136%2C0)](https://bstats.org/plugin/bukkit/BetterModel/24237)

</div>

### Modern lightweight Minecraft model implementation for Bukkit, Folia
This plugin implements server-side model by using packet-based item display.

- Importing .bbmodel
- Resource pack auto-generation
- Multiple hit-box

### Why I created BetterModel even though ModelEngine already exists?
The main reason I created it is:
- To reduce traffic - MEG uses too many packet, making significant network issue.
- To provide faster update - MEG's update is too slow.
- To provide more flexible API - MEG is a closed-source plugin and API is very limited.

### Summary
![1](https://github.com/user-attachments/assets/397a1ff3-37f2-4e3a-9868-732d3a044c4c)  
This plugin, of course, implements generic block bench model with animation.  

### Network optimization
![3](https://github.com/user-attachments/assets/6c421f33-682e-4674-a5d2-13310e5dbbfe)  
This plugin can optimize network by **sight-trace**. It won't send packets when the player's not looking.  

### Multiple hit-box
![4](https://github.com/user-attachments/assets/dc311327-1212-4889-a6c8-dd71cc8f2c8b)  
![5](https://github.com/user-attachments/assets/0fc3250d-ef4f-4e64-9cc2-c143a474d046)  
This plugin provides multiple hit-box both client and server. (tracking animation rotation)

### MythicMobs support (experimental)
![6](https://github.com/user-attachments/assets/06804f8c-af0d-46ce-adff-b868b65ba44a)  
![7](https://github.com/user-attachments/assets/d2f7e69f-dce1-4fa6-84cd-83b35560a0b4)  
Like MEG, This plugin implements simple MythicMobs support.
```
model
state
defaultstate
partvisibility
changepart
tint
brightness
```

### Player animation
![8](https://github.com/user-attachments/assets/1683eae7-e22d-4919-a660-2f0df2ab8a09)  
This plugin supports player animation.

### Version
- Spigot, Paper or a fork (e.g. Folia)
- Java 21
- Minecraft 1.20.2-1.21.5

### Dependency
No

### Command
/bettermodel reload - Reloads this plugin.  
/bettermodel spawn <model> - Summons this model to husk.  
/bettermodel disguise <model> - disguises self.  
/bettermodel undisguise <model> - undisguises self  
/bettermodel limb <true/false> - Shows/Hides player limb.  
/bettermodel play <model> <animation> - Plays player animation.  

### Permission
bettermodel - Accesses to command.

### Build
./gradlew build

### API
We are using maven central.
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.toxicity188:BetterModel:VERSION")
}
```

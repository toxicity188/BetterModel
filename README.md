<div align="center">  

![0](https://github.com/user-attachments/assets/4cb52a01-b938-42f4-ade6-83b33ce53320)

Welcome to BetterModel!

[Github](https://github.com/toxicity188/BetterModel)  

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.toxicity188/BetterModel?style=for-the-badge)
[![GitHub Release](https://img.shields.io/github/v/release/toxicity188/BetterModel?display_name=release&style=for-the-badge&logo=kotlin)](https://github.com/toxicity188/BetterModel/releases/latest)
[![Discord](https://img.shields.io/badge/Discord-%235865F2.svg?style=for-the-badge&logo=discord&logoColor=white)](https://discord.com/invite/rePyFESDbk)
[![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/toxicity188/BetterModel?style=for-the-badge&logo=github)](https://github.com/toxicity188/BetterModel/issues)
[![bStats Servers](https://img.shields.io/bstats/servers/24237?style=for-the-badge&logo=minecraft&label=bStats&color=0%2C150%2C136%2C0)](https://bstats.org/plugin/bukkit/BetterModel/24237)

</div>

### Modern lightweight Minecraft model implementation for Paper, Folia
This plugin implements server-side model by using packet-based item display.

- Importing .bbmodel
- Resource pack auto-generation
- Multiple hit-box

### Why I have to make this even ModelEngine exists?
The main reason I made this is that:
- To reduce traffic - MEG uses too many packet, making significant network issue.
- To provide faster update - MEG's update is too slow.
- To provide more flexible API - MEG is closed-source plugin and API is very limited.

### Summary
![1](https://github.com/user-attachments/assets/142136b7-f508-457e-8d69-ba93b8ddb567)  
This plugin, of course, implements generic block bench model with animation.  
![2](https://github.com/user-attachments/assets/e6d899e4-a3b0-4ee9-a62d-664e10b06709)  
This plugin can track entity's movement and head rotation.


### Network optimization
![3](https://github.com/user-attachments/assets/6c421f33-682e-4674-a5d2-13310e5dbbfe)  
This plugin can optimize network by **sight-trace**. don't send some packet when player not look this.  

### Multiple hit-box
![4](https://github.com/user-attachments/assets/dc311327-1212-4889-a6c8-dd71cc8f2c8b)  
![5](https://github.com/user-attachments/assets/0fc3250d-ef4f-4e64-9cc2-c143a474d046)  
This plugin provides multiple hit-box both client and server. (tracking animation rotation)

### MythicMobs support (experimental)
![6](https://github.com/user-attachments/assets/542f80ea-e3a7-4ea1-9875-181c77229739)  
![7](https://github.com/user-attachments/assets/13e95fab-bb3d-40f3-b205-76751d3007cf)  
Like MEG, This plugin implements simple MythicMobs support.
```
model
state
defaultstate
partvisibility
```

### Version
- Paper of that's fork (with Folia)
- Java 21
- Minecraft 1.20.4-1.21.4

### Dependency
No

### Command
/bettermodel reload - Reloads this plugin.  
/bettermodel spawn <model> - Summons this model to husk.

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

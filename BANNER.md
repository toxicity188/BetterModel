<div align="center">  

![0](https://github.com/user-attachments/assets/89e191ba-ed4f-44ab-bb98-634cfe568dca)

Welcome to **BetterModel**!

[![](https://img.shields.io/badge/SpigotMC-ED8106?style=for-the-badge&logo=SpigotMC&logoColor=white)](https://www.spigotmc.org/resources/121561/)
[![](https://img.shields.io/badge/Modrinth-00AF5C?style=for-the-badge&logo=Modrinth&logoColor=white)](https://modrinth.com/plugin/bettermodel)
[![](https://img.shields.io/badge/Hangar-185DEB?style=for-the-badge&logo=Hangar&logoColor=white)](https://hangar.papermc.io/toxicity188/BetterModel)
[![](https://img.shields.io/badge/Github-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/toxicity188/BetterModel)

</div>

## ⚡Lightweight BlockBench model engine for Bukkit
* * *
![](https://github.com/user-attachments/assets/5a6c1a8c-6fe2-4a67-a10e-e63e40825d35)
![](https://github.com/user-attachments/assets/ff515577-6a72-48ba-9943-81f00dddb375)
* * *

This plugin implements **fully server-side 3D model** by using an item-display packet.

- Importing Generic BlockBench model `.bbmodel`
- Auto-generating resource pack
- Playing animation
- Syncing with base entity
- Custom hit box
- Supports 12-limb player animation

#### 🔥Why do I create BetterModel even though ModelEngine already exists?
The main reason I created it is:
- To reduce network cost—MEG’s network optimization is outdated and insufficient for modern servers.
- To enable faster updates—We can’t afford to wait for MEG’s slow update cycle anymore.
- To provide a more flexible API—MEG is closed-source with a very limited API, which makes extending or integrating difficult.
- To restore vanilla behavior-MEG breaks several vanilla entity features and physics, which this project aims to fix.

Also, you can refer [my document](https://github.com/toxicity188/BetterModel/wiki/Compare-with-ModelEngine) to compare both ModelEngine and BetterModel.

## 🌎Generic BlockBench model with animation
![](https://github.com/user-attachments/assets/b4e69aef-a446-4ac3-b84e-eb42fe4f069d)
* * *
[![](https://img.shields.io/badge/YouTube-FF0000?style=for-the-badge&logo=YouTube&logoColor=white)](https://youtu.be/f3U7Lmo3aA8?si=SnglL0YKn20CrR7Y)  
This plugin, of course, implements a Generic BlockBench model with animation.

####  Optimization
BetterModel has been trying many kind of CPU and network cost reduce.
- culling by sight
- async entity spawn/despawn handling
- parallel packet bundling
- and much more

You can see the report of network optimization in [here](https://github.com/toxicity188/BetterModel/wiki/Report-about-network-cost-about-two-model-plugin-(ModelEngine,-BetterModel)).

#### Custom hitbox
* * *
![](https://github.com/user-attachments/assets/94aee9ed-9c2f-4975-92c4-3ea84ae31d24)
* * *
This plugin provides **custom hitbox** both client and server. (tracking animation rotation)

#### MythicMobs support
* * *
![](https://github.com/user-attachments/assets/eb2d64ef-7b6e-4306-8c31-d92d0266dbac)
* * *
Like MEG, This plugin supports **MythicMobs**, you can use some MEG's mechanics in BetterModel too.

## 💡Player model with animation
![](https://github.com/user-attachments/assets/0c13bec2-898f-4d9a-a709-10e0571337f3)
![](https://github.com/user-attachments/assets/034dd64c-6889-4a01-961d-e69679b1c71b)
* * *
This plugin supports **player model with using user's custom skin without textures**.

## 🚀Plugin support
[![](https://img.shields.io/badge/Github%20Wiki-181717?logo=github&logoColor=white)](https://github.com/toxicity188/BetterModel/wiki)
[![](https://deepwiki.com/badge.svg)](https://deepwiki.com/toxicity188/BetterModel)
#### Platform
- [Spigot](https://www.spigotmc.org/)
- [Paper](https://papermc.io/downloads/paper) or a fork (e.g., [Folia](https://papermc.io/downloads/folia))

⚠️**Paper is highly recommended.**

#### Version
- Java **21** or higher
- Minecraft server **1.20.5-1.21.8**
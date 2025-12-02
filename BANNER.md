<div align="center">  

![](https://github.com/user-attachments/assets/89e191ba-ed4f-44ab-bb98-634cfe568dca)

[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/plugin/bettermodel)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/hangar_vector.svg)](https://hangar.papermc.io/toxicity188/BetterModel)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg)](https://github.com/toxicity188/BetterModel)

</div>

# ⚡ What is BetterModel?
* * *
![](https://github.com/user-attachments/assets/5a6c1a8c-6fe2-4a67-a10e-e63e40825d35)
![](https://github.com/user-attachments/assets/ff515577-6a72-48ba-9943-81f00dddb375)
* * *

**BetterModel** is a plugin-based engine that provide runtime BlockBench model rendering & animating for Minecraft Java Edition.  

It implements **fully server-side 3D model** by using an item display entity packet.

- Importing Generic BlockBench model `.bbmodel`
- Auto-generating resource pack
- Playing animation
- Syncing with base entity
- Custom hit box
- Supports 12-limb player animation

## 🔥 Why do I create BetterModel even though ModelEngine already exists?
The main reason I created it is:
- To reduce network cost—MEG’s network optimization is outdated and insufficient for modern servers.
- To enable faster updates—We can’t afford to wait for MEG’s slow update cycle anymore.
- To provide a more flexible API—MEG is closed-source with a very limited API, which makes extending or integrating difficult.
- To restore vanilla behavior-MEG breaks several vanilla entity features and physics, which this project aims to fix.

Also, you can refer [my document](https://github.com/toxicity188/BetterModel/wiki/Compare-with-ModelEngine) to compare both ModelEngine and BetterModel.

## 🌎 Generic BlockBench model with animation
![](https://github.com/user-attachments/assets/b4e69aef-a446-4ac3-b84e-eb42fe4f069d)
* * *
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/youtube-singular_vector.svg)](https://youtu.be/f3U7Lmo3aA8?si=SnglL0YKn20CrR7Y)  
This plugin, of course, implements a Generic BlockBench model with animation.

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

## 💡 Player model with animation
![](https://github.com/user-attachments/assets/0c13bec2-898f-4d9a-a709-10e0571337f3)
![](https://github.com/user-attachments/assets/034dd64c-6889-4a01-961d-e69679b1c71b)
* * *
This plugin supports **player model with using user's custom skin without textures**.

## 📗 Official wiki
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/ghpages_vector.svg)](https://github.com/toxicity188/BetterModel/wiki)

## 🚀 Supported environment

[![](https://img.shields.io/badge/minecraft-1.20.5%7E1.21.11-8FCA5C?style=for-the-badge)](https://www.minecraft.net/en-us/download/server)
[![](https://img.shields.io/badge/java-21%7E-ED8B00?style=for-the-badge)](https://adoptium.net/)

[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/paper_vector.svg)](https://papermc.io/downloads/paper)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/purpur_vector.svg)](https://purpurmc.org/)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/spigot_vector.svg)](https://www.spigotmc.org/)

[![](https://img.shields.io/badge/folia-supported-blue?style=for-the-badge)](https://papermc.io/downloads/folia)

## 🌈 My plugin community
[![](https://discord.com/api/guilds/1012718460297551943/widget.png?style=banner2)](https://discord.com/invite/rePyFESDbk)

## 📊 Plugin Stats
[![](https://bstats.org/signatures/bukkit/BetterModel.svg)](https://bstats.org/plugin/bukkit/BetterModel/24237)

## 💖 Support my plugin
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/buymeacoffee-singular_vector.svg)](https://buymeacoffee.com/toxicity188)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/ghsponsors-singular_vector.svg)](https://github.com/sponsors/toxicity188)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/paypal-singular_vector.svg)](https://www.paypal.com/paypalme/toxicity188?country.x=KR&locale.x=en_US)

# GSTweaks

![GSPack logo](src/main/resources/gspack-logo.png)

**Authors** — Steef_an & ChatGPT Codex

GSTweaks is available under the [MIT License](LICENSE). The included NeoForge template files are covered by the [NeoForge template license](TEMPLATE_LICENSE.txt).

GSTweaks is a NeoForge mod for Minecraft **26.1.2** and Java 25. It registers the five Cores, the Creative Singularity, the placeable Trophy of Infinity, Crux blocks, and Mars items and blocks. Existing GSPack registry IDs are preserved.

Use NeoForge **26.1.2.122 or later** for Minecraft 26.1.2. See the [GSTweaks wiki](https://github.com/steefan1998/GSTweaks/wiki) for installation, settings, and mod compatibility.

## What's new in 0.3

- **Tinkers Hard Mode** gives operators a single switch for wooden vanilla tools, Tinker ingredients in modded recipes, and disabling Mekanism and AllTheModium paxel recipes.
- **Molten Dark Matter and Red Matter** add smelting and casting recipes for Equivox or ProjectE items.
- **Crude oil unification** lets Oritech, BuildCraft, and Stellaris recipes work with a common base oil. Mekanism supplies the conversion machine when Oritech is absent.

## Optional compatibility

| Mod | Integration |
| --- | --- |
| Stellaris | Expands `stellaris:mars` with biomes, dungeons, villages, camps, mobs, and oxygen systems. |
| AllTheOres | Adds Martian osmium and uranium, their ore drops, and optional duplicate ore unification. |
| AllTheModium | Adds Martian unobtainium with its original mining protection, XP, and drop. With Tinkers, a Tinker tool of netherite tier or higher, for example after the netherite upgrade, can mine Allthemodium ores. Vibranium and unobtainium still need their own tiers. |
| Mekanism Additions | Adds Baby Creepers on Mars and in the research dome. The dome has a regular Creeper when Mekanism Additions is absent. |
| JEI | Hides configured material duplicates when the preferred variant is available. |
| Thaumaturge | Adds material aspects for common tags and lets the Thaumometer record modded blocks that have no aspects. |
| Tinkers’ Construct / Continuum Construct | Adds missing melting recipes and 29 optional molten materials. Every material has its own stats for heads, handles, and bindings, along with casting, repair, and recycling recipes. |
| FTB Library / FTB Essentials | Adds inventory sidebar buttons for Daily Rewards and the operator-only GSTweaks Options. |
| FTB Library + Weather Story | Makes the FTB rain button toggle local rain and temporarily clear weather in dimensions managed by Weather Story. |
| Powah and Extended Crafting | GSPack's KubeJS scripts skip individual recipes when a required mod or ingredient is missing. |

The [molten materials list](src/main/resources/molten-materials.json) shows which fluids GSTweaks adds. Each fluid is registered when Tinkers and the corresponding material mod are installed. Existing melting recipes take precedence. Ore-based casting needs a matching ore and registered output. Dark Matter and Red Matter use refined items directly.

The [tool part stats](src/main/resources/packs/tinkers_materials/data/gstweaks/tinkering/materials/stats) define each material's trade-offs. Desh is light and fast, boron trades speed for durability, and novite deals relatively high damage with less durability than titanium. Salt, saltpeter, and sulfur are cheap, stone-tier materials. Dark Matter and Red Matter are the strongest.

With Equivox or ProjectE, one Dark Matter or Red Matter item melts into 100 mB. Dark Matter melts at 1100 °C and Red Matter at 1200 °C. Both are netherite-tier tool materials. A reusable gem cast turns 100 mB back into one item. If both mods are present, the recipes use Equivox items. The ProjectE integration recognizes the original item IDs. GSTweaks builds target Minecraft 26.1.2.

## Migrating from GSPack

1. Close Minecraft or stop the server. Make a copy of the instance and the world.
2. Place `GSTweaks-0.3.1.jar` in the copy's `mods` folder. Remove any older GSTweaks jar from that folder.
3. Start the client and server. GSTweaks archives recognized legacy files before KubeJS reads them. The converter installs updated recipes and stops if it finds customized files that need to be merged manually.

Automatic backups are stored in `gstweaks-backup/kubejs-*/kubejs`. To roll back, use the original world copy, restore the archived files, and remove the new jar and the files listed in `created-files.json`.

Run this command from the project directory to preview the migration.

```powershell
.\migration\Install-GSTweaks.ps1 -Instance 'D:\path\to\GSPack-copy'
```

The mod ID is `gstweaks`. Registry IDs such as `gspack:tech_core`, `gspack:infinity_trophy`, and `stellaris:mars` stay the same. GSTweaks converts legacy Mars ores as chunks load. Existing settings in `config/gspack-mars.json` still apply.

If an older custom version used different item or block IDs, add aliases to `config/gstweaks-id-aliases.json` on both the client and server. Only use legacy IDs that are available after the migration.

```json
{
  "items": {
    "kubejs:tech_core": "gspack:tech_core",
    "kubejs:infinity_trophy": "gspack:infinity_trophy"
  },
  "blocks": {
    "kubejs:infinity_trophy": "gspack:infinity_trophy"
  }
}
```

## Unification settings

GSTweaks creates `config/gstweaks/unification.json` on first launch. The order of `groups[].items` sets the preference. GSTweaks and the KubeJS recipe script both choose the first registered variant. `convertBlocks` enables block conversion for an individual material group. `blockReplacements` lists explicit block ID replacements for older worlds.

Use `enabled` to toggle unification. `overrideOreData` controls whether GSTweaks applies GSPack's world generation and drop overrides for duplicate ores when AllTheOres is installed. `hiddenItems` and `hiddenOres` control which duplicates JEI hides. Use the same settings on the client and server, and restart after making changes.

## GSTweaks settings

GSTweaks creates `config/gstweaks/gstweaks.json` on first launch. Changes take effect after a restart.

| Setting | Effect |
| --- | --- |
| `autoMigrateKubeJs` | Runs the KubeJS converter when the mod loads. |
| `endgameTooltips` | Shows additional information on the Cores and Trophy. |
| `migrateMarsOres` | Replaces legacy Mars ores as chunks load. |
| `marsZombieSuit`, `marsRaidPillagers`, `marsVillageLifeSupport` | Controls each Mars feature independently. |
| `thaumaturgeCompat` | Enables additional scans and material aspects. |
| `tinkersOreRecipes` | Enables additional melting and casting recipes. |
| `tinkersToolMaterials` | Enables the new tool part materials and their recipes. |
| `unifyOil` | On by default. Converts supported crude oils to a [common base oil](https://github.com/steefan1998/GSTweaks/wiki/Mod-Compatibility#crude-oil-unification). |
| `woodenToolsOnly` | Off by default. Enables Tinkers Hard Mode and requires Continuum Tinker. |

### Tinkers Hard Mode

Hard Mode keeps the wooden vanilla recipes for pickaxes, axes, shovels, hoes, swords, and spears. It removes recipes for those items from stone through netherite, including smithing upgrades, and blocks Mekanism and AllTheModium paxel recipes. Supported modded recipes that ask for a vanilla tool use the corresponding Continuum Tinker tool by type. A replacement pickaxe must meet the original mining tier or higher. For example, the BuildCraft Quarry requires a diamond-tier or better Tinker pickaxe.

Operators can open **GSTweaks Options** with `/gstweaks options` or the FTB inventory sidebar. Run `/gstweaks options wooden_tools_only true` to enable Hard Mode or use `false` to restore the recipes. Omit the slash in the server console. The command saves the setting and reloads server recipes immediately. The game or server may briefly freeze during the reload.

### JustDireThings compatibility

With JustDireThings and Continuum Tinker installed, use the JustDireThings Smelter, Glowing, or Hammer upgrade in a Tinker Station to apply Autosmelt, Glowing, or Expanded respectively. The Continuum Tinker modifier's tool restrictions and slot cost apply. JustDireThings upgrade recipes that call for a pickaxe accept a Tinker pickaxe of the required mining tier. With Hard Mode off, those recipes also accept their original vanilla pickaxe.

## Daily Rewards

Open the menu with `/gstweaks daily` or use the FTB sidebar. Each player can claim one reward per day from the menu or with `/gstweaks daily claim`. By default, a reward becomes available after **30 minutes of online playtime** that day. Rewards reset at **midnight in the host's time zone**. Playtime is saved per player in the world.

Operators can inspect an online player's status with `/gstweaks daily debug <player>`. This command only displays the player's status. The implementation is based on [GSMod-DailyRewards](https://github.com/steefan1998/GSmod/tree/main/GSMod-DailyRewards). When replacing the old mod, GSTweaks can read each player's last claim day from the same world. Use one Daily Rewards module per instance.

Operators can check the active time zone and whether it was selected automatically with `/gstweaks daily admin timezone`. Run `/gstweaks daily admin timezone auto` to use the server's time zone, or the local machine's time zone in single-player. To set a fixed zone, use a command such as `/gstweaks daily admin timezone Europe/Amsterdam`. Existing fixed settings are kept until changed.

Server settings are stored in `config/gstweaks/daily-rewards.json`.

| Setting | Default | Effect |
| --- | --- | --- |
| `enabled` | `true` | Enables rewards. |
| `timeZone` | `auto` | Uses the host's time zone or a fixed IANA zone such as `Europe/Amsterdam` to determine the reward day. |
| `resetHour` | `0` | Local reset hour from 0 to 23. |
| `minimumOnlineMinutes` | `30` | Online playtime required each day. Set to `0` to make the reward available immediately. |
| `experience` | `30` | XP points per claim. |
| `rewards` | 2 gold ingots and 4 bread | List of objects with an `item` ID and a `count`. |

## What the converter migrates

The converter moves legacy registrations, tooltips, Mars assets and behavior, material conversion, and Thaumaturge scans and material aspects into GSTweaks. The Core and Trophy recipes, `gspack_crossmod_recipes.js`, the Thaumaturge recipe guard, and recipe unification remain in KubeJS. GSTweaks handles Tinkers melting, casting, and tool part recipes. For a new KubeJS instance, copy the pack recipes you want from `migration/kubejs/server_scripts`.

Other pack features, including quest detection, mastery, EMC, and client settings, still run in KubeJS.

## Building

Use JDK 25 and point `packMods` at the directory containing the local compile-time jars for Stellaris, Architectury, Potentials, AllTheModium, JEI, Thaumaturge, Continuum Construct, Continuum Core, and Weather Story. The build keeps those jars separate from GSTweaks.

```powershell
.\gradlew.bat build '-PpackMods=D:/path/to/GSPack/mods'
```

The build produces `build/libs/GSTweaks-0.3.1.jar`.

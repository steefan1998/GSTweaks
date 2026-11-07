# GSTweaks

![GSPack-logo](src/main/resources/gspack-logo.png)

**Auteurs:** Steef_an & ChatGPT Codex

Een NeoForge-mod voor Minecraft **26.1.2** en Java 25. GSTweaks registreert de vijf Cores, Creative Singularity, plaatsbare Trophy of Infinity, Crux-blokken en Mars-items en -blokken. De bestaande GSPack-ID’s blijven behouden.

## Optionele compatibiliteit

| Mod | Integratie |
| --- | --- |
| Stellaris | Uitbreiding van `stellaris:mars` met biomen, dungeons, dorpen, kampen, mobs en zuurstofsystemen. |
| AllTheOres | Mars-osmium en -uranium, de bijbehorende drops en optionele unificatie van dubbele ertsen. |
| AllTheModium | Mars-unobtainium met de oorspronkelijke mijnbeveiliging, XP en drop. |
| Mekanism Additions | Baby Creepers op Mars en in de research dome. Zonder deze compat bevat de dome een gewone Creeper. |
| JEI | Verbergt geconfigureerde materiaalduplicaten als de voorkeursvariant beschikbaar is. |
| Thaumaturge | Voegt materiaalaspecten voor gemeenschappelijke tags toe. De Thaumometer kan ook aspectloze modblokken registreren. |
| Tinkers’ Construct / Continuum Construct | Vult ontbrekende smeltrecepten aan en voegt 27 optionele molten materialen toe. Achttien materialen hebben eigen stats voor heads, handles en bindings, plus recepten voor gieten, repareren en recyclen. |
| FTB Library / FTB Essentials | Opent Daily Rewards via een knop in de inventariszijbalk. |
| Powah en Extended Crafting | De GSPack-recepten in KubeJS slaan individuele recepten over als een vereiste mod of een ingrediënt ontbreekt. |

De lijst met extra molten materialen staat in `src/main/resources/molten-materials.json`. De vloeistoffen worden alleen geregistreerd met Tinkers en de bijbehorende materiaalmod. De compat laat bestaande smeltrecepten voorgaan en voegt een gietrecept toe als er een passend erts en eindproduct beschikbaar is. De extra vloeistoffen zijn bedoeld voor tanks en giettafels.

De toolpartstats staan bij de [materiaaldefinities](src/main/resources/packs/tinkers_materials/data/gstweaks/tinkering/materials/stats). Zo is desh licht en snel, is boron duurzaam met een snelheidsnadeel, en levert novite relatief hoge schade met minder duurzaamheid dan titanium.

## Overstappen vanaf GSPack

1. Sluit Minecraft of stop de server. Maak een kopie van de instance en de wereld.
2. Plaats `GSTweaks-0.2.0.jar` in `mods` van de kopie. Haal een eventuele eerdere GSTweaks-jar uit die map.
3. Start client en server. GSTweaks archiveert herkende oude bestanden vóór KubeJS ze leest. De converter installeert bijgewerkte recepten en stopt bij aangepaste bestanden die handmatig moeten worden samengevoegd.

De automatische back-up staat in `gstweaks-backup/kubejs-*/kubejs`. Om terug te gaan, gebruik je de oude wereldkopie en zet je de gearchiveerde bestanden terug. Verwijder ook de nieuwe jar en de bestanden die in `created-files.json` staan.

Voor een proefrun vanuit deze projectmap kun je het volgende commando gebruiken.

```powershell
.\migration\Install-GSTweaks.ps1 -Instance 'D:\pad\naar\GSPack-kopie'
```

De mod-ID is `gstweaks`. ID’s als `gspack:tech_core`, `gspack:infinity_trophy` en `stellaris:mars` blijven hetzelfde. GSTweaks zet oude ertsen op Mars bij het laden van chunks om. De bestaande instellingen uit `config/gspack-mars.json` blijven bruikbaar.

Als een oudere eigen versie andere item- of blok-ID’s gebruikte, kun je op client en server aliassen opnemen in `config/gstweaks-id-aliases.json`. Gebruik uitsluitend oude ID’s die na de overstap vrij zijn.

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

## Unify instellen

GSTweaks maakt `config/gstweaks/unification.json` aan bij de eerste start. De volgorde in `groups[].items` bepaalt de voorkeur. GSTweaks en het KubeJS-receptscript kiezen allebei de eerste geregistreerde variant. `convertBlocks` schakelt blokconversie per materiaalgroep in. `blockReplacements` bevat expliciete blok-ID’s voor oudere werelden.

Met `enabled` schakel je Unify uit. Met `overrideOreData` regel je of GSTweaks de GSPack-wereldgeneratie en -drops voor dubbele ertsen activeert als AllTheOres aanwezig is. `hiddenItems` en `hiddenOres` bepalen welke duplicaten JEI verbergt. Gebruik dezelfde instellingen op client en server en herstart na wijzigingen.

## GSTweaks instellen

Bij de eerste start maakt GSTweaks `config/gstweaks/gstweaks.json` aan. Wijzigingen gelden na een herstart.

| Instelling | Functie |
| --- | --- |
| `autoMigrateKubeJs` | Start de KubeJS-converter bij het laden van de mod. |
| `endgameTooltips` | Toont de extra uitleg bij Cores en Trophy. |
| `migrateMarsOres` | Vervangt oude Mars-ertsen bij het laden van chunks. |
| `marsZombieSuit`, `marsRaidPillagers`, `marsVillageLifeSupport` | Regelt de afzonderlijke Mars-functies. |
| `thaumaturgeCompat` | Activeert extra scans en materiaalaspecten. |
| `tinkersOreRecipes` | Activeert de aanvullende smelt- en gietrecepten. |
| `tinkersToolMaterials` | Activeert de nieuwe toolpartmaterialen en bijbehorende recepten. |

## Daily Rewards

Open het menu via `/gstweaks daily` of de FTB-zijbalk. Iedere speler kan eenmaal per dag claimen via het menu of `/gstweaks daily claim`. Standaard wordt de claim beschikbaar na **30 minuten online speeltijd** op die dag. De reset is om **00.00 uur in Europe/Amsterdam**. Speeltijd wordt per speler in de wereld opgeslagen.

Operators kunnen de status van een online speler bekijken met `/gstweaks daily debug <speler>`. Dat commando kent geen beloning toe. De implementatie is gebaseerd op [GSMod-DailyRewards](https://github.com/steefan1998/GSmod/tree/main/GSMod-DailyRewards). Bij vervanging van de oude mod kan GSTweaks de laatst geclaimde dag uit dezelfde wereld overnemen. Gebruik één Daily Rewards-module per instance.

Met `/gstweaks daily admin timezone` ziet een operator de huidige tijdzone. Met bijvoorbeeld `/gstweaks daily admin timezone Europe/Amsterdam` wordt de tijdzone direct aangepast en opgeslagen.

De serverinstellingen staan in `config/gstweaks/daily-rewards.json`.

| Instelling | Standaard | Functie |
| --- | --- | --- |
| `enabled` | `true` | Schakelt beloningen in. |
| `timeZone` | `Europe/Amsterdam` | Tijdzone waarin het dagvenster valt. |
| `resetHour` | `0` | Lokaal resetuur tussen 0 en 23. |
| `minimumOnlineMinutes` | `30` | Vereiste online speeltijd van die dag. `0` maakt de claim direct beschikbaar. |
| `experience` | `30` | XP-punten per claim. |
| `rewards` | 2 goudstaven en 4 brood | Lijst met objecten met een `item`-ID en `count`. |

## Bereik van de converter

De converter vervangt de oude registraties, tooltips, Mars-assets en -gedrag, de Unify-conversie en de Thaumaturge-scans en materiaalaspecten. De Cores- en Trophy-recepten, `gspack_crossmod_recipes.js`, Thaumaturge-receptguard en Unify-receptvervanging blijven in KubeJS. GSTweaks beheert de Tinkers-smelt-, giet- en toolpartrecepten. Bij een nieuwe KubeJS-instance kun je de gewenste packrecepten uit `migration/kubejs/server_scripts` handmatig overzetten.

Overige packfuncties zoals questdetectie, mastery, EMC en clientinstellingen vallen buiten deze migratie en draaien voorlopig in KubeJS.

## Bouwen

Gebruik JDK 25 en wijs `packMods` naar de map met de lokale compile-time jars van Stellaris, Architectury, Potentials, AllTheModium, JEI en Thaumaturge. Deze jars worden niet in GSTweaks verpakt.

```powershell
.\gradlew.bat build '-PpackMods=D:/pad/naar/GSPack/mods'
```

De uitvoer staat in `build/libs/GSTweaks-0.2.0.jar`.

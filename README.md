# LevelZ: Unofficial NeoForge Port

Unofficial port of **[LevelZ](https://github.com/Globox1997/LevelZ)** by **Globox_Z** (Fabric 1.21.1, version 2.0.13) to **NeoForge 1.21.1**.

LevelZ adds character levels and skills (mining, melee, archery, magic, farming and so on) and locks blocks, items, mobs,
crafting recipes and dimensions behind skill requirements. Both the skill and the restriction data are plain datapack files.

## Status: early alpha

The port builds, starts on a dedicated server and reaches the title screen in the client with all skills and restrictions
loaded from the datapack (12 skills, 548 restriction entries, no datapack errors). Please report anything that behaves
differently from the original (see below).

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.200 or newer
- [Cloth Config](https://modrinth.com/mod/cloth-config) for NeoForge 15.0 or newer
- [LibZ (Unofficial NeoForge Port)](https://github.com/sanufsoiii/libz-neoforge) — the shared inventory-tab and config
  library of the Globox_Z mods

## Getting textures, models and translations

All LevelZ assets (textures, models, translations) are **All Rights Reserved** by Globox_Z. They are **not included** in
this port, and the datapack files (`data/levelz/`) are.

1. Download the original **LevelZ 2.0.13** for **Fabric 1.21.1** yourself: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/levelz/files) or [Modrinth](https://modrinth.com/mod/levelz/version/2.0.13+1.21.1).
2. Put the downloaded `.jar` into your `resourcepacks` folder. **Not** into `mods`.

On startup the port finds the jar and loads **only its client assets** (textures, models, translations) as a built-in
resource pack. Nothing is downloaded by the mod itself. Without the jar the game works, but items and the skill screen
have no textures, and a one-time notice on the title screen explains what to do.

## Building

Requires JDK 21. LibZ must be published locally first:

```bash
cd ../../libz/port && ./gradlew publishToMavenLocal
cd ../../levelz/port && ./gradlew build
```

The mod jar is in `build/libs/`.

## Reporting bugs

This is an **unofficial** port. Please **do not** report its bugs to Globox_Z or on the original issue tracker. Use this
repository instead: https://github.com/sanufsoiii/levelz-neoforge/issues

## License and credits

- LevelZ was created by **Globox_Z**: https://github.com/Globox1997/LevelZ. All gameplay design, art and names belong to them.
- This port is not affiliated with or endorsed by the original author.
- Code: **GPL-3.0-only**, same as the original code (see `LICENSE`). The port stays open source for that reason.
- Folder `_excluded/` keeps original Fabric-only code (ModMenu, Inmis backpack, Trinkets/Cosmetic Armor/Easy Anvils/
  Easy Magic/Alloygery integrations, Placeholder API) for reference; it is not compiled.

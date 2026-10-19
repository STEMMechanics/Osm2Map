# Osm2Map (Real world in Minecraft)

Osm2Map 4.0 targets **Paper 26.2**, **Java 25**, and **FastAsyncWorldEdit 2.15.3+**.
The old Spigot 1.12.2 / WorldEdit 6 APIs have been migrated. This release is not a
backwards-compatible jar for 1.12.2, and Spigot and Folia are not verified targets.

Build and run the compatibility checks with Java 25:

```sh
bash gradlew clean build
```

Install `build/libs/osm2map-4.0.0-all.jar` alongside a FAWE build supporting 26.2.
See [installation](.github/doc/INSTALL.md) and the
[compatibility report](.github/doc/COMPATIBILITY.md) for tested coverage and remaining
live-server checks.

[Download Here](https://github.com/STEMMechanics/Osm2Map/releases)

### Features
- **Custom World Generator** based on OpenStreetMap data
   - Buildings
   - Gardens
   - Water
   - Trees
   - Ground color using satellite images
- **Teleporting to Ways or POIs** by name
- **Multi World Support**
   - One different config for each World
- **Builtin Guard System** (optional)
   - Claim/Unclaim zones (buildings/gardens)
   - Build with friends in the same zone
   - Support of FAWE's (Fast WorldEdit) Mask system 
- **Customizable**
   - Change all blocks used by the Generator (ground, ways, ...)

### Doc pages
- [Installation](.github/doc/INSTALL.md)
- [Configuration](.github/doc/CONFIG.md)
- [Commands and Permissions](.github/doc/COMMANDS.md)
- [Guard Mode](.github/doc/GUARD.md)
- [Satellite Images (ground coloring)](.github/doc/TILES.md)
- [OpenStreetMap](.github/doc/OSM.md)

Example Eiffel Tower :
![alt text](./.github/images/eiffel_1.jpg "Logo Title Text 1")

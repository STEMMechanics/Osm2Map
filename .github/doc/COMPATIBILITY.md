# Minecraft 26.3 compatibility

The 26.3 update changes the Paper and FAWE build dependencies and aligns CI with
Java 25. No Java API migration was needed. Automated checks cover API
compatibility, configuration, database behavior, and packaging; successful live
generation on Paper 26.3 remains unverified.

The earlier 26.2 migration addressed configuration, database, and satellite palette
failures exposed by user-provided server logs. That coverage is retained below.

## Build targets

- Paper API `26.3.build.195-stable`, Java 25
- FAWE Core and Bukkit API `2.16.1` (provided by the installed FAWE plugin)
- Gradle 9.4.1 and Shadow 9.4.2
- H2 2.2.220 and LocationTech JTS for database geometry

[Paper project setup](https://docs.papermc.io/paper/dev/project-setup/) specifies the
26.3 API and Java 25. [FAWE 2.16.0](https://github.com/IntellectualSites/FastAsyncWorldEdit/releases/tag/2.16.0)
introduced Minecraft 26.3 support; this build pins FAWE 2.16.1. Use a FAWE Paper
distribution marked for Minecraft 26.3.

The plugin descriptor retains `api-version: "26.2"` as its minimum server API
version. This does not prevent loading on 26.3; 26.2 has not been revalidated as
part of this update.

## Changes and checks

| Area | Migration / validation |
| --- | --- |
| Server APIs | All application Java sources compile against the pinned Paper and FAWE APIs. |
| Blocks | BlockData / WorldEdit block states replace MaterialData and numeric runtime IDs. Legacy config values use LegacyMapper; new config uses modern names. |
| Generation | Current generateNoise and BiomeProvider hooks; actual world height bounds; generation buffers released after use. Live chunk generation still needs testing. |
| Schematics | Clipboard and BlockVector3 replace removed WorldEdit 6 types. Quarter turns transform directional block states and retain NBT in the clipboard. Live loading/pasting still needs testing; Bukkit chunk placement transfers block states, not block-entity NBT. |
| Guard | Current FAWE mask signatures, current material names, off-hand interaction items, and world-specific WorldEdit selections. In-game enforcement still needs testing. |
| Commands | Paper command map API replaces reflection. Help displays the version from plugin metadata, including CI build numbers. A bytecode test checks the bundled Intake jar's Bukkit, Guava, and Bungee method/field references against the resolved server libraries. This does not exercise command execution or reflection inside dependencies. |
| Particles / rules | Typed DUST particles and current GameRules replace removed constants and legacy rule names. In-game effects still need testing. |
| Database | Tests cover schema creation, geometry queries, claims/friends, reopen, POI position/rotation, generated IDs, options, and moderators. Removed obsolete H2 LOG / UNDO_LOG settings and corrected POI joins. |
| OSM | Fixture imports a road, building, named POI, and multipolygon through osm4j into H2. Old osm4j geometry crosses to LocationTech via WKT. |
| Satellite HTTP | During the earlier 26.2 migration, the configured ArcGIS endpoint returned HTTP 200, image/jpeg, 14,401 bytes for tile 0/0/0. No full terrain-coloring run performed. |
| Initialization | Invalid world configuration fails before listeners register; empty databases import automatically; generator requests validate initialization. Missing palettes are installed from the JAR, and malformed palettes fail before generation. Config and palette parser regression tests cover these inputs. |
| Packaging | Verify one Osm2Map descriptor and no bundled Bukkit, WorldEdit, FAWE, or SLF4J classes. Standalone config creation works without server jars. |
| Releases | Numbered and unnumbered JARs pass packaging and standalone config checks. Five release-script tests cover creation at the exact built commit, published-release retries, draft recovery, missing artifacts, and publication failures. Workflow syntax passes actionlint. |

Run `bash gradlew clean build`. Test results are in `build/reports/tests/test/`;
the installable artifact is `build/libs/osm2map-4.0.0-all.jar`.

With the pinned 26.3 dependencies, the full build passes all 13 tests, plugin JAR
verification, and standalone configuration generation.

See [releasing](RELEASING.md) for the merge-to-master workflow and numbered JARs.

Bungee chat APIs are still deprecated in Paper but resolve successfully; compilation
warnings are not evidence that every runtime path has been exercised. Spigot,
Folia, other Minecraft releases, and arbitrary third-party plugin combinations have
not been validated.

## Live server checks remaining

Run these checks on an isolated server before deploying to existing worlds.

1. Start an isolated Paper 26.3 server with FAWE 2.16.1 and the built Osm2Map jar; confirm
   both plugins enable and `/o2m` and `/osm2map` register.
2. Import `src/test/resources/compatibility.osm` into a fresh database with
   `forceInit: true`, `tile.active: false`, and the configured generator. Verify
   terrain, road/building placement, biome, and chunks above Y=255.
3. Exercise POI/way teleportation and dust indicators, create/regenerate/remove a
   zone from a selection in a second world, then test claim/friend/unclaim and FAWE
   edits inside and outside the permitted area (including protected bedrock).
4. Load and rotate a directional tree schematic, enable satellite coloring, and
   restart to verify persisted map/claim data.

Use copies of old worlds and databases. H2 1.x database files require an explicit
export/import migration to H2 2.x; this update does not convert those files or
validate an in-place Minecraft world upgrade. `forceInit` rebuilds the map database
and is not a preservation/migration mechanism.

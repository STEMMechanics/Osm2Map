- [Installation](INSTALL.md)
- [Configuration](CONFIG.md)
- [Commands and Permissions](COMMANDS.md)
- [Guard Mode](GUARD.md)
- [Satellite Images (ground coloring)](TILES.md)
- [OpenStreetMap](OSM.md)

# Installation

## Requirements

- Java 25
- Paper 26.3
- [FastAsyncWorldEdit](https://modrinth.com/plugin/fastasyncworldedit) 2.16.1 or newer,
  using its Paper build marked compatible with Minecraft 26.3

FAWE supplies WorldEdit; do not also install the old WorldEdit 6 jar.
See [compatibility and validation](COMPATIBILITY.md).

## Requirements (OpenStreetMap)
You need to download the map you want with the `format OSM XML`

[How to download a OSM XML map ?](OSM.md#get-a-osm-xml-map-file)

## Osm2Map

- Download the `osm2map-<version>-all.jar` asset from [GitHub releases](https://github.com/STEMMechanics/Osm2Map/releases), for example `osm2map-4.0.0-build.4-all.jar`. Copy it into `plugins/` and use that filename in the standalone config command below.
- Alternatively, build with `bash gradlew clean build` and copy `build/libs/osm2map-4.0.0-all.jar` into `plugins/`.
- From the server directory, run `java -jar plugins/osm2map-4.0.0-all.jar` to generate the default config. Existing config files are left intact.

- Edit the config file [(CONFIG.md)](CONFIG.md) in `plugins/osm2map/config/config.json`
- When satellite coloring is enabled, the bundled `groundBlocks.txt` is copied to the configured palette path if missing. Existing palettes are validated and preserved.
- Supply the configured OSM XML file. Empty databases are initialized and imported automatically. Set `forceInit` to `true` only to rebuild an existing map database, including claims and POIs; use a fresh database path when testing.
- Configure the generator below before starting Paper.
> :warning: **If you change the .osm**: Delete the world and set **forceInit** to **true** in the world's config to force reload the .osm file

#### Configure Bukkit
```yaml
# end of bukkit.yml
worlds:
  world: # replace "world" by the name of your world
    generator: osm2map
```

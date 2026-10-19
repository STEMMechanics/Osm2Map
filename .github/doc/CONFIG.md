- [Installation](INSTALL.md)
- [Configuration](CONFIG.md)
- [Commands and Permissions](COMMANDS.md)
- [Guard Mode](GUARD.md)
- [Satellite Images (ground coloring)](TILES.md)
- [OpenStreetMap](OSM.md)

# Config Documentation

Block values accept modern names and states, for example
`minecraft:oak_stairs[facing=east,half=bottom]`. Existing numeric values such as
`35:8` and legacy names such as `minecraft:planks:2` are translated through FAWE's
legacy mapper. Legacy names retain their old meaning; use an explicit state
(e.g. `minecraft:stone_slab[type=bottom]`) to disambiguate a name shared with an old
block. Invalid values raise an error instead of silently substituting air.

```json5
{
    "worlds": [
        {
            "name": "world", // Exact name of your world
            "forceInit": false, // If true : force init world Borders, world Spawn point, ... (reset automatically to false)
            "config": { // World Config
                "databaseFile": "plugins/osm2map/world-o2m.db", // All Data of o2m (different for each World) 
                "generation": { 
                    "osmFile": "plugins/osm2map/world.osm", // OSM XML Map file
                    "zoom": 1.0, // Zoom divider : 0.5 => 2x bigger
                    "height": 10, // World gen min Height
                    "elevation": { // Elevation -> see Section "Elevation"
                        "enabled": false,
                        "folder": "plugins/osm2map/paris/elev"
                    },
                    "dirtLayerSize": 5,
                    "treeSchematicsEnabled": true, // Enable Tree Generation
                    "treeSchematicsFolder": "plugins/osm2map/schematic/tree/medium", // Folder of trees schematics
                    "seed": 1 // Random Seed (not very used) 
                },
                "guard": { // GUARD Mode
                    "active": true,
                    "maxClaim": 3, // Max claim at the same time
                    "maxFriend": 3 // Max friends by claim,
                },
                "tile": { // Tile is for coloring the terrain based on satellite images
                    // see Section "Satellite Images (Tiles)"
                    "active": true,
                    "tilesFolder": "plugins/osm2map/tiles",
                    "tilesUrl": "https://services.arcgisonline.com/arcgis/rest/services/World_Imagery/MapServer/tile/{zoom}/{y}/{x}",
                    "blockColorsFile": "plugins/osm2map/groundBlocks.txt",
                    "tilesZoom": 17,
                    "offsetX": 0, // if your map and satellite images are not exactly aligned 
                    "offsetY": 0,
                    "mulR": 1.2, // Color Channel multiplier
                    "mulG": 1.5, // More Greenish for better look
                    "mulB": 1.2,
                    "blur": { // Blur on satellite images (Recommended for better results)
                        "active": true,
                        "blurSize": 3,
                        "blurStrength": 0.5
                    }
                },
                "blocks": { // Generation Blocks Settings
                    "wayBlock": "minecraft:coal_block",
                    "wayBorderBlock": "minecraft:smooth_stone_slab",
                    "wayTrackBlock": "minecraft:oak_planks",
                    "buildingsBorderBlock": "minecraft:cobblestone",
                    "buildingsCornerBlock": "minecraft:stone_bricks",
                    "buildingsFillBlock": "minecraft:oak_planks",
                    "gardensCornerBlock": "minecraft:oak_fence",
                    "gardensBorderBlock": "minecraft:oak_leaves[persistent=true]",
                    "groundTopBlock": "minecraft:grass_block",
                    "groundMiddleBlock": "minecraft:dirt",
                    "groundBottomBlock": "minecraft:bedrock",
                    "waterWaysBlock": "minecraft:water"
                }
            }
        }//,
        // { name: "world2", ... }
    ]
}
```
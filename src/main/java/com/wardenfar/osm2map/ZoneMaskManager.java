package com.wardenfar.osm2map;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.entity.Player;
import com.fastasyncworldedit.core.regions.FaweMask;
import com.fastasyncworldedit.core.regions.FaweMaskManager;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.wardenfar.osm2map.config.World;
import com.wardenfar.osm2map.map.DBMapData;
import com.wardenfar.osm2map.map.entity.Vector2d;
import com.wardenfar.osm2map.map.entity.Vector2i;

public class ZoneMaskManager extends FaweMaskManager {

    Osm2map plugin;

    public ZoneMaskManager(Osm2map plugin) {
        super("Osm2Map");
        this.plugin = plugin;
    }

    @Override
    public FaweMask getMask(Player fp, MaskType type, boolean isWhitelist) {
        String player = fp.getName();
        String worldName = fp.getWorld().getName();

        World pluginWorld = plugin.getWorlds().getWorld(worldName);
        if(pluginWorld != null && pluginWorld.config.guard.active){
            DBMapData mapData = plugin.getMapData(worldName);

            Vector2d tl = mapData.getTopLeftPos();
            Vector2d br = mapData.getBottomRightPos();

            final int minY = fp.getWorld().getMinY();
            final int maxY = fp.getWorld().getMaxY();
            Region maskedRegion = new AbstractRegion(fp.getWorld()) {
                @Override
                public BlockVector3 getMinimumPoint() { return BlockVector3.at(tl.x, minY, tl.y); }
                @Override
                public BlockVector3 getMaximumPoint() { return BlockVector3.at(br.x, maxY, br.y); }
                @Override
                public void expand(BlockVector3... changes) throws RegionOperationException { throw new RegionOperationException("Fixed claim mask"); }
                @Override
                public void contract(BlockVector3... changes) throws RegionOperationException { throw new RegionOperationException("Fixed claim mask"); }
                @Override
                public boolean contains(BlockVector3 position) { return contains(position.x(), position.y(), position.z()); }
                @Override
                public boolean contains(int x, int y, int z) {
                    if(y >= minY && y <= maxY && contains(x, z)){
                        int dirtLayerSize = pluginWorld.config.generation.dirtLayerSize;
                        int elev = plugin.getElevByBlockXZ(worldName, x, z);
                        int bedrockTopY = Math.max(0, elev - dirtLayerSize - 2) + 1;
                        return y > bedrockTopY;
                    }
                    return false;
                }

                @Override
                public boolean contains(int x, int z) {
                    return mapData.isPlayerCanChangeAt(player, new Vector2i(x, z), true);
                }
            };

            return new FaweMask(maskedRegion) {
                @Override
                public boolean isValid(Player player, MaskType type) {
                    //return mapData.isPlayerCanChangeAt(player.getName(), new Vector2i(player.getLocation().x, player.getLocation().z), true);
                    return player.getWorld().getName().equals(worldName) && pluginWorld.config.guard.active;
                }
            };
        }

        return null;
    }
}

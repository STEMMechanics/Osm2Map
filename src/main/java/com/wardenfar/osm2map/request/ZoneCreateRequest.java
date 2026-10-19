package com.wardenfar.osm2map.request;

import com.wardenfar.osm2map.terrainGeneration.GenerationClipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.locationtech.jts.geom.Coordinate;
import com.wardenfar.osm2map.Osm2map;
import com.wardenfar.osm2map.map.entity.JtsZone;
import com.wardenfar.osm2map.map.entity.Vector2i;
import com.wardenfar.osm2map.pluginInterface.SchemInterface;
import com.wardenfar.osm2map.terrainGeneration.OsmElevGenerator;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.data.BlockData;

import java.util.List;

import static com.wardenfar.osm2map.Util.builder;
import static com.wardenfar.osm2map.Util.sendMessage;

public class ZoneCreateRequest extends Request {

    String worldName;
    JtsZone.Type type;
    boolean doGen;
    List<Vector2i> points;

    public ZoneCreateRequest(String worldName, JtsZone.Type type, boolean doGen, List<Vector2i> points) {
        this.worldName = worldName;
        this.type = type;
        this.doGen = doGen;
        this.points = points;
    }

    @Override
    public TextComponent[] getMessage() {
        return new TextComponent[]{builder("Do you really want to create this zone ?", ChatColor.DARK_PURPLE)};
    }

    @Override
    public void confirm(Osm2map plugin) {
        sendMessage(src, "Confirmed zone creation", ChatColor.LIGHT_PURPLE);
        JtsZone zone = plugin.getMapData(worldName).addZone(type, points);
        sendMessage(src, "Zone added !", ChatColor.GREEN);

        if (doGen) {
            sendMessage(src, "Generation started ...", ChatColor.GREEN);
            zone.computePixels(null, null);
            World world = ((Player) src).getWorld();

            OsmElevGenerator populator = plugin.getOsmElevGenerator();
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            for (Coordinate c : zone.geometry.getBoundary().getCoordinates()) {
                minX = (int) Math.min(minX, c.x);
                maxX = (int) Math.max(maxX, c.x);
                minY = (int) Math.min(minY, c.y);
                maxY = (int) Math.max(maxY, c.y);
            }
            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            BlockVector3 origin = BlockVector3.at(minX, world.getMinHeight(), minY);
            GenerationClipboard clip = new GenerationClipboard(BlockVector3.at(width, world.getMaxHeight() - world.getMinHeight(), height), origin);
            populator.populate(worldName, clip, new SchemInterface());
            for (int x = 0; x < clip.getWidth(); x++) {
                for (int z = 0; z < clip.getLength(); z++) {
                    for (int y = 0; y < clip.getHeight(); y++) {
                        BaseBlock block = clip.getBlock(BlockVector3.at(x, y, z));
                        if (block != null) {
                            Object blockObject = plugin.getInterface().fromBlock(block);
                            setBlock(world, (int) (origin.x() + x), (int) (origin.y() + y), (int) (origin.z() + z), (BlockData) blockObject);
                        }
                    }
                }
            }
            sendMessage(src, "Generation finished ...", ChatColor.GREEN);
        }
    }

    @Override
    public void cancel(Osm2map plugin) {
        sendMessage(src, "Canceled zone creation", ChatColor.LIGHT_PURPLE);
    }

    private BlockData getBlock(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z).getBlockData();
    }

    private void setBlock(World world, int x, int y, int z, BlockData block) {
        Block b = world.getBlockAt(x,y,z);
        b.setBlockData(block, false);
    }
}

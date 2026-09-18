package com.wardenfar.osm2map.pluginInterface;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.block.data.BlockData;

public class BukkitInterface extends Interface {
    private final SchemInterface schematics = new SchemInterface();

    @Override
    public BlockData getBlock(int id) { return fromBlock(schematics.getBlock(id)); }

    @Override
    public BlockData getBlock(int id, int variant) { return fromBlock(schematics.getBlock(id, variant)); }

    @Override
    public BlockData getBlock(String id) { return fromBlock(schematics.getBlock(id)); }

    @Override
    public BlockData fromBlock(BaseBlock block) { return BukkitAdapter.adapt(block.toImmutableState()); }

    @Override
    public boolean isAir(Object block) {
        return block instanceof BlockData && ((BlockData) block).getMaterial().isAir();
    }

    @Override
    public Class<?> getBlockClass() { return BlockData.class; }
}

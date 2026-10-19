package com.wardenfar.osm2map.pluginInterface;

import com.sk89q.worldedit.world.block.BaseBlock;

public abstract class Interface {
    public abstract Object getBlock(int id);
    public abstract Object getBlock(int id, int variant);
    public abstract Object getBlock(String id);
    public abstract Object fromBlock(BaseBlock block);
    public abstract boolean isAir(Object block);
    public abstract Class<?> getBlockClass();
}

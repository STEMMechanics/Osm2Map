package com.wardenfar.osm2map.terrainGeneration;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BaseBlock;

/** A local-coordinate generation buffer with a separate world origin. */
public final class GenerationClipboard {
    private final BlockArrayClipboard clipboard;
    private final BlockVector3 size;
    private final BlockVector3 origin;
    private final BlockVector3 offset;

    public GenerationClipboard(BlockVector3 size, BlockVector3 origin) {
        this(size, origin, BlockVector3.ZERO);
    }

    public GenerationClipboard(BlockVector3 size, BlockVector3 origin, BlockVector3 offset) {
        if (size.x() <= 0 || size.y() <= 0 || size.z() <= 0) {
            throw new IllegalArgumentException("Clipboard dimensions must be positive");
        }
        this.size = size;
        this.origin = origin;
        this.offset = offset;
        clipboard = new BlockArrayClipboard(new CuboidRegion(BlockVector3.ZERO, size.subtract(1, 1, 1)));
    }

    public BlockVector3 getSize() { return size; }
    public BlockVector3 getOrigin() { return origin; }
    public BlockVector3 getOffset() { return offset; }
    public int getWidth() { return size.x(); }
    public int getHeight() { return size.y(); }
    public int getLength() { return size.z(); }
    public BaseBlock getBlock(BlockVector3 position) { return clipboard.getFullBlock(position); }
    public void setBlock(BlockVector3 position, BaseBlock block) { clipboard.setBlock(position, block); }
}

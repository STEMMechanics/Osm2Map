package com.wardenfar.osm2map.terrainGeneration;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BaseBlock;

public class SuperSchematic {
    private final Clipboard clipboard;

    public SuperSchematic(Clipboard clipboard) { this.clipboard = clipboard; }
    public int getWidth() { return clipboard.getDimensions().x(); }
    public int getHeight() { return clipboard.getDimensions().y(); }
    public int getLength() { return clipboard.getDimensions().z(); }
    public Clipboard getClipboard() { return clipboard; }
    public BlockVector3 getSize() { return clipboard.getDimensions(); }

    public BaseBlock getBlock(BlockVector3 localPosition) {
        return clipboard.getFullBlock(clipboard.getMinimumPoint().add(localPosition));
    }

    public SuperSchematic rotate(float angle) {
        if (!Float.isFinite(angle) || angle % 90 != 0) {
            throw new IllegalArgumentException("Schematic rotation must be a multiple of 90 degrees");
        }
        int turns = Math.floorMod((int) (angle / 90), 4);
        int width = getWidth(), length = getLength();
        int newWidth = turns % 2 == 0 ? width : length;
        int newLength = turns % 2 == 0 ? length : width;
        BlockArrayClipboard rotated = new BlockArrayClipboard(new CuboidRegion(BlockVector3.ZERO,
                BlockVector3.at(newWidth - 1, getHeight() - 1, newLength - 1)));
        // Preserve the old clockwise coordinate rotation, including directional block states and NBT.
        AffineTransform transform = new AffineTransform().rotateY(-turns * 90);
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                int newX = switch (turns) { case 1 -> length - 1 - z; case 2 -> width - 1 - x; case 3 -> z; default -> x; };
                int newZ = switch (turns) { case 1 -> x; case 2 -> length - 1 - z; case 3 -> width - 1 - x; default -> z; };
                for (int y = 0; y < getHeight(); y++) {
                    BaseBlock block = getBlock(BlockVector3.at(x, y, z));
                    rotated.setBlock(BlockVector3.at(newX, y, newZ), BlockTransformExtent.transform(block, transform));
                }
            }
        }
        return new SuperSchematic(rotated);
    }
}

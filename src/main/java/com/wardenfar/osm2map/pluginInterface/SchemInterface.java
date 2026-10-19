package com.wardenfar.osm2map.pluginInterface;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import com.wardenfar.osm2map.Util;

public class SchemInterface extends Interface {
    public SchemInterface() { Util.createMap(); }

    @Override
    public BaseBlock getBlock(int id) { return getBlock(id, 0); }

    @Override
    public BaseBlock getBlock(int id, int variant) {
        BlockState state = LegacyMapper.getInstance().getBlockFromLegacy(id, variant);
        if (state == null) throw new IllegalArgumentException("Unknown legacy block: " + id + ":" + variant);
        return state.toBaseBlock();
    }

    @Override
    public BaseBlock getBlock(String id) {
        // Retain the meanings of pre-flattening config and ground colour entries.
        Integer legacy = Util.getBlockId(id);
        if (legacy != null) return getBlock(legacy, Util.getVariantId(id));
        if (id.matches("[0-9]+(:[0-9]+)?")) {
            String[] parts = id.split(":");
            return getBlock(Integer.parseInt(parts[0]), parts.length == 2 ? Integer.parseInt(parts[1]) : 0);
        }
        ParserContext context = new ParserContext();
        context.setRestricted(false);
        context.setTryLegacy(false);
        try {
            return WorldEdit.getInstance().getBlockFactory().parseFromInput(id, context);
        } catch (InputParseException e) {
            throw new IllegalArgumentException("Invalid block configuration: " + id, e);
        }
    }

    @Override
    public BaseBlock fromBlock(BaseBlock block) { return block; }

    @Override
    public boolean isAir(Object block) {
        return block instanceof BaseBlock && ((BaseBlock) block).getBlockType().getMaterial().isAir();
    }

    @Override
    public Class<?> getBlockClass() { return BaseBlock.class; }
}

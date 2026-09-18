package com.wardenfar.osm2map.config;

import com.wardenfar.osm2map.Util;
import com.wardenfar.osm2map.pluginInterface.Interface;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class BlockColorsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final Interface blocks = new Interface() {
        public Object getBlock(int id) { return id; }
        public Object getBlock(int id, int variant) { return id; }
        public Object getBlock(String id) { return id.equals("invalid") ? null : id; }
        public Object fromBlock(BaseBlock block) { return block; }
        public boolean isAir(Object block) { return false; }
        public Class<?> getBlockClass() { return String.class; }
    };

    @Test public void bundledPaletteParsesIncludingComments() throws Exception {
        var file = temporary.newFile().toPath();
        try (var input = getClass().getResourceAsStream("/groundBlocks.txt")) {
            Files.write(file, input.readAllBytes());
        }
        assertEquals(20, Util.getBlocksColors(file.toString(), blocks).size());
    }

    @Test public void malformedAndEmptyPalettesAreRejected() throws Exception {
        for (String content : new String[]{"# comment\n", "stone 82,104,", "stone 256,0,0", "stone", "invalid 1,2,3"}) {
            var file = temporary.newFile().toPath();
            Files.writeString(file, content);
            try {
                Util.getBlocksColors(file.toString(), blocks);
                fail("Accepted " + content);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains(file.toString()));
            }
        }
    }

    @Test public void missingPaletteIsRejected() {
        try {
            Util.getBlocksColors(temporary.getRoot() + "/missing.txt", blocks);
            fail("Accepted missing palette");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Cannot read block colours"));
        }
    }
}

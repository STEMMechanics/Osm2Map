package com.wardenfar.osm2map.config;

import com.wardenfar.osm2map.Util;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class WorldsConfigTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void rejectsInvalidConfigWithoutOverwritingIt() throws Exception {
        for (String json : new String[] {
                "{\"worlds\":[{\"name\":\"julia-creek\"},]}",
                "{\"worlds\":[null]}", "null", "", "{broken",
                "{\"worlds\":null}", "{\"worlds\":[{\"config\":null}]}",
                "{\"worlds\":[{\"name\":null}]}",
                "{\"worlds\":[{\"name\":\"same\"},{\"name\":\"same\"}]}"
        }) {
            File file = temporary.newFile();
            Files.writeString(file.toPath(), json);
            try {
                Util.readOrCreateWorldsJson(file.toString());
                fail("Accepted invalid configuration: " + json);
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains(file.toString()));
            }
            assertEquals(json, Files.readString(file.toPath()));
        }
    }

    @Test public void acceptsCommentsAndResetsForceInit() throws Exception {
        File file = temporary.newFile();
        Files.writeString(file.toPath(),
                "{\"worlds\":[{ // world settings\n\"name\":\"julia-creek\",\"forceInit\":true}]}");
        Worlds loaded = Util.readOrCreateWorldsJson(file.toString());
        assertTrue(loaded.getWorld("julia-creek").forceInitSave);
        assertFalse(loaded.getWorld("julia-creek").forceInit);
        assertFalse(Util.readOrCreateWorldsJson(file.toString()).getWorld("julia-creek").forceInitSave);
    }

    @Test public void createsDefaultConfiguration() throws Exception {
        File file = new File(temporary.getRoot(), "nested/config.json");
        assertNotNull(Util.readOrCreateWorldsJson(file.toString()).getWorld("world"));
        assertTrue(file.isFile());
    }
}

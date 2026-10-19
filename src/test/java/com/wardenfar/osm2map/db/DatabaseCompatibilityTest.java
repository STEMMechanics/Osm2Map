package com.wardenfar.osm2map.db;

import com.flowpowered.math.vector.Vector3d;
import com.wardenfar.osm2map.Util;
import com.wardenfar.osm2map.map.entity.JtsZone;
import com.wardenfar.osm2map.map.entity.Poi;
import com.wardenfar.osm2map.map.entity.Vector2i;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import static org.junit.Assert.*;

public class DatabaseCompatibilityTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Database database;
    private File file;

    @Before public void open() throws Exception {
        file = new File(temporary.newFolder(), "map");
        database = Database.create(file, true);
    }
    @After public void close() { if (database != null) database.close(); }

    @Test public void emptyDatabaseCanBeImportedAndReopened() throws Exception {
        database.close();
        file = new File(temporary.newFolder(), "new-map.db");
        database = Database.create(file, false);
        assertTrue(database.isEmpty());
        database.close();
        database = Database.create(file, false);
        assertTrue(database.isEmpty());
        database.initializeSchema();
        assertFalse(database.isEmpty());
        try (var input = getClass().getResourceAsStream("/compatibility.osm")) {
            com.wardenfar.osm2map.map.DBMapData.readFromOsmXML(null, database,
                    java.util.logging.Logger.getLogger("compatibility-test"), input, 10, 1);
        }
        database.close();
        database = Database.create(file, false);
        assertFalse(database.isEmpty());
        assertNotNull(com.wardenfar.osm2map.map.DBMapData.readFromDB(null, database));
        assertEquals("test street", database.getWay(10).name);
    }

    @Test public void missingOptionsFailsClearly() {
        try {
            database.getOptions();
            fail("Expected missing options error");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("options"));
        }
    }

    @Test public void geometryClaimsAndFriendsSurviveReopening() {
        JtsZone zone = new JtsZone(42, JtsZone.Type.BUILDING, null, null,
                Util.geometryFromRectangle(0, 0, 10, 10));
        database.insertZone(zone);
        database.setOwnerOfZone(42, "owner");
        database.addFriend(42, "friend");
        assertTrue(database.getZonesFromPos(new Vector2i(5, 5)).contains(42L));
        assertFalse(database.getZonesFromPos(new Vector2i(20, 20)).contains(42L));
        assertTrue(database.getZonesByPosAndPlayer("friend", new Vector2i(5, 5), true, false).contains(42L));
        database.close();
        database = Database.create(file, false);
        assertEquals("owner", database.getZone(42).owner);
        assertEquals(100, database.getZone(42).geometry.getArea(), 0.001);
        assertTrue(database.getFriendsByZone(42).contains("friend"));
    }

    @Test public void poiPositionAndRotationRoundTrip() {
        Poi original = new Poi(-1, "Test POI", new Vector3d(1, 12, 3), new Vector3d(90, 45, 0));
        database.insertPoi(original);
        Poi loaded = database.getOnePoiByName(original.name);
        assertNotNull(loaded);
        assertEquals(original.pos, loaded.pos);
        assertEquals(original.rot, loaded.rot);
        database.removePoi(loaded.id);
        assertNull(database.getOnePoiByName(original.name));
    }

    @Test public void osmXmlImportsWaysBuildingsAndMultipolygons() throws Exception {
        try (var input = getClass().getResourceAsStream("/compatibility.osm")) {
            var map = com.wardenfar.osm2map.map.DBMapData.readFromOsmXML(null, database,
                    java.util.logging.Logger.getLogger("compatibility-test"), input, 10, 1);
            assertNotNull(map);
            assertEquals("test street", database.getWay(10).name);
            assertEquals(JtsZone.Type.BUILDING, database.getZone(20).type);
            assertTrue(database.getZone(20).geometry.getArea() > 0);
            assertNotNull(database.getOnePoiByName("Test Building"));
            assertEquals(2, database.getZonesFromChunk(new Vector2i(-1000, -1000), new Vector2i(1000, 1000)).size());
        }
    }

    @Test public void optionsAndGeneratedZoneIdsRoundTrip() {
        database.setOptions(new OptionsRecord(10, 1.5, -27, 153, -28, 154));
        assertEquals(1.5, database.getOptions().zoom, 0.0);
        JtsZone zone = new JtsZone(-1, JtsZone.Type.GARDEN, null, null,
                Util.geometryFromRectangle(20, 20, 5, 5));
        database.insertZone(zone);
        assertEquals(1, database.getZonesFromPos(new Vector2i(22, 22)).size());
        database.addModo("moderator");
        assertTrue(database.getModos().contains("moderator"));
        database.removeModo("moderator");
        assertFalse(database.getModos().contains("moderator"));
    }
}

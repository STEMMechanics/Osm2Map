package com.wardenfar.osm2map.db;

import com.flowpowered.math.vector.Vector3d;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import com.wardenfar.osm2map.Util;
import com.wardenfar.osm2map.map.entity.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;

public class Database {

    private File file;

    private Connection conn;
    private Statement statement;

    private Database(File file) {
        this.file = file;
    }

    private void init() {
        try {
            DriverManager.registerDriver(org.h2.Driver.load());
            conn = DriverManager.getConnection(getUrl(false));
            statement = conn.createStatement();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot open database " + file, e);
        }
    }

    public boolean isEmpty() {
        try (ResultSet tables = conn.getMetaData().getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
            return !tables.next();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot inspect database " + file, e);
        }
    }

    private String getUrl(boolean ifExist) {
        return "jdbc:h2:" + file.getAbsolutePath() + ";USER=o2m;PASSWORD=o2m" + (ifExist ? ";IFEXISTS=TRUE" : "");
    }

    public void setOptions(OptionsRecord options) {
        executeSql("DELETE FROM options");
        try {
            PreparedStatement stat = conn.prepareStatement("INSERT INTO options (id,height,zoom,TL_LAT,TL_LON,BR_LAT,BR_LON) VALUES (1,?,?,?,?,?,?)");
            stat.setInt(1, options.height);
            stat.setDouble(2, options.zoom);
            stat.setDouble(3, options.tlLat);
            stat.setDouble(4, options.tlLon);
            stat.setDouble(5, options.brLat);
            stat.setDouble(6, options.brLon);
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public OptionsRecord getOptions() {
        try (ResultSet rs = statement.executeQuery("SELECT * FROM options WHERE id = 1")) {
            if (rs.next()) {
                return new OptionsRecord(
                        rs.getInt("height"),
                        rs.getDouble("zoom"),
                        rs.getDouble("TL_LAT"),
                        rs.getDouble("TL_LON"),
                        rs.getDouble("BR_LAT"),
                        rs.getDouble("BR_LON"));
            } else {
                throw new IllegalStateException("No options row");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read map options from " + file + "; database is not initialized or is incompatible", e);
        }
    }

    public JtsWay getWay(long id) {
        try {
            PreparedStatement stat = conn.prepareStatement("SELECT * FROM way WHERE id = ?");
            stat.setLong(1, id);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                Object o = rs.getObject("linestring");
                return new JtsWay(id, rs.getString("name"), rs.getString("type"), rs.getFloat("thickness"), (Geometry) o);
            } else {
                throw new IllegalStateException("No way found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JtsZone getZone(long id) {
        try {
            PreparedStatement stat = conn.prepareStatement("SELECT * FROM zone WHERE id = ?");
            stat.setLong(1, id);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                Geometry geometry = (Geometry) rs.getObject("poly");
                List<String> friends = getFriendsByZone(id);
                return new JtsZone(id, JtsZone.Type.values()[rs.getByte("type")], rs.getString("owner"), friends, geometry);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JtsMapElement getMapElement(long id) {
        try {
            PreparedStatement stat = conn.prepareStatement("SELECT * FROM mapelement WHERE id = ?");
            stat.setLong(1, id);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                Geometry geometry = (Geometry) rs.getObject("geometry");
                return new JtsMapElement(id, rs.getString("type"), geometry);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getFriendsByZone(long zoneId) {
        List<String> friends = new ArrayList<>();
        try {
            PreparedStatement stat2 = conn.prepareStatement("SELECT friend.player FROM friend WHERE zone_id = ?");
            stat2.setLong(1, zoneId);
            ResultSet rs2 = stat2.executeQuery();
            while (rs2.next()) {
                friends.add(rs2.getString("player"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friends;
    }

    public void addFriend(long zoneId, String friend) {
        try {
            PreparedStatement stat = conn.prepareStatement("INSERT INTO friend (zone_id,player) VALUES (?,?)");
            stat.setLong(1, zoneId);
            stat.setString(2, friend);
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeFriend(long zoneId, String friend) {
        try {
            PreparedStatement stat = conn.prepareStatement("DELETE FROM friend WHERE zone_id = ? AND player = ?");
            stat.setLong(1, zoneId);
            stat.setString(2, friend);
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertWay(JtsWay w) {
        try {
            PreparedStatement stat = conn.prepareStatement("INSERT INTO way (id,name,type,thickness,linestring) VALUES (?,?,?,?,CAST(? AS GEOMETRY))");
            stat.setLong(1, w.id);
            stat.setString(2, w.name);
            stat.setString(3, w.type);
            stat.setFloat(4, w.thickness);
            stat.setString(5, Util.toViWkt(w.geometry));
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void prepareBigImport() {
        executeSql("SET LOCK_MODE 0");
        executeSql("SET CACHE_SIZE 65536");
    }

    public void resetBigImport() {
        executeSql("SET LOCK_MODE 1");
    }

    public static Database create(File file, boolean init) {
        Database db = new Database(file);
        db.init();
        if (init) {
            db.initializeSchema();
        }
        return db;
    }

    public void initializeSchema() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("schema.sql"))))) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append('\n');
            }
            statement.execute("DROP ALL OBJECTS");
            statement.execute(sql.toString());
        } catch (Exception e) {
            close();
            throw new IllegalStateException("Cannot initialize database " + file, e);
        }
    }

    public void insertZones(List<JtsZone> zones) {
        for (JtsZone z : zones) {
            insertZone(z);
        }
    }

    public void insertZone(JtsZone z) {
        try {
            if (z.id != -1) {
                PreparedStatement stat = conn.prepareStatement("INSERT INTO zone (id,poly,type) VALUES (?,?,?)");
                stat.setLong(1, z.id);
                stat.setString(2, Util.toViWkt(z.geometry));
                stat.setByte(3, (byte) z.type.ordinal());
                stat.execute();
                for (String friend : z.friends) {
                    addFriend(z.id, friend);
                }
            } else {
                PreparedStatement stat = conn.prepareStatement("INSERT INTO zone (poly,type) VALUES (?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
                stat.setString(1, Util.toViWkt(z.geometry));
                stat.setByte(2, (byte) z.type.ordinal());
                stat.executeUpdate();
                ResultSet rs = stat.getGeneratedKeys();
                if (rs.next()) {
                    long id = rs.getLong(1);
                    z.id = id;
                    for (String friend : z.friends) {
                        addFriend(id, friend);
                    }
                } else {
                    throw new IllegalStateException("zone id not returned");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertMapElement(JtsMapElement me) {
        try {
            PreparedStatement stat = conn.prepareStatement("INSERT INTO mapelement (type,geometry) VALUES (?,CAST(? AS GEOMETRY))");
            stat.setString(1, me.type);
            stat.setString(2, Util.toViWkt(me.geometry));
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<Long> getZonesByPosAndPlayer(String player, Vector2i pos, boolean byFriend, boolean addPublic) {
        String ownerCondition;
        if (addPublic) {
            ownerCondition = "zone.owner = '" + player + "' OR zone.owner = '*'";
        } else {
            ownerCondition = "zone.owner = '" + player + "'";
        }

        String sql;
        if (byFriend) {
            sql = "SELECT zone.id FROM zone LEFT JOIN friend ON zone.id = friend.zone_id WHERE (zone.poly && CAST(? AS GEOMETRY)) AND intersectsRect(zone.poly,?,?,1,1) AND (friend.player = '" + player + "' OR " + ownerCondition + ")";
        } else {
            sql = "SELECT zone.id FROM zone WHERE (zone.poly && CAST(? AS GEOMETRY)) AND intersectsRect(zone.poly,?,?,1,1) AND " + ownerCondition;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            String wkt = Util.toViWkt(Util.geometryFactory.createPoint(new Coordinate(pos.x, pos.y)));
            ps.setString(1, wkt);
            ps.setInt(2, pos.x);
            ps.setInt(3, pos.y);
            ResultSet rs = ps.executeQuery();
            Set<Long> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Set<Long> getZonesFromChunk(Vector2i min, Vector2i max) {
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(min.x, min.y),
                new Coordinate(max.x, min.y),
                new Coordinate(max.x, max.y),
                new Coordinate(min.x, max.y),
                new Coordinate(min.x, min.y),
        };
        Polygon rect = Util.geometryFactory.createPolygon(coords);
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM zone WHERE (zone.poly && CAST(? AS GEOMETRY))");
            String wkt = Util.toViWkt(rect);
            ps.setString(1, wkt);
            ResultSet rs = ps.executeQuery();
            Set<Long> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Set<Long> getWaysFromChunk(Vector2i min, Vector2i max) {
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(min.x, min.y),
                new Coordinate(max.x, min.y),
                new Coordinate(max.x, max.y),
                new Coordinate(min.x, max.y),
                new Coordinate(min.x, min.y),
        };
        Polygon rect = Util.geometryFactory.createPolygon(coords);
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM way WHERE isWithinDistance(CAST(? AS GEOMETRY),way.linestring,way.thickness)");
            ps.setString(1, Util.toViWkt(rect));
            ResultSet rs = ps.executeQuery();
            Set<Long> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Set<Long> getMapElementsFromChunk(Vector2i min, Vector2i max) {
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(min.x, min.y),
                new Coordinate(max.x, min.y),
                new Coordinate(max.x, max.y),
                new Coordinate(min.x, max.y),
                new Coordinate(min.x, min.y),
        };
        Polygon rect = Util.geometryFactory.createPolygon(coords);
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM mapelement WHERE (geometry && CAST(? AS GEOMETRY))");
            ps.setString(1, Util.toViWkt(rect));
            ResultSet rs = ps.executeQuery();
            Set<Long> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setOwnerOfZone(long zoneId, String owner) {
        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE zone SET zone.owner = ? WHERE zone.id = ?");
            ps.setString(1, owner);
            ps.setLong(2, zoneId);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<Long> getZonesFromPos(Vector2i pos) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT zone.id FROM zone WHERE (zone.poly && CAST(? AS GEOMETRY)) AND intersectsRect(zone.poly,?,?,1,1)");
            String wkt = Util.toViWkt(Util.geometryFactory.createPoint(new Coordinate(pos.x, pos.y)));
            ps.setString(1, wkt);
            ps.setInt(2, pos.x);
            ps.setInt(3, pos.y);
            ResultSet rs = ps.executeQuery();
            Set<Long> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getOwnerByZone(Long zoneId) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT zone.owner FROM zone WHERE zone.id = ?");
            ps.setLong(1, zoneId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("owner");
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Set<Long> getZonesOwnedByPlayer(String player) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT zone.id FROM zone WHERE zone.owner = ?");
            ps.setString(1, player);
            ResultSet rs = ps.executeQuery();
            Set<Long> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Poi getPoi(long id) {
        String sql = "SELECT p.id, p.name, pos.x AS px, pos.y AS py, pos.z AS pz, "
                + "rot.x AS rx, rot.y AS ry, rot.z AS rz, p.rotation "
                + "FROM poi p JOIN vector3d pos ON p.position = pos.id "
                + "LEFT JOIN vector3d rot ON p.rotation = rot.id WHERE p.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Vector3d pos = new Vector3d(rs.getDouble("px"), rs.getDouble("py"), rs.getDouble("pz"));
                Vector3d rot = rs.getObject("rotation") == null ? null
                        : new Vector3d(rs.getDouble("rx"), rs.getDouble("ry"), rs.getDouble("rz"));
                return new Poi(rs.getLong("id"), rs.getString("name"), pos, rot);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read POI " + id, e);
        }
    }

    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }

    public Poi getOnePoiByName(String name) {
        try {
            PreparedStatement stat = conn.prepareStatement("SELECT poi.id FROM poi WHERE poi.name LIKE ?");
            stat.setString(1, name);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                return getPoi(rs.getLong("id"));
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Poi> getAllPoisByName(String name) {
        try {
            PreparedStatement stat = conn.prepareStatement("SELECT poi.id FROM poi WHERE poi.name_lower LIKE ?");
            stat.setString(1, name.toLowerCase());
            ResultSet rs = stat.executeQuery();
            List<Poi> pois = new ArrayList<>();
            while (rs.next()) {
                pois.add(getPoi(rs.getLong("id")));
            }
            return pois;
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Long> getPois() {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT poi.id FROM poi");
            ResultSet rs = ps.executeQuery();
            List<Long> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void executeSql(String sql) {
        try {
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<Long> getZonesByOwnerOrFriend(String player, boolean addPublic) {
        String ownerCondition;
        if (addPublic) {
            ownerCondition = "zone.owner = ? OR zone.owner = '*'";
        } else {
            ownerCondition = "zone.owner = ?";
        }

        try {
            PreparedStatement ps = conn.prepareStatement("SELECT zone.id FROM zone LEFT JOIN friend ON zone.id = friend.zone_id WHERE friend.player = ? OR " + ownerCondition);
            ps.setString(1, player);
            ps.setString(2, player);
            ResultSet rs = ps.executeQuery();
            Set<Long> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Set<Long> getWaysFromNameLower(String toLowerCase) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT way.id FROM way WHERE way.name LIKE ?");
            ps.setString(1, "%" + toLowerCase + "%");
            ResultSet rs = ps.executeQuery();
            Set<Long> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void insertPoi(Poi poi) {
        try {
            long posId, rotId = -1;

            PreparedStatement statPos = conn.prepareStatement("INSERT INTO vector3d (x,y,z) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
            statPos.setDouble(1, poi.pos.getX());
            statPos.setDouble(2, poi.pos.getY());
            statPos.setDouble(3, poi.pos.getZ());
            statPos.executeUpdate();
            ResultSet posIdRs = statPos.getGeneratedKeys();
            if (posIdRs.next()) {
                posId = posIdRs.getLong(1);
            } else {
                throw new IllegalStateException("posId is null");
            }

            if (poi.rot != null) {
                PreparedStatement statRot = conn.prepareStatement("INSERT INTO vector3d (x,y,z) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
                statRot.setDouble(1, poi.rot.getX());
                statRot.setDouble(2, poi.rot.getY());
                statRot.setDouble(3, poi.rot.getZ());
                statRot.executeUpdate();
                ResultSet rotIdRs = statRot.getGeneratedKeys();
                if (rotIdRs.next()) {
                    rotId = rotIdRs.getLong(1);
                } else {
                    throw new IllegalStateException("posId is null");
                }
            }

            if (rotId != -1) {
                PreparedStatement stat = conn.prepareStatement("INSERT INTO poi (name,position,rotation) VALUES (?,?,?)");
                stat.setString(1, poi.name);
                stat.setLong(2, posId);
                stat.setLong(3, rotId);
                stat.execute();
            } else {
                PreparedStatement stat = conn.prepareStatement("INSERT INTO poi (name,position) VALUES (?,?)");
                stat.setString(1, poi.name);
                stat.setLong(2, posId);
                stat.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePoi(long id) {
        try {
            PreparedStatement stat = conn.prepareStatement("DELETE FROM poi WHERE poi.id = ?");
            stat.setLong(1, id);
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeAllFriend(long zoneId) {
        try {
            PreparedStatement stat = conn.prepareStatement("DELETE FROM friend WHERE zone_id = ?");
            stat.setLong(1, zoneId);
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<Long> getOwnedZones() {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT zone.id FROM zone WHERE zone.owner IS NOT NULL");
            ResultSet rs = ps.executeQuery();
            Set<Long> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
            return ids;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void removeZone(long zoneId) {
        try {
            PreparedStatement stat = conn.prepareStatement("DELETE FROM zone WHERE id = ?");
            stat.setLong(1, zoneId);
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        removeAllFriend(zoneId);
    }

    public List<String> getModos() {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT modo.player FROM modo");
            ResultSet rs = ps.executeQuery();
            List<String> modos = new ArrayList<>();
            while (rs.next()) {
                modos.add(rs.getString("player"));
            }
            return modos;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addModo(String player) {
        try {
            PreparedStatement stat = conn.prepareStatement("INSERT INTO modo (player) VALUES (?)");
            stat.setString(1, player);
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeModo(String player) {
        try {
            PreparedStatement stat = conn.prepareStatement("DELETE FROM modo WHERE player = ?");
            stat.setString(1, player);
            stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

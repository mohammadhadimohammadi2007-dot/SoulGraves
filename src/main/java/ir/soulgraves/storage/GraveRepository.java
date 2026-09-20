package ir.soulgraves.storage;

import ir.soulgraves.grave.Grave;
import ir.soulgraves.util.ItemSerialization;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public final class GraveRepository {

    private final Database db;

    public GraveRepository(Database db) { this.db = db; }

    public void insert(Grave g) throws Exception {
        String sql = """
            INSERT INTO graves(id, owner_uuid, world, x, y, z, death_time, creation_playtime,
                death_cause, killer, grave_order, marker_type, marker_id, items_data)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        Connection c = db.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, g.getId().toString());
            ps.setString(2, g.getOwnerUuid().toString());
            ps.setString(3, g.getWorldName());
            ps.setDouble(4, g.getX());
            ps.setDouble(5, g.getY());
            ps.setDouble(6, g.getZ());
            ps.setLong(7, g.getDeathTimeMs());
            ps.setLong(8, g.getCreationPlaytimeTicks());
            ps.setString(9, g.getDeathCause());
            ps.setString(10, g.getKiller());
            ps.setInt(11, g.getGraveOrder());
            ps.setString(12, g.getMarkerType().name());
            ps.setString(13, g.getMarkerId());
            ps.setBytes(14, ItemSerialization.toBytes(g.getItems()));
            ps.executeUpdate();
        }
    }

    public void updateItems(UUID graveId, List<ItemStack> items) throws Exception {
        String sql = "UPDATE graves SET items_data = ? WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setBytes(1, ItemSerialization.toBytes(items));
            ps.setString(2, graveId.toString());
            ps.executeUpdate();
        }
    }

    public void delete(UUID graveId) throws Exception {
        try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM graves WHERE id = ?")) {
            ps.setString(1, graveId.toString());
            ps.executeUpdate();
        }
    }

    public List<Grave> findByOwner(UUID owner) throws Exception {
        String sql = "SELECT * FROM graves WHERE owner_uuid = ? ORDER BY death_time ASC";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<Grave> list = new ArrayList<>();
                while (rs.next()) list.add(fromRow(rs));
                return list;
            }
        }
    }

    public List<Grave> findAll() throws Exception {
        List<Grave> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement("SELECT * FROM graves")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(fromRow(rs));
            }
        }
        return list;
    }

    private Grave fromRow(ResultSet rs) throws Exception {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID owner = UUID.fromString(rs.getString("owner_uuid"));
        return new Grave(
                id, owner,
                rs.getString("world"),
                rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                rs.getLong("death_time"),
                rs.getLong("creation_playtime"),
                rs.getString("death_cause"),
                rs.getString("killer"),
                rs.getInt("grave_order"),
                Grave.MarkerType.valueOf(rs.getString("marker_type")),
                rs.getString("marker_id"),
                ItemSerialization.fromBytes(rs.getBytes("items_data"))
        );
    }
}

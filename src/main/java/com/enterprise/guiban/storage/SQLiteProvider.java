package com.enterprise.guiban.storage;

import com.enterprise.guiban.GUIBAN;
import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class SQLiteProvider implements StorageProvider {

    private final GUIBAN plugin;
    private Connection connection;

    public SQLiteProvider(GUIBAN plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            File db = new File(plugin.getDataFolder(), "guiban.db");
            if (!db.exists()) {
                plugin.getDataFolder().mkdirs();
                db.createNewFile();
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());

            connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS punishments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid TEXT," +
                "type TEXT," +
                "reason TEXT," +
                "start_time LONG," +
                "expiry_time LONG," +
                "punisher TEXT);"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addPunishment(Punishment punishment) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO punishments (uuid, type, reason, start_time, expiry_time, punisher) VALUES (?, ?, ?, ?, ?, ?)"
            );
            ps.setString(1, punishment.getUuid().toString());
            ps.setString(2, punishment.getType().name());
            ps.setString(3, punishment.getReason());
            ps.setLong(4, punishment.getStartTime());
            ps.setLong(5, punishment.getExpiryTime());
            ps.setString(6, punishment.getPunisher());
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removePunishment(UUID uuid, PunishmentType type) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM punishments WHERE uuid = ? AND type = ?"
            );
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Punishment getActivePunishment(UUID uuid, PunishmentType type) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM punishments WHERE uuid = ? AND type = ? ORDER BY start_time DESC LIMIT 1"
            );
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Punishment p = new Punishment(
                    uuid,
                    type,
                    rs.getString("reason"),
                    rs.getLong("start_time"),
                    rs.getLong("expiry_time"),
                    rs.getString("punisher")
                );
                rs.close();
                ps.close();
                if (!p.isExpired()) return p;
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Punishment> getHistory(UUID uuid) {
        List<Punishment> history = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM punishments WHERE uuid = ? ORDER BY start_time DESC"
            );
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(new Punishment(
                    uuid,
                    PunishmentType.valueOf(rs.getString("type")),
                    rs.getString("reason"),
                    rs.getLong("start_time"),
                    rs.getLong("expiry_time"),
                    rs.getString("punisher")
                ));
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return history;
    }
}

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
            connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS ip_bans (ip TEXT PRIMARY KEY, reason TEXT, start_time LONG, expiry_time LONG, punisher TEXT);"
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    @Override
    public synchronized void addPunishment(Punishment punishment) {
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
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    @Override
    public synchronized void removePunishment(UUID uuid, PunishmentType type) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM punishments WHERE uuid = ? AND type = ?"
            );
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    @Override
    public synchronized Punishment getActivePunishment(UUID uuid, PunishmentType type) {
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
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized List<Punishment> getHistory(UUID uuid) {
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
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
        return history;
    }

    @Override
    public synchronized List<Punishment> getActivePunishments(PunishmentType type) {
        List<Punishment> out = new ArrayList<>();
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM punishments WHERE type = ? ORDER BY start_time DESC"
            );
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                if (seen.contains(uuid)) continue;
                Punishment p = new Punishment(uuid, type, rs.getString("reason"),
                    rs.getLong("start_time"), rs.getLong("expiry_time"), rs.getString("punisher"));
                if (!p.isExpired()) {
                    seen.add(uuid);
                    out.add(p);
                }
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
        return out;
    }

    @Override
    public synchronized boolean isIpBanned(String ip) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT expiry_time FROM ip_bans WHERE ip = ?");
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long exp = rs.getLong("expiry_time");
                rs.close();
                ps.close();
                return exp == -1 || System.currentTimeMillis() < exp;
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public synchronized void addIpBan(String ip, String reason, long expiry, String punisher) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO ip_bans (ip, reason, start_time, expiry_time, punisher) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, ip);
            ps.setString(2, reason);
            ps.setLong(3, System.currentTimeMillis());
            ps.setLong(4, expiry);
            ps.setString(5, punisher);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    @Override
    public synchronized void removeIpBan(String ip) {
        try {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM ip_bans WHERE ip = ?");
            ps.setString(1, ip);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    @Override
    public synchronized int getWarnCount(UUID uuid) {
        int count = 0;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM punishments WHERE uuid = ? AND type = 'WARN'");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Punishment p = new Punishment(uuid, PunishmentType.WARN, rs.getString("reason"),
                    rs.getLong("start_time"), rs.getLong("expiry_time"), rs.getString("punisher"));
                if (!p.isExpired()) count++;
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
        return count;
    }

    @Override
    public synchronized void cleanupExpired() {
        try {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM punishments WHERE type != 'KICK' AND expiry_time != -1 AND expiry_time < ?");
            ps.setLong(1, System.currentTimeMillis());
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }
}

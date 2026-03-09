package com.enterprise.guiban.storage;

import com.enterprise.guiban.GUIBAN;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MySQLProvider implements StorageProvider {

    private final GUIBAN plugin;
    private HikariDataSource dataSource;

    public MySQLProvider(GUIBAN plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        HikariConfig config = new HikariConfig();
        String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        String database = plugin.getConfig().getString("storage.mysql.database", "guiban");
        String username = plugin.getConfig().getString("storage.mysql.username", "root");
        String password = plugin.getConfig().getString("storage.mysql.password", "");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS punishments (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "uuid VARCHAR(36)," +
                "type VARCHAR(10)," +
                "reason TEXT," +
                "start_time BIGINT," +
                "expiry_time BIGINT," +
                "punisher VARCHAR(64));"
            );
            stmt.execute("CREATE TABLE IF NOT EXISTS ip_bans (ip VARCHAR(45) PRIMARY KEY, reason TEXT, start_time BIGINT, expiry_time BIGINT, punisher VARCHAR(64));");
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    @Override
    public void addPunishment(Punishment punishment) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO punishments (uuid, type, reason, start_time, expiry_time, punisher) VALUES (?, ?, ?, ?, ?, ?)"
             )) {
            ps.setString(1, punishment.getUuid().toString());
            ps.setString(2, punishment.getType().name());
            ps.setString(3, punishment.getReason());
            ps.setLong(4, punishment.getStartTime());
            ps.setLong(5, punishment.getExpiryTime());
            ps.setString(6, punishment.getPunisher());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    @Override
    public void removePunishment(UUID uuid, PunishmentType type) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM punishments WHERE uuid = ? AND type = ?"
             )) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    @Override
    public Punishment getActivePunishment(UUID uuid, PunishmentType type) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM punishments WHERE uuid = ? AND type = ? ORDER BY start_time DESC LIMIT 1"
             )) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Punishment p = new Punishment(
                        uuid,
                        type,
                        rs.getString("reason"),
                        rs.getLong("start_time"),
                        rs.getLong("expiry_time"),
                        rs.getString("punisher")
                    );
                    if (!p.isExpired()) return p;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Punishment> getHistory(UUID uuid) {
        List<Punishment> history = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM punishments WHERE uuid = ? ORDER BY start_time DESC"
             )) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
        return history;
    }

    @Override
    public List<Punishment> getActivePunishments(PunishmentType type) {
        List<Punishment> out = new ArrayList<>();
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM punishments WHERE type = ? ORDER BY start_time DESC"
             )) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
        return out;
    }

    @Override
    public boolean isIpBanned(String ip) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT expiry_time FROM ip_bans WHERE ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long exp = rs.getLong("expiry_time");
                    return exp == -1 || System.currentTimeMillis() < exp;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void addIpBan(String ip, String reason, long expiry, String punisher) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM ip_bans WHERE ip = ?")) {
                del.setString(1, ip);
                del.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO ip_bans (ip, reason, start_time, expiry_time, punisher) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, ip);
                ps.setString(2, reason);
                ps.setLong(3, System.currentTimeMillis());
                ps.setLong(4, expiry);
                ps.setString(5, punisher);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    @Override
    public void removeIpBan(String ip) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM ip_bans WHERE ip = ?")) {
            ps.setString(1, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    @Override
    public int getWarnCount(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM punishments WHERE uuid = ? AND type = 'WARN'")) {
            ps.setString(1, uuid.toString());
            int count = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Punishment p = new Punishment(uuid, PunishmentType.WARN, rs.getString("reason"),
                        rs.getLong("start_time"), rs.getLong("expiry_time"), rs.getString("punisher"));
                    if (!p.isExpired()) count++;
                }
            }
            return count;
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void cleanupExpired() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM punishments WHERE type != 'KICK' AND expiry_time != -1 AND expiry_time < ?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }
}

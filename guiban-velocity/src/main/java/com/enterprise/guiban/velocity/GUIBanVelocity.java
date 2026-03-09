package com.enterprise.guiban.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;
import java.util.UUID;

@Plugin(id = "guiban", name = "GUIBan", version = "2.0", authors = {"Mathew Pittard"},
        description = "Enforces GUIBan bans at the proxy using shared MySQL.")
public class GUIBanVelocity {

    private final Logger logger;
    private final Path dataDirectory;
    private volatile BanChecker banChecker;

    @Inject
    public GUIBanVelocity(Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Files.createDirectories(dataDirectory);
            banChecker = new BanChecker(loadConfig());
            logger.info("GUIBan Velocity enabled. Bans will be enforced at the proxy.");
        } catch (Exception e) {
            logger.error("GUIBan Velocity failed to start: " + e.getMessage());
            banChecker = null;
        }
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        if (banChecker == null) return;

        UUID uuid = event.getUniqueId();
        if (uuid == null) return; // Older clients; let backend handle

        BanChecker.BanInfo ban = banChecker.getActiveBan(uuid);
        if (ban != null) {
            Component message = LegacyComponentSerializer.legacySection().deserialize(
                    "§cYou are banned!\n§7Reason: §f" + ban.reason + "\n§7Expires: §f" + ban.expiresText);
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(message));
        }
    }

    private Config loadConfig() throws IOException {
        Path configPath = dataDirectory.resolve("config.properties");
        if (!Files.exists(configPath)) {
            String defaultConfig = "# GUIBan Velocity - use same MySQL as your Paper GUIBan\n" +
                    "mysql.host=localhost\n" +
                    "mysql.port=3306\n" +
                    "mysql.database=guiban\n" +
                    "mysql.username=root\n" +
                    "mysql.password=\n";
            Files.writeString(configPath, defaultConfig);
            logger.info("Created default config at " + configPath);
        }

        Properties p = new Properties();
        try (var in = Files.newInputStream(configPath)) {
            p.load(in);
        }
        return new Config(
                p.getProperty("mysql.host", "localhost"),
                Integer.parseInt(p.getProperty("mysql.port", "3306")),
                p.getProperty("mysql.database", "guiban"),
                p.getProperty("mysql.username", "root"),
                p.getProperty("mysql.password", "")
        );
    }

    private static final class Config {
        final String host, database, username, password;
        final int port;

        Config(String host, int port, String database, String username, String password) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
        }
    }

    private static final class BanChecker {
        static final class BanInfo {
            final String reason;
            final String expiresText;

            BanInfo(String reason, String expiresText) {
                this.reason = reason;
                this.expiresText = expiresText;
            }
        }

        private final Config config;
        private volatile boolean closed;

        BanChecker(Config config) throws SQLException {
            this.config = config;
            ensureTable();
        }

        private Connection getConnection() throws SQLException {
            String url = "jdbc:mysql://" + config.host + ":" + config.port + "/" + config.database;
            return DriverManager.getConnection(url, config.username, config.password);
        }

        private void ensureTable() throws SQLException {
            String sql = "CREATE TABLE IF NOT EXISTS punishments (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "uuid VARCHAR(36)," +
                    "type VARCHAR(10)," +
                    "reason TEXT," +
                    "start_time BIGINT," +
                    "expiry_time BIGINT," +
                    "punisher VARCHAR(64));";
            try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
                st.execute(sql);
            }
        }

        BanInfo getActiveBan(UUID uuid) {
            if (closed) return null;
            String sql = "SELECT reason, expiry_time FROM punishments WHERE uuid = ? AND type = 'BAN' ORDER BY start_time DESC LIMIT 1";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String reason = rs.getString("reason");
                        long expiry = rs.getLong("expiry_time");
                        if (expiry != -1 && System.currentTimeMillis() > expiry) return null;
                        String expiresText = expiry == -1 ? "Permanent" : "Expires later";
                        return new BanInfo(reason != null ? reason : "No reason", expiresText);
                    }
                }
            } catch (SQLException e) {
                return null;
            }
            return null;
        }
    }
}

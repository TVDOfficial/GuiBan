package com.enterprise.guiban.storage;

import com.enterprise.guiban.GUIBAN;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class YamlProvider implements StorageProvider {

    private final GUIBAN plugin;
    private File file;
    private FileConfiguration config;

    public YamlProvider(GUIBAN plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            file = new File(plugin.getDataFolder(), "data.yml");
            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            }
            config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Storage error: " + e.getMessage());
        }
    }

    private void save() {
        try { config.save(file); } catch (Exception ignored) {}
    }

    @Override
    public synchronized void addPunishment(Punishment p) {
        String path = "punishments." + p.getUuid() + "." + p.getStartTime();
        config.set(path + ".type", p.getType().name());
        config.set(path + ".reason", p.getReason());
        config.set(path + ".expiry", p.getExpiryTime());
        config.set(path + ".punisher", p.getPunisher());
        save();
    }

    @Override
    public synchronized void removePunishment(UUID uuid, PunishmentType type) {
        ConfigurationSection section = config.getConfigurationSection("punishments." + uuid);
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            if (section.getString(key + ".type").equals(type.name())) {
                config.set("punishments." + uuid + "." + key, null);
            }
        }
        save();
    }

    @Override
    public synchronized Punishment getActivePunishment(UUID uuid, PunishmentType type) {
        ConfigurationSection section = config.getConfigurationSection("punishments." + uuid);
        if (section == null) return null;
        
        Punishment latest = null;
        for (String key : section.getKeys(false)) {
            if (section.getString(key + ".type").equals(type.name())) {
                Punishment p = new Punishment(
                    uuid,
                    type,
                    section.getString(key + ".reason"),
                    Long.parseLong(key),
                    section.getLong(key + ".expiry"),
                    section.getString(key + ".punisher")
                );
                if (!p.isExpired()) {
                    if (latest == null || p.getStartTime() > latest.getStartTime()) {
                        latest = p;
                    }
                }
            }
        }
        return latest;
    }

    @Override
    public synchronized List<Punishment> getHistory(UUID uuid) {
        List<Punishment> history = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("punishments." + uuid);
        if (section == null) return history;

        java.util.List<String> keys = new java.util.ArrayList<>(section.getKeys(false));
        keys.sort((a, b) -> Long.compare(Long.parseLong(b), Long.parseLong(a)));
        for (String key : keys) {
            history.add(new Punishment(
                uuid,
                PunishmentType.valueOf(section.getString(key + ".type")),
                section.getString(key + ".reason"),
                Long.parseLong(key),
                section.getLong(key + ".expiry"),
                section.getString(key + ".punisher")
            ));
        }
        return history;
    }

    @Override
    public synchronized List<Punishment> getActivePunishments(PunishmentType type) {
        List<Punishment> out = new ArrayList<>();
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        ConfigurationSection root = config.getConfigurationSection("punishments");
        if (root == null) return out;
        for (String uuidStr : root.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (Exception e) { continue; }
            if (seen.contains(uuid)) continue;
            Punishment active = getActivePunishment(uuid, type);
            if (active != null) {
                seen.add(uuid);
                out.add(active);
            }
        }
        return out;
    }

    @Override
    public synchronized boolean isIpBanned(String ip) {
        String path = "ip_bans." + ip.replace(".", "_");
        if (!config.contains(path)) return false;
        long exp = config.getLong(path + ".expiry_time", -1);
        return exp == -1 || System.currentTimeMillis() < exp;
    }

    @Override
    public synchronized void addIpBan(String ip, String reason, long expiry, String punisher) {
        String path = "ip_bans." + ip.replace(".", "_");
        config.set(path + ".reason", reason);
        config.set(path + ".start_time", System.currentTimeMillis());
        config.set(path + ".expiry_time", expiry);
        config.set(path + ".punisher", punisher);
        save();
    }

    @Override
    public synchronized void removeIpBan(String ip) {
        config.set("ip_bans." + ip.replace(".", "_"), null);
        save();
    }

    @Override
    public synchronized int getWarnCount(UUID uuid) {
        int count = 0;
        ConfigurationSection section = config.getConfigurationSection("punishments." + uuid);
        if (section == null) return 0;
        for (String key : section.getKeys(false)) {
            if ("WARN".equals(section.getString(key + ".type"))) {
                long exp = section.getLong(key + ".expiry", -1);
                if (exp == -1 || System.currentTimeMillis() < exp) count++;
            }
        }
        return count;
    }

    @Override
    public synchronized void cleanupExpired() {
        long now = System.currentTimeMillis();
        ConfigurationSection root = config.getConfigurationSection("punishments");
        if (root == null) return;
        for (String uuidStr : new ArrayList<>(root.getKeys(false))) {
            ConfigurationSection section = root.getConfigurationSection(uuidStr);
            if (section == null) continue;
            for (String key : new ArrayList<>(section.getKeys(false))) {
                if ("KICK".equals(section.getString(key + ".type"))) continue;
                long exp = section.getLong(key + ".expiry", -1);
                if (exp != -1 && now > exp) config.set("punishments." + uuidStr + "." + key, null);
            }
        }
        save();
    }
}

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
            e.printStackTrace();
        }
    }

    private void save() {
        try { config.save(file); } catch (Exception ignored) {}
    }

    @Override
    public void addPunishment(Punishment p) {
        String path = "punishments." + p.getUuid() + "." + p.getStartTime();
        config.set(path + ".type", p.getType().name());
        config.set(path + ".reason", p.getReason());
        config.set(path + ".expiry", p.getExpiryTime());
        config.set(path + ".punisher", p.getPunisher());
        save();
    }

    @Override
    public void removePunishment(UUID uuid, PunishmentType type) {
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
    public Punishment getActivePunishment(UUID uuid, PunishmentType type) {
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
    public List<Punishment> getHistory(UUID uuid) {
        List<Punishment> history = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("punishments." + uuid);
        if (section == null) return history;

        for (String key : section.getKeys(false)) {
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
}

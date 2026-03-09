package com.enterprise.guiban.utils;

import com.enterprise.guiban.GUIBAN;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class MessageHelper {

    private static GUIBAN plugin;
    private static FileConfiguration messages;

    private MessageHelper() {}

    public static void init(GUIBAN p) {
        plugin = p;
        load();
    }

    public static void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public static String get(String key) {
        return color(messages.getString(key, "§7" + key));
    }

    public static String get(String key, String... replacements) {
        String s = get(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            s = s.replace(replacements[i], replacements[i + 1]);
        }
        return s;
    }

    public static String prefix() {
        return get("prefix");
    }

    private static String color(String s) {
        return s == null ? "" : s.replace("&", "§");
    }
}

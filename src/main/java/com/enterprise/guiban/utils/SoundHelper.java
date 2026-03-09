package com.enterprise.guiban.utils;

import com.enterprise.guiban.GUIBAN;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundHelper {

    private SoundHelper() {}

    public static void play(GUIBAN plugin, Player player, String configKey) {
        String name = plugin.getConfig().getString("sounds." + configKey, "");
        if (name == null || name.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(name);
            player.playSound(player.getLocation(), sound, 0.5f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }
}

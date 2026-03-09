package com.enterprise.guiban.placeholders;

import com.enterprise.guiban.GUIBAN;
import com.enterprise.guiban.storage.Punishment;
import com.enterprise.guiban.storage.PunishmentType;
import com.enterprise.guiban.utils.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class GUIBanPlaceholders extends PlaceholderExpansion {

    private final GUIBAN plugin;

    public GUIBanPlaceholders(GUIBAN plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "guiban";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Mathew Pittard";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        String[] parts = params.split("_");
        if (parts.length < 2) return "";
        String type = parts[0].toUpperCase();
        String what = parts[1].toLowerCase();
        PunishmentType pt;
        try {
            pt = PunishmentType.valueOf(type);
        } catch (Exception e) {
            return "";
        }
        Punishment p = plugin.getStorage().getActivePunishment(player.getUniqueId(), pt);
        if (p == null) return "none";
        return switch (what) {
            case "reason" -> p.getReason();
            case "duration" -> TimeUtil.formatDuration(p.getExpiryTime());
            case "punisher" -> p.getPunisher();
            default -> "";
        };
    }
}

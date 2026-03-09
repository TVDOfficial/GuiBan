package com.enterprise.guiban.listener;

import com.enterprise.guiban.GUIBAN;
import com.enterprise.guiban.storage.Punishment;
import com.enterprise.guiban.storage.PunishmentType;
import com.enterprise.guiban.storage.StorageProvider;
import com.enterprise.guiban.utils.MessageHelper;
import com.enterprise.guiban.utils.TimeUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final StorageProvider storage;
    private final GUIBAN plugin;

    public JoinListener(StorageProvider storage, GUIBAN plugin) {
        this.storage = storage;
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String ip = event.getPlayer().getAddress() != null ? event.getPlayer().getAddress().getAddress().getHostAddress() : null;
        if (ip != null && storage.isIpBanned(ip)) {
            event.getPlayer().kickPlayer(MessageHelper.get("ban-message", "{reason}", "IP Banned", "{duration}", "Permanent", "{appeal}", plugin.getConfig().getString("ban-appeal-url", "")));
            return;
        }
        Punishment ban = storage.getActivePunishment(event.getPlayer().getUniqueId(), PunishmentType.BAN);
        if (ban != null) {
            String appeal = plugin.getConfig().getString("ban-appeal-url", "");
            String msg = MessageHelper.get("ban-message")
                .replace("{reason}", ban.getReason())
                .replace("{duration}", TimeUtil.formatDuration(ban.getExpiryTime()))
                .replace("{appeal}", appeal.isEmpty() ? "N/A" : appeal);
            event.getPlayer().kickPlayer(msg);
        }
    }
}

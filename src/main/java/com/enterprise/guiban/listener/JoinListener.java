package com.enterprise.guiban.listener;

import com.enterprise.guiban.GUIBAN;
import com.enterprise.guiban.storage.StorageProvider;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final StorageProvider storage;

    public JoinListener(StorageProvider storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        com.enterprise.guiban.storage.Punishment ban = storage.getActivePunishment(event.getPlayer().getUniqueId(), com.enterprise.guiban.storage.PunishmentType.BAN);
        if (ban != null) {
            String message = "§cYou are banned!\n§7Reason: §f" + ban.getReason() + "\n§7Expires: §f" + com.enterprise.guiban.utils.TimeUtil.formatDuration(ban.getExpiryTime());
            event.getPlayer().kickPlayer(message);
        }
    }
}

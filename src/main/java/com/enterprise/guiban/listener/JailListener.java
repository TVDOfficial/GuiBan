package com.enterprise.guiban.listener;

import com.enterprise.guiban.storage.Punishment;
import com.enterprise.guiban.storage.PunishmentType;
import com.enterprise.guiban.storage.StorageProvider;
import com.enterprise.guiban.utils.TimeUtil;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class JailListener implements Listener {

    private final StorageProvider storage;

    public JailListener(StorageProvider storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Punishment jail = storage.getActivePunishment(event.getPlayer().getUniqueId(), PunishmentType.JAIL);
        if (jail != null) {
            event.setCancelled(true);
            // Only send message occasionally to avoid spam
            if (System.currentTimeMillis() % 100 == 0) {
                event.getPlayer().sendMessage("§cYou are jailed!");
                event.getPlayer().sendMessage("§7Expires: §f" + TimeUtil.formatDuration(jail.getExpiryTime()));
            }
        }
    }
}

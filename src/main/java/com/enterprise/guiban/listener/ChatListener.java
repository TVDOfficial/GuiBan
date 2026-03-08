package com.enterprise.guiban.listener;

import com.enterprise.guiban.storage.StorageProvider;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final StorageProvider storage;

    public ChatListener(StorageProvider storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        com.enterprise.guiban.storage.Punishment mute = storage.getActivePunishment(event.getPlayer().getUniqueId(), com.enterprise.guiban.storage.PunishmentType.MUTE);
        if (mute != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou are muted!");
            event.getPlayer().sendMessage("§7Reason: §f" + mute.getReason());
            event.getPlayer().sendMessage("§7Expires: §f" + com.enterprise.guiban.utils.TimeUtil.formatDuration(mute.getExpiryTime()));
        }
    }
}
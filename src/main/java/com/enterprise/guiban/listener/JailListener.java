package com.enterprise.guiban.listener;

import com.enterprise.guiban.GUIBAN;
import com.enterprise.guiban.storage.Punishment;
import com.enterprise.guiban.storage.PunishmentType;
import com.enterprise.guiban.storage.StorageProvider;
import com.enterprise.guiban.utils.MessageHelper;
import com.enterprise.guiban.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JailListener implements Listener {

    private final StorageProvider storage;
    private final GUIBAN plugin;
    private final Map<UUID, Long> lastJailMessage = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000;

    public JailListener(StorageProvider storage, GUIBAN plugin) {
        this.storage = storage;
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        Punishment jail = storage.getActivePunishment(player.getUniqueId(), PunishmentType.JAIL);
        if (jail != null) {
            event.setCancelled(true);
            if (plugin.getConfig().getBoolean("jail.use", false)) {
                Location loc = getJailLocation();
                if (loc != null) player.teleport(loc);
            }
            long now = System.currentTimeMillis();
            if (now - lastJailMessage.getOrDefault(player.getUniqueId(), 0L) > MESSAGE_COOLDOWN_MS) {
                lastJailMessage.put(player.getUniqueId(), now);
                player.sendMessage(MessageHelper.get("jailed"));
                player.sendMessage(MessageHelper.get("jailed-expires").replace("{duration}", TimeUtil.formatDuration(jail.getExpiryTime())));
            }
        }
    }

    private Location getJailLocation() {
        if (!plugin.getConfig().getBoolean("jail.use", false)) return null;
        World w = Bukkit.getWorld(plugin.getConfig().getString("jail.world", "world"));
        if (w == null) return null;
        double x = plugin.getConfig().getDouble("jail.x", 0);
        double y = plugin.getConfig().getDouble("jail.y", 64);
        double z = plugin.getConfig().getDouble("jail.z", 0);
        return new Location(w, x, y, z);
    }
}

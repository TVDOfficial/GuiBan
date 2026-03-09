package com.enterprise.guiban.listener;

import com.enterprise.guiban.GUIBAN;
import com.enterprise.guiban.storage.AsyncStorageHelper;
import com.enterprise.guiban.storage.Punishment;
import com.enterprise.guiban.storage.PunishmentType;
import com.enterprise.guiban.ui.ReasonPending;
import com.enterprise.guiban.utils.PermissionHelper;
import com.enterprise.guiban.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ReasonInputListener implements Listener {

    private final GUIBAN plugin;

    public ReasonInputListener(GUIBAN plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGuiHandler().isSearchPending(player)) {
            event.setCancelled(true);
            String raw = event.getMessage().trim();
            final String filterFinal = raw.equalsIgnoreCase("clear") ? null : raw;
            plugin.getGuiHandler().clearSearchPending(player);
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getGuiHandler().openMainMenu(player, 0, filterFinal);
            });
            return;
        }
        ReasonPending pending = plugin.getReasonPendingByPlayer().remove(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        String reason = event.getMessage().trim();
        if (reason.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(PermissionHelper.PREFIX() + "§7Cancelled."));
            return;
        }
        if (reason.isEmpty()) reason = "No reason provided";

        PunishmentType type = pending.getType();
        UUID targetUuid = pending.getTargetUuid();
        long expiry = pending.getExpiryTime();
        String reasonFinal = reason;

        Bukkit.getScheduler().runTask(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
            String name = target.getName() != null ? target.getName() : targetUuid.toString();

            if (type == PunishmentType.KICK) {
                if (target.isOnline()) {
                    target.getPlayer().kickPlayer("§cKicked by §f" + player.getName() + "\n§7Reason: §f" + reasonFinal);
                    player.sendMessage(PermissionHelper.PREFIX() + "§f" + name + " §7has been kicked.");
                } else {
                    player.sendMessage("§cPlayer is not online.");
                }
                return;
            }

            Punishment p = new Punishment(targetUuid, type, reasonFinal, System.currentTimeMillis(), expiry, player.getName());
            String duration = TimeUtil.formatDuration(expiry);
            AsyncStorageHelper.addPunishmentAsync(plugin, p, () -> {
                player.sendMessage(PermissionHelper.PREFIX() + "§f" + name + " §7has been " + type.name().toLowerCase() + "ed.");
                player.sendMessage("§7Reason: §f" + reasonFinal);
                player.sendMessage("§7Duration: §f" + duration);

                if (type == PunishmentType.BAN && target.isOnline()) {
                    target.getPlayer().kickPlayer("§cYou are banned!\n§7Reason: §f" + reasonFinal + "\n§7Expires: §f" + duration);
                } else if (target.isOnline()) {
                    target.getPlayer().sendMessage("§cYou have been " + type.name().toLowerCase() + "ed for §f" + duration);
                    target.getPlayer().sendMessage("§7Reason: §f" + reasonFinal);
                }
            });
        });
    }
}

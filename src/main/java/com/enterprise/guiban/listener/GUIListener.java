package com.enterprise.guiban.listener;

import com.enterprise.guiban.GUIBAN;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GUIListener implements Listener {

    private final GUIBAN plugin;

    public GUIListener(GUIBAN plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§8GUIBan") && !title.startsWith("§8Manage:")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (title.contains("Player Selection")) {
            if (clicked.getType() == Material.PLAYER_HEAD) {
                UUID targetUuid = plugin.getGuiHandler().getPlayerUuidFromHead(clicked);
                if (targetUuid != null) {
                    Player target = Bukkit.getPlayer(targetUuid);
                    if (target != null && target.isOnline()) {
                        plugin.getGuiHandler().openModerationMenu(player, target);
                    }
                }
            }
        } else if (title.startsWith("§8Manage:")) {
            UUID targetUuid = plugin.getGuiHandler().getModerationTarget(player.getUniqueId());
            if (targetUuid == null) return;

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
            String targetName = target.getName() != null ? target.getName() : targetUuid.toString();

            Material type = clicked.getType();
            switch (type) {
                case RED_CONCRETE:
                    if (player.hasPermission("guiban.ban")) player.performCommand("guiban ban " + targetName + " 1h GUI Punishment");
                    break;
                case ORANGE_CONCRETE:
                    if (player.hasPermission("guiban.mute")) player.performCommand("guiban mute " + targetName + " 1h GUI Punishment");
                    break;
                case YELLOW_CONCRETE:
                    if (player.hasPermission("guiban.jail")) player.performCommand("guiban jail " + targetName + " 1h GUI Punishment");
                    break;
                case IRON_DOOR:
                    if (player.hasPermission("guiban.kick")) player.performCommand("guiban kick " + targetName + " GUI Punishment");
                    break;
                default:
                    return;
            }
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith("§8Manage:")) {
            if (event.getPlayer() instanceof Player) {
                plugin.getGuiHandler().clearModerationTarget(((Player) event.getPlayer()).getUniqueId());
            }
        }
    }
}

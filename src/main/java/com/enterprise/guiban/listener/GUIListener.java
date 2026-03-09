package com.enterprise.guiban.listener;

import com.enterprise.guiban.GUIBAN;
import com.enterprise.guiban.storage.AsyncStorageHelper;
import com.enterprise.guiban.storage.PunishmentType;
import com.enterprise.guiban.ui.ReasonPending;
import com.enterprise.guiban.utils.PermissionHelper;
import com.enterprise.guiban.utils.TimeUtil;
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
        if (!title.startsWith("§8GUIBan") && !title.startsWith("§8Manage:") && !title.startsWith("§8Select duration:") && !title.startsWith("§8Active ") && !title.startsWith("§8Punishments"))
            return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String action = plugin.getGuiHandler().getAction(clicked);

        if (title.contains("Player Selection")) {
            handlePlayerListClick(player, clicked, action);
            return;
        }
        if (title.startsWith("§8Manage:")) {
            handleModerationClick(player, clicked, action);
            return;
        }
        if (title.startsWith("§8Select duration:")) {
            handleTimeMenuClick(player, clicked, action);
            return;
        }
        if (title.startsWith("§8Active ")) {
            handleViewPunishmentsClick(player, clicked, action);
            return;
        }
        if (title.startsWith("§8Punishments")) {
            handlePunishmentsSectionClick(player, action);
        }
    }

    private void handlePunishmentsSectionClick(Player player, String action) {
        if (action == null) return;
        if ("punishments_back".equals(action)) {
            plugin.getGuiHandler().openMainMenu(player);
            return;
        }
        if ("view_bans".equals(action)) {
            if (player.hasPermission("guiban.view")) plugin.getGuiHandler().openViewPunishmentsMenu(player, PunishmentType.BAN, 0);
            return;
        }
        if ("view_mutes".equals(action)) {
            if (player.hasPermission("guiban.view")) plugin.getGuiHandler().openViewPunishmentsMenu(player, PunishmentType.MUTE, 0);
            return;
        }
        if ("view_jails".equals(action)) {
            if (player.hasPermission("guiban.view")) plugin.getGuiHandler().openViewPunishmentsMenu(player, PunishmentType.JAIL, 0);
        }
    }

    private void handlePlayerListClick(Player player, ItemStack clicked, String action) {
        if (action != null) {
            switch (action) {
                case "prev_page": {
                    int page = Math.max(0, plugin.getGuiHandler().getPlayerListPage(player) - 1);
                    plugin.getGuiHandler().openMainMenu(player, page, plugin.getGuiHandler().getPlayerListFilter(player));
                    break;
                }
                case "next_page": {
                    int page = plugin.getGuiHandler().getPlayerListPage(player) + 1;
                    plugin.getGuiHandler().openMainMenu(player, page, plugin.getGuiHandler().getPlayerListFilter(player));
                    break;
                }
                case "search":
                    player.closeInventory();
                    plugin.getGuiHandler().setSearchPending(player);
                    player.sendMessage(PermissionHelper.PREFIX() + "§7Type player name to search (or §cclear§7 to show all).");
                    break;
                case "punishments_section":
                    if (player.hasPermission("guiban.view")) plugin.getGuiHandler().openPunishmentsSectionMenu(player);
                    break;
                default:
                    break;
            }
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID targetUuid = plugin.getGuiHandler().getPlayerUuidFromHead(clicked);
            if (targetUuid != null) {
                plugin.getGuiHandler().openModerationMenu(player, Bukkit.getOfflinePlayer(targetUuid));
            }
        }
    }

    private void handleModerationClick(Player player, ItemStack clicked, String action) {
        UUID targetUuid = plugin.getGuiHandler().getModerationTarget(player.getUniqueId());
        if (targetUuid == null) return;
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = target.getName() != null ? target.getName() : targetUuid.toString();

        if ("back".equals(action)) {
            plugin.getGuiHandler().clearModerationTarget(player.getUniqueId());
            plugin.getGuiHandler().openMainMenu(player);
            return;
        }
        if ("unban".equals(action)) {
            if (player.hasPermission("guiban.unban")) {
                AsyncStorageHelper.removePunishmentAsync(plugin, targetUuid, PunishmentType.BAN, () -> {
                    player.sendMessage(PermissionHelper.PREFIX() + "§f" + targetName + " §7has been unbanned.");
                    player.closeInventory();
                });
            } else {
                player.closeInventory();
            }
            return;
        }
        if ("unmute".equals(action)) {
            if (player.hasPermission("guiban.unmute")) {
                AsyncStorageHelper.removePunishmentAsync(plugin, targetUuid, PunishmentType.MUTE, () -> {
                    player.sendMessage(PermissionHelper.PREFIX() + "§f" + targetName + " §7has been unmuted.");
                    player.closeInventory();
                });
            } else {
                player.closeInventory();
            }
            return;
        }
        if ("unjail".equals(action)) {
            if (player.hasPermission("guiban.unjail")) {
                AsyncStorageHelper.removePunishmentAsync(plugin, targetUuid, PunishmentType.JAIL, () -> {
                    player.sendMessage(PermissionHelper.PREFIX() + "§f" + targetName + " §7has been unjailed.");
                    player.closeInventory();
                });
            } else {
                player.closeInventory();
            }
            return;
        }

        if (!PermissionHelper.canBePunishedBy(target, player)) {
            player.sendMessage(PermissionHelper.PREFIX() + "§cYou cannot punish that player (bypass or higher level).");
            return;
        }

        if ("ban".equals(action)) {
            if (!player.hasPermission("guiban.ban")) return;
            player.closeInventory();
            plugin.getGuiHandler().openTimeMenu(player, targetUuid, PunishmentType.BAN);
            return;
        }
        if ("mute".equals(action)) {
            if (!player.hasPermission("guiban.mute")) return;
            player.closeInventory();
            plugin.getGuiHandler().openTimeMenu(player, targetUuid, PunishmentType.MUTE);
            return;
        }
        if ("jail".equals(action)) {
            if (!player.hasPermission("guiban.jail")) return;
            player.closeInventory();
            plugin.getGuiHandler().openTimeMenu(player, targetUuid, PunishmentType.JAIL);
            return;
        }
        if ("kick".equals(action)) {
            if (!player.hasPermission("guiban.kick")) return;
            player.closeInventory();
            plugin.getReasonPendingByPlayer().put(player.getUniqueId(), new ReasonPending(PunishmentType.KICK, targetUuid, 0));
            player.sendMessage(PermissionHelper.PREFIX() + "§7Type reason in chat (or §ccancel§7).");
        }
    }

    private void handleTimeMenuClick(Player player, ItemStack clicked, String action) {
        if (action == null) return;
        UUID targetUuid = plugin.getGuiHandler().getModerationTarget(player.getUniqueId());
        if (targetUuid == null) return;
        PunishmentType type = plugin.getGuiHandler().getTimeMenuType(player);
        if (type == null) return;

        long expiry;
        switch (action) {
            case "time_1m": expiry = System.currentTimeMillis() + 60 * 1000; break;
            case "time_5m": expiry = System.currentTimeMillis() + 5 * 60 * 1000; break;
            case "time_15m": expiry = System.currentTimeMillis() + 15 * 60 * 1000; break;
            case "time_30m": expiry = System.currentTimeMillis() + 30 * 60 * 1000; break;
            case "time_1h": expiry = System.currentTimeMillis() + 60 * 60 * 1000; break;
            case "time_6h": expiry = System.currentTimeMillis() + 6 * 60 * 60 * 1000; break;
            case "time_12h": expiry = System.currentTimeMillis() + 12 * 60 * 60 * 1000; break;
            case "time_1d": expiry = System.currentTimeMillis() + 24 * 60 * 60 * 1000; break;
            case "time_1w": expiry = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000; break;
            case "time_2w": expiry = System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000; break;
            case "time_perm": expiry = -1; break;
            default: return;
        }

        player.closeInventory();
        plugin.getReasonPendingByPlayer().put(player.getUniqueId(), new ReasonPending(type, targetUuid, expiry));
        player.sendMessage(PermissionHelper.PREFIX() + "§7Type reason in chat (or §ccancel§7).");
    }

    private void handleViewPunishmentsClick(Player player, ItemStack clicked, String action) {
        if (action != null) {
            PunishmentType type = plugin.getGuiHandler().getViewPunishmentsType(player);
            int page = plugin.getGuiHandler().getViewPunishmentsPage(player);
            if ("view_prev".equals(action)) {
                plugin.getGuiHandler().openViewPunishmentsMenu(player, type, Math.max(0, page - 1));
                return;
            }
            if ("view_next".equals(action)) {
                plugin.getGuiHandler().openViewPunishmentsMenu(player, type, page + 1);
                return;
            }
            if ("view_back".equals(action)) {
                plugin.getGuiHandler().clearModerationTarget(player.getUniqueId());
                plugin.getGuiHandler().openMainMenu(player);
            }
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID uuid = plugin.getGuiHandler().getPlayerUuidFromHead(clicked);
            if (uuid != null) {
                String displayName = Bukkit.getOfflinePlayer(uuid).getName() != null ? Bukkit.getOfflinePlayer(uuid).getName() : uuid.toString();
                player.sendMessage(PermissionHelper.PREFIX() + "§7Loading history for §f" + displayName + "§7...");
                AsyncStorageHelper.getHistoryAsync(plugin, uuid, history -> {
                    player.sendMessage(PermissionHelper.PREFIX() + "§7History for §f" + displayName);
                    for (com.enterprise.guiban.storage.Punishment p : history) {
                        String dur = TimeUtil.formatDuration(p.getExpiryTime());
                        player.sendMessage("§8- §7" + p.getType() + " §f" + p.getReason() + " §8(" + dur + ") §7by §f" + p.getPunisher());
                    }
                });
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player p = (Player) event.getPlayer();
            String title = event.getView().getTitle();
            if (title.startsWith("§8Manage:") || title.startsWith("§8Select duration:")) {
                plugin.getGuiHandler().clearModerationTarget(p.getUniqueId());
            }
        }
    }
}

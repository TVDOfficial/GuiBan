package com.enterprise.guiban.ui;

import com.enterprise.guiban.GUIBAN;
import com.enterprise.guiban.storage.AsyncStorageHelper;
import com.enterprise.guiban.storage.Punishment;
import com.enterprise.guiban.storage.PunishmentType;
import com.enterprise.guiban.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GUIHandler {

    private static final int PLAYERS_PER_PAGE = 45;
    private static final int VIEW_PER_PAGE = 45;

    private final GUIBAN plugin;
    private final NamespacedKey keyPlayerUuid;
    private final NamespacedKey keyAction;
    private FileConfiguration guiConfig;
    private final Map<UUID, UUID> moderationTargetByOpener = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerListPage = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerListFilter = new ConcurrentHashMap<>();
    private final Map<UUID, PunishmentType> viewPunishmentsType = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> viewPunishmentsPage = new ConcurrentHashMap<>();
    private final Map<UUID, PunishmentType> timeMenuTypeByOpener = new ConcurrentHashMap<>();
    private final Map<UUID, String> searchPending = new ConcurrentHashMap<>();

    public GUIHandler(GUIBAN plugin) {
        this.plugin = plugin;
        this.keyPlayerUuid = new NamespacedKey(plugin, "player_uuid");
        this.keyAction = new NamespacedKey(plugin, "action");
        loadConfig();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (!file.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void openMainMenu(Player player) {
        openMainMenu(player, playerListPage.getOrDefault(player.getUniqueId(), 0), playerListFilter.get(player.getUniqueId()));
    }

    public void openMainMenu(Player player, int page, String filter) {
        playerListPage.put(player.getUniqueId(), page);
        if (filter == null) playerListFilter.remove(player.getUniqueId()); else playerListFilter.put(player.getUniqueId(), filter);
        List<OfflinePlayer> toShow = new ArrayList<>();
        if (filter != null && !filter.isEmpty()) {
            toShow.addAll(com.enterprise.guiban.utils.PlayerLookup.search(filter));
        } else {
            toShow.addAll(Bukkit.getOnlinePlayers());
        }
        int totalPages = Math.max(1, (toShow.size() + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE);
        int from = page * PLAYERS_PER_PAGE;
        int to = Math.min(from + PLAYERS_PER_PAGE, toShow.size());

        String title = guiConfig.getString("menus.player_selection.title", "§8GUIBan - Player Selection");
        if (page > 0 || to < toShow.size()) title += " §7(" + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        for (int i = from; i < to; i++) {
            inv.setItem(i - from, getPlayerHead(toShow.get(i)));
        }

        inv.setItem(45, createButton(Material.RED_WOOL, "§c§lPrevious Page", "prev_page"));
        inv.setItem(46, createButton(Material.WRITABLE_BOOK, "§6§lPunishments §8(Lists)", "punishments_section"));
        inv.setItem(49, createButton(Material.NAME_TAG, "§b§lSearch Player", "search"));
        inv.setItem(53, createButton(Material.GREEN_WOOL, "§a§lNext Page", "next_page"));

        player.openInventory(inv);
    }

    /** Opens the "Punishments" section menu: choose Bans / Mutes / Jails list. Loads counts async. */
    public void openPunishmentsSectionMenu(Player player) {
        player.openInventory(Bukkit.createInventory(null, 27, "§8Punishments - Loading..."));
        AsyncStorageHelper.loadPunishmentCountsAsync(plugin, counts -> {
            int banCount = counts[0];
            int muteCount = counts[1];
            int jailCount = counts[2];
            Inventory inv = Bukkit.createInventory(null, 27, "§8Punishments - Lists");
            inv.setItem(11, createButton(Material.RED_CONCRETE, "§c§lActive Bans §7(" + banCount + ")", "view_bans"));
            inv.setItem(13, createButton(Material.ORANGE_CONCRETE, "§6§lActive Mutes §7(" + muteCount + ")", "view_mutes"));
            inv.setItem(15, createButton(Material.YELLOW_CONCRETE, "§e§lActive Jails §7(" + jailCount + ")", "view_jails"));
            inv.setItem(22, createButton(Material.ARROW, "§f§lBack", "punishments_back"));
            player.openInventory(inv);
        });
    }

    public void openModerationMenu(Player sender, OfflinePlayer target) {
        moderationTargetByOpener.put(sender.getUniqueId(), target.getUniqueId());
        String tName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        String title = guiConfig.getString("menus.moderation.title", "§8Manage: {PLAYER}").replace("{PLAYER}", tName);
        Inventory inv = Bukkit.createInventory(null, 27, title);

        inv.setItem(10, createConfigItem("ban", tName));
        inv.setItem(12, createConfigItem("mute", tName));
        inv.setItem(14, createConfigItem("jail", tName));
        inv.setItem(16, createConfigItem("kick", tName));
        inv.setItem(19, createButton(Material.LIME_CONCRETE, "§a§lUnban", "unban"));
        inv.setItem(21, createButton(Material.GREEN_CONCRETE, "§a§lUnmute", "unmute"));
        inv.setItem(23, createButton(Material.LIGHT_GRAY_CONCRETE, "§7§lUnjail", "unjail"));
        inv.setItem(22, createButton(Material.ARROW, "§f§lBack", "back"));

        sender.openInventory(inv);
    }

    public void openTimeMenu(Player opener, UUID targetUuid, PunishmentType type) {
        moderationTargetByOpener.put(opener.getUniqueId(), targetUuid);
        timeMenuTypeByOpener.put(opener.getUniqueId(), type);
        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
        if (targetName == null) targetName = targetUuid.toString();
        String title = "§8Select duration: " + targetName;
        Inventory inv = Bukkit.createInventory(null, 27, title);

        inv.setItem(10, createButton(Material.CLOCK, "§71 minute", "time_1m"));
        inv.setItem(11, createButton(Material.CLOCK, "§75 minutes", "time_5m"));
        inv.setItem(12, createButton(Material.CLOCK, "§715 minutes", "time_15m"));
        inv.setItem(13, createButton(Material.CLOCK, "§730 minutes", "time_30m"));
        inv.setItem(14, createButton(Material.CLOCK, "§71 hour", "time_1h"));
        inv.setItem(15, createButton(Material.CLOCK, "§76 hours", "time_6h"));
        inv.setItem(16, createButton(Material.CLOCK, "§712 hours", "time_12h"));
        inv.setItem(19, createButton(Material.CLOCK, "§71 day", "time_1d"));
        inv.setItem(20, createButton(Material.CLOCK, "§72 weeks", "time_2w"));
        inv.setItem(21, createButton(Material.CLOCK, "§71 week", "time_1w"));
        inv.setItem(22, createButton(Material.BARRIER, "§c§lPermanent", "time_perm"));

        opener.openInventory(inv);
    }

    public void openViewPunishmentsMenu(Player player, PunishmentType type, int page) {
        viewPunishmentsType.put(player.getUniqueId(), type);
        viewPunishmentsPage.put(player.getUniqueId(), page);
        player.openInventory(Bukkit.createInventory(null, 54, "§8Active " + type.name() + "s - Loading..."));
        AsyncStorageHelper.getActivePunishmentsAsync(plugin, type, list -> {
            int totalPages = Math.max(1, (list.size() + VIEW_PER_PAGE - 1) / VIEW_PER_PAGE);
            int from = page * VIEW_PER_PAGE;
            int to = Math.min(from + VIEW_PER_PAGE, list.size());

            String title = "§8Active " + type.name() + "s §7(" + (page + 1) + "/" + totalPages + ")";
            Inventory inv = Bukkit.createInventory(null, 54, title);

            for (int i = from; i < to; i++) {
                Punishment p = list.get(i);
                inv.setItem(i - from, getPunishmentDisplayItem(p));
            }

            inv.setItem(45, createButton(Material.RED_WOOL, "§c§lPrevious Page", "view_prev"));
            inv.setItem(49, createButton(Material.ARROW, "§f§lBack to list", "view_back"));
            inv.setItem(53, createButton(Material.GREEN_WOOL, "§a§lNext Page", "view_next"));

            player.openInventory(inv);
        });
    }

    private ItemStack getPunishmentDisplayItem(Punishment p) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(p.getUuid());
            meta.setOwningPlayer(op);
            meta.getPersistentDataContainer().set(keyPlayerUuid, PersistentDataType.STRING, p.getUuid().toString());
            String name = op.getName() != null ? op.getName() : p.getUuid().toString();
            meta.setDisplayName("§f" + name);
            String duration = TimeUtil.formatDuration(p.getExpiryTime());
            meta.setLore(java.util.Arrays.asList(
                "§7Reason: §f" + p.getReason(),
                "§7Duration: §f" + duration,
                "§7By: §f" + p.getPunisher(),
                "§8Click to view history"
            ));
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createButton(Material mat, String name, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.getPersistentDataContainer().set(keyAction, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getPlayerHead(OfflinePlayer player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.getPersistentDataContainer().set(keyPlayerUuid, PersistentDataType.STRING, player.getUniqueId().toString());
            String pName = player.getName() != null ? player.getName() : "Unknown";
            String name = guiConfig.getString("menus.player_selection.item.name", "§b{PLAYER}").replace("{PLAYER}", pName);
            meta.setDisplayName(name);
            List<String> lore = guiConfig.getStringList("menus.player_selection.item.lore").stream()
                    .map(s -> s.replace("{PLAYER}", pName))
                    .collect(Collectors.toList());
            List<String> statusLines = getPlayerStatusLore(player.getUniqueId());
            if (!statusLines.isEmpty()) {
                lore.add("");
                lore.add("§8§lStatus:");
                lore.addAll(statusLines);
            }
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private List<String> getPlayerStatusLore(UUID uuid) {
        List<String> lines = new ArrayList<>();
        Punishment ban = plugin.getStorage().getActivePunishment(uuid, PunishmentType.BAN);
        if (ban != null) {
            lines.add("§c§lBanned §7(" + TimeUtil.formatDuration(ban.getExpiryTime()) + ")");
            lines.add("§7Reason: §f" + ban.getReason());
        }
        Punishment mute = plugin.getStorage().getActivePunishment(uuid, PunishmentType.MUTE);
        if (mute != null) {
            lines.add("§6§lMuted §7(" + TimeUtil.formatDuration(mute.getExpiryTime()) + ")");
            lines.add("§7Reason: §f" + mute.getReason());
        }
        Punishment jail = plugin.getStorage().getActivePunishment(uuid, PunishmentType.JAIL);
        if (jail != null) {
            lines.add("§e§lJailed §7(" + TimeUtil.formatDuration(jail.getExpiryTime()) + ")");
            lines.add("§7Reason: §f" + jail.getReason());
        }
        return lines;
    }

    private ItemStack createConfigItem(String key, String targetName) {
        String path = "menus.moderation.items." + key;
        Material mat = Material.valueOf(guiConfig.getString(path + ".material", "STONE"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(guiConfig.getString(path + ".name", "§c" + key.toUpperCase()));
            List<String> lore = guiConfig.getStringList(path + ".lore").stream()
                    .map(s -> s.replace("{PLAYER}", targetName))
                    .collect(Collectors.toList());
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyAction, PersistentDataType.STRING, key);
            item.setItemMeta(meta);
        }
        return item;
    }

    public UUID getModerationTarget(UUID openerUuid) {
        return moderationTargetByOpener.get(openerUuid);
    }

    public void clearModerationTarget(UUID openerUuid) {
        moderationTargetByOpener.remove(openerUuid);
        viewPunishmentsType.remove(openerUuid);
        viewPunishmentsPage.remove(openerUuid);
        timeMenuTypeByOpener.remove(openerUuid);
    }

    public PunishmentType getTimeMenuType(Player p) {
        return timeMenuTypeByOpener.get(p.getUniqueId());
    }

    public UUID getPlayerUuidFromHead(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(keyPlayerUuid, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keyAction, PersistentDataType.STRING);
    }

    public int getPlayerListPage(Player p) { return playerListPage.getOrDefault(p.getUniqueId(), 0); }
    public void setPlayerListPage(Player p, int page) { playerListPage.put(p.getUniqueId(), page); }
    public String getPlayerListFilter(Player p) { return playerListFilter.get(p.getUniqueId()); }
    public void setPlayerListFilter(Player p, String filter) { playerListFilter.put(p.getUniqueId(), filter); }
    public void clearSearchPending(Player p) { searchPending.remove(p.getUniqueId()); }
    public void setSearchPending(Player p) { searchPending.put(p.getUniqueId(), ""); }
    public boolean isSearchPending(Player p) { return searchPending.containsKey(p.getUniqueId()); }

    public PunishmentType getViewPunishmentsType(Player p) { return viewPunishmentsType.get(p.getUniqueId()); }
    public int getViewPunishmentsPage(Player p) { return viewPunishmentsPage.getOrDefault(p.getUniqueId(), 0); }
}

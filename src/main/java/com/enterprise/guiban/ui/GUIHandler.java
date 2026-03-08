package com.enterprise.guiban.ui;

import com.enterprise.guiban.GUIBAN;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GUIHandler {

    private final GUIBAN plugin;
    private final NamespacedKey keyPlayerUuid;
    private FileConfiguration guiConfig;
    private final Map<UUID, UUID> moderationTargetByOpener = new ConcurrentHashMap<>();

    public GUIHandler(GUIBAN plugin) {
        this.plugin = plugin;
        this.keyPlayerUuid = new NamespacedKey(plugin, "player_uuid");
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
        String title = guiConfig.getString("menus.player_selection.title", "§8GUIBan - Player Selection");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= 54) break;
            inv.setItem(slot++, getPlayerHead(target));
        }

        player.openInventory(inv);
    }

    private ItemStack getPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.getPersistentDataContainer().set(keyPlayerUuid, PersistentDataType.STRING, player.getUniqueId().toString());
            String name = guiConfig.getString("menus.player_selection.item.name", "§b{PLAYER}")
                    .replace("{PLAYER}", player.getName());
            meta.setDisplayName(name);
            List<String> lore = guiConfig.getStringList("menus.player_selection.item.lore").stream()
                    .map(s -> s.replace("{PLAYER}", player.getName()))
                    .collect(Collectors.toList());
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    public void openModerationMenu(Player sender, Player target) {
        moderationTargetByOpener.put(sender.getUniqueId(), target.getUniqueId());
        String title = guiConfig.getString("menus.moderation.title", "§8Manage: {PLAYER}")
                .replace("{PLAYER}", target.getName());
        Inventory inv = Bukkit.createInventory(null, 27, title);

        inv.setItem(10, createConfigItem("ban", target));
        inv.setItem(12, createConfigItem("mute", target));
        inv.setItem(14, createConfigItem("jail", target));
        inv.setItem(16, createConfigItem("kick", target));

        sender.openInventory(inv);
    }

    public UUID getModerationTarget(UUID openerUuid) {
        return moderationTargetByOpener.get(openerUuid);
    }

    public void clearModerationTarget(UUID openerUuid) {
        moderationTargetByOpener.remove(openerUuid);
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

    private ItemStack createConfigItem(String key, Player target) {
        String path = "menus.moderation.items." + key;
        Material mat = Material.valueOf(guiConfig.getString(path + ".material", "STONE"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(guiConfig.getString(path + ".name", "§c" + key.toUpperCase()));
        List<String> lore = guiConfig.getStringList(path + ".lore").stream()
                .map(s -> s.replace("{PLAYER}", target.getName()))
                .collect(Collectors.toList());
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
}

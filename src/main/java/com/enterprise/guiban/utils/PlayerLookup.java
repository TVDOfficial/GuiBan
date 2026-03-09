package com.enterprise.guiban.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PlayerLookup {

    private PlayerLookup() {}

    /** Case-insensitive player lookup by name. Checks online first, then offline. */
    public static OfflinePlayer find(String name) {
        if (name == null || name.isEmpty()) return null;
        String lower = name.toLowerCase();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().equals(lower)) return p;
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op.getName() != null && op.getName().toLowerCase().equals(lower)) return op;
        return Bukkit.getOfflinePlayer(name);
    }

    /** Get players matching query for search (online + offline). */
    public static List<OfflinePlayer> search(String query) {
        List<OfflinePlayer> out = new ArrayList<>();
        if (query == null || query.isEmpty()) return out;
        String q = query.toLowerCase();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().contains(q)) out.add(p);
        }
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && op.getName().toLowerCase().contains(q)) {
                boolean exists = out.stream().anyMatch(o -> o.getUniqueId().equals(op.getUniqueId()));
                if (!exists) out.add(op);
            }
        }
        return out;
    }
}

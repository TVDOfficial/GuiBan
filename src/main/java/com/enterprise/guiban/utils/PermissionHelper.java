package com.enterprise.guiban.utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Permission levels 1-10. Higher level can punish lower level.
 * guiban.bypass = cannot be punished. guiban.level.* or guiban.* = level 10.
 */
public final class PermissionHelper {

    public static String PREFIX() {
        try {
            return com.enterprise.guiban.utils.MessageHelper.prefix();
        } catch (Exception e) {
            return "§b[GUIBan] §r";
        }
    }

    private PermissionHelper() {}

    /** Returns 1-10, or 0 if no level permission. guiban.* or guiban.level.* counts as 10. */
    public static int getLevel(CommandSender sender) {
        if (sender == null || !sender.hasPermission("guiban.use")) return 0;
        if (sender.hasPermission("guiban.*") || sender.hasPermission("guiban.level.*")) return 10;
        for (int i = 10; i >= 1; i--) {
            if (sender.hasPermission("guiban.level." + i)) return i;
        }
        return 0;
    }

    /** True if target cannot be punished (bypass or higher/equal level). */
    public static boolean canBePunishedBy(OfflinePlayer target, CommandSender punisher) {
        if (target == null || punisher == null) return true;
        if (punisher instanceof Player && target.getUniqueId().equals(((Player) punisher).getUniqueId())) return false;
        if (target.isOnline() && target.getPlayer() != null) {
            Player p = target.getPlayer();
            if (p.hasPermission("guiban.bypass") || p.hasPermission("guiban.*")) return false;
            int targetLevel = getLevel(p);
            int punisherLevel = getLevel(punisher);
            if (punisherLevel > 0 && targetLevel >= punisherLevel) return false;
        }
        return true;
    }

    public static boolean hasBypass(OfflinePlayer target) {
        if (target == null || !target.isOnline() || target.getPlayer() == null) return false;
        return target.getPlayer().hasPermission("guiban.bypass") || target.getPlayer().hasPermission("guiban.*");
    }
}

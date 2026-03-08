package com.enterprise.guiban.command;

import com.enterprise.guiban.GUIBAN;
import com.enterprise.guiban.storage.Punishment;
import com.enterprise.guiban.storage.PunishmentType;
import com.enterprise.guiban.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GuiBanCommand implements CommandExecutor {

    private final GUIBAN plugin;

    public GuiBanCommand(GUIBAN plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            if (sender instanceof Player) {
                if (!sender.hasPermission("guiban.use")) {
                    sender.sendMessage("§cYou do not have permission to use GUIBan.");
                    return true;
                }
                plugin.getGuiHandler().openMainMenu((Player) sender);
            } else {
                sender.sendMessage("§cUsage: /guiban <ban|mute|kick|jail|unban|unmute|unjail> <player> [time] [reason]");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /guiban " + sub + " <player> [time] [reason]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID uuid = target.getUniqueId();
        String playerName = target.getName() != null ? target.getName() : args[1];

        String timeStr = args.length > 2 ? args[2] : "perm";
        long expiry = TimeUtil.parseTime(timeStr);
        
        // If expiry is -1 but arguments were provided, it might be a reason instead of time
        String reason = "No reason provided";
        if (expiry == -1 && !timeStr.equalsIgnoreCase("perm") && !timeStr.equalsIgnoreCase("permanent")) {
             // Treat timeStr as part of reason
             reason = timeStr + (args.length > 3 ? " " + getReason(args, 3) : "");
             expiry = -1;
        } else {
             reason = args.length > 3 ? getReason(args, 3) : "No reason provided";
        }

        switch (sub) {
            case "ban":
                if (!sender.hasPermission("guiban.ban")) { sender.sendMessage("§cNo permission."); return true; }
                executePunishment(sender, uuid, playerName, PunishmentType.BAN, reason, expiry);
                break;
            case "mute":
                if (!sender.hasPermission("guiban.mute")) { sender.sendMessage("§cNo permission."); return true; }
                executePunishment(sender, uuid, playerName, PunishmentType.MUTE, reason, expiry);
                break;
            case "jail":
                if (!sender.hasPermission("guiban.jail")) { sender.sendMessage("§cNo permission."); return true; }
                executePunishment(sender, uuid, playerName, PunishmentType.JAIL, reason, expiry);
                break;
            case "kick":
                if (!sender.hasPermission("guiban.kick")) { sender.sendMessage("§cNo permission."); return true; }
                if (target.isOnline()) {
                    target.getPlayer().kickPlayer("§cKicked by §f" + sender.getName() + "\n§7Reason: §f" + reason);
                    sender.sendMessage("§b[GUIBan] §f" + playerName + " §7has been kicked.");
                } else {
                    sender.sendMessage("§cPlayer is not online.");
                }
                break;
            case "unban":
                if (!sender.hasPermission("guiban.unban")) { sender.sendMessage("§cNo permission."); return true; }
                plugin.getStorage().removePunishment(uuid, PunishmentType.BAN);
                sender.sendMessage("§b[GUIBan] §f" + playerName + " §7has been unbanned.");
                break;
            case "unmute":
                if (!sender.hasPermission("guiban.unmute")) { sender.sendMessage("§cNo permission."); return true; }
                plugin.getStorage().removePunishment(uuid, PunishmentType.MUTE);
                sender.sendMessage("§b[GUIBan] §f" + playerName + " §7has been unmuted.");
                break;
            case "unjail":
                if (!sender.hasPermission("guiban.unjail")) { sender.sendMessage("§cNo permission."); return true; }
                plugin.getStorage().removePunishment(uuid, PunishmentType.JAIL);
                sender.sendMessage("§b[GUIBan] §f" + playerName + " §7has been unjailed.");
                break;
            case "reload":
                if (!sender.hasPermission("guiban.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                plugin.reloadPluginConfigs();
                sender.sendMessage("§b[GUIBan] §7Configurations reloaded.");
                break;
            case "help":
                sender.sendMessage("§b[]=====[ §fGUIBan Help §b]=====[]");
                sender.sendMessage("§b/gb §7- Open GUI");
                sender.sendMessage("§b/gb ban <player> [time] [reason] §7- Ban player");
                sender.sendMessage("§b/gb mute <player> [time] [reason] §7- Mute player");
                sender.sendMessage("§b/gb jail <player> [time] [reason] §7- Jail player");
                sender.sendMessage("§b/gb kick <player> [reason] §7- Kick player");
                sender.sendMessage("§b/gb unban/unmute/unjail <player> §7- Remove punishment");
                sender.sendMessage("§b/gb reload §7- Reload configs");
                sender.sendMessage("§b[]=======================[]");
                break;
            default:
                sender.sendMessage("§cUnknown subcommand.");
                break;
        }

        return true;
    }

    private void executePunishment(CommandSender sender, UUID uuid, String name, PunishmentType type, String reason, long expiry) {
        Punishment p = new Punishment(uuid, type, reason, System.currentTimeMillis(), expiry, sender.getName());
        plugin.getStorage().addPunishment(p);

        String duration = TimeUtil.formatDuration(expiry);
        sender.sendMessage("§b[GUIBan] §f" + name + " §7has been " + type.name().toLowerCase() + "ed.");
        sender.sendMessage("§7Reason: §f" + reason);
        sender.sendMessage("§7Duration: §f" + duration);

        if (type == PunishmentType.BAN && Bukkit.getPlayer(uuid) != null) {
            Bukkit.getPlayer(uuid).kickPlayer("§cYou are banned!\n§7Reason: §f" + reason + "\n§7Expires: §f" + duration);
        } else if (Bukkit.getPlayer(uuid) != null) {
            Bukkit.getPlayer(uuid).sendMessage("§cYou have been " + type.name().toLowerCase() + "ed for §f" + duration);
            Bukkit.getPlayer(uuid).sendMessage("§7Reason: §f" + reason);
        }
    }

    private String getReason(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        return sb.toString().trim();
    }
}

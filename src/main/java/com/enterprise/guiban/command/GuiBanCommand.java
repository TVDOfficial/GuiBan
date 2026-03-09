package com.enterprise.guiban.command;

import com.enterprise.guiban.GUIBAN;
import com.enterprise.guiban.storage.AsyncStorageHelper;
import com.enterprise.guiban.storage.Punishment;
import com.enterprise.guiban.storage.PunishmentType;
import com.enterprise.guiban.utils.*;
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
                    sender.sendMessage(MessageHelper.get("no-permission"));
                    return true;
                }
                SoundHelper.play(plugin, (Player) sender, "open-menu");
                plugin.getGuiHandler().openMainMenu((Player) sender);
            } else {
                sender.sendMessage("§cUsage: /guiban <ban|mute|kick|jail|unban|unmute|unjail> <player> [time] [reason]");
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("help")) {
            sender.sendMessage("§b[]=====[ §fGUIBan Help §b]=====[]");
            sender.sendMessage("§b/gb §7or §b/guiban §7- Open GUI");
            sender.sendMessage("§b/gb ban <player> [time] [reason] §7- Ban player");
            sender.sendMessage("§b/gb mute <player> [time] [reason] §7- Mute player");
            sender.sendMessage("§b/gb jail <player> [time] [reason] §7- Jail player");
            sender.sendMessage("§b/gb kick <player> [reason] §7- Kick player");
            sender.sendMessage("§b/gb unban/unmute/unjail <player> §7- Remove punishment");
            sender.sendMessage("§b/gb list [ban|mute|jail] §7- View active punishments");
            sender.sendMessage("§b/gb reload §7- Reload configs");
            sender.sendMessage("§b[]=======================[]");
            return true;
        }
        if (sub.equals("reload")) {
            if (!sender.hasPermission("guiban.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            plugin.reloadPluginConfigs();
            sender.sendMessage(PermissionHelper.PREFIX() + "§7Configurations reloaded.");
            return true;
        }
        if (sub.equals("list")) {
            if (!sender.hasPermission("guiban.view")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            PunishmentType listType = args.length >= 2 ? parseType(args[1]) : null;
            if (listType == null) {
                sender.sendMessage(PermissionHelper.PREFIX() + "§7Usage: /gb list <ban|mute|jail>");
                return true;
            }
            PunishmentType listTypeFinal = listType;
            sender.sendMessage(PermissionHelper.PREFIX() + "§7Loading...");
            AsyncStorageHelper.getActivePunishmentsAsync(plugin, listType, active -> {
                sender.sendMessage("§b[]=====[ §fActive " + listTypeFinal.name() + "s §b]=====[]");
                for (Punishment p : active) {
                    String name = Bukkit.getOfflinePlayer(p.getUuid()).getName();
                    if (name == null) name = p.getUuid().toString();
                    String duration = TimeUtil.formatDuration(p.getExpiryTime());
                    sender.sendMessage("§7" + name + " §f- §7" + p.getReason() + " §8(" + duration + ") §7by §f" + p.getPunisher());
                }
                sender.sendMessage("§b[]=======================[]");
            });
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /guiban " + sub + " <player> [time] [reason]");
            return true;
        }

        OfflinePlayer target = PlayerLookup.find(args[1]);
        if (target == null) target = Bukkit.getOfflinePlayer(args[1]);
        final OfflinePlayer targetFinal = target;
        UUID uuid = target.getUniqueId();
        String playerName = target.getName() != null ? target.getName() : args[1];

        if (sender instanceof Player && !plugin.getRateLimiter().allow(((Player) sender).getUniqueId())) {
            sender.sendMessage(MessageHelper.get("prefix") + "§cRate limit exceeded. Try again later.");
            return true;
        }

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
        final String reasonFinal = reason;

        switch (sub) {
            case "ban":
                if (!sender.hasPermission("guiban.ban")) { sender.sendMessage("§cNo permission."); return true; }
                if (!PermissionHelper.canBePunishedBy(target, sender)) {
                    sender.sendMessage(PermissionHelper.PREFIX() + "§cYou cannot punish that player (bypass or higher level).");
                    return true;
                }
                executePunishment(sender, uuid, playerName, PunishmentType.BAN, reason, expiry);
                break;
            case "mute":
                if (!sender.hasPermission("guiban.mute")) { sender.sendMessage("§cNo permission."); return true; }
                if (!PermissionHelper.canBePunishedBy(target, sender)) {
                    sender.sendMessage(PermissionHelper.PREFIX() + "§cYou cannot punish that player (bypass or higher level).");
                    return true;
                }
                executePunishment(sender, uuid, playerName, PunishmentType.MUTE, reason, expiry);
                break;
            case "jail":
                if (!sender.hasPermission("guiban.jail")) { sender.sendMessage("§cNo permission."); return true; }
                if (!PermissionHelper.canBePunishedBy(target, sender)) {
                    sender.sendMessage(PermissionHelper.PREFIX() + "§cYou cannot punish that player (bypass or higher level).");
                    return true;
                }
                executePunishment(sender, uuid, playerName, PunishmentType.JAIL, reason, expiry);
                break;
            case "kick":
                if (!sender.hasPermission("guiban.kick")) { sender.sendMessage("§cNo permission."); return true; }
                if (!PermissionHelper.canBePunishedBy(target, sender)) {
                    sender.sendMessage(PermissionHelper.PREFIX() + "§cYou cannot punish that player (bypass or higher level).");
                    return true;
                }
                if (target.isOnline()) {
                    target.getPlayer().kickPlayer("§cKicked by §f" + sender.getName() + "\n§7Reason: §f" + reason);
                    sender.sendMessage(PermissionHelper.PREFIX() + "§f" + playerName + " §7has been kicked.");
                } else {
                    sender.sendMessage("§cPlayer is not online.");
                }
                break;
            case "unban":
                if (!sender.hasPermission("guiban.unban")) { sender.sendMessage("§cNo permission."); return true; }
                AsyncStorageHelper.removePunishmentAsync(plugin, uuid, PunishmentType.BAN, () ->
                    sender.sendMessage(PermissionHelper.PREFIX() + "§f" + playerName + " §7has been unbanned."));
                break;
            case "unmute":
                if (!sender.hasPermission("guiban.unmute")) { sender.sendMessage("§cNo permission."); return true; }
                AsyncStorageHelper.removePunishmentAsync(plugin, uuid, PunishmentType.MUTE, () ->
                    sender.sendMessage(PermissionHelper.PREFIX() + "§f" + playerName + " §7has been unmuted."));
                break;
            case "unjail":
                if (!sender.hasPermission("guiban.unjail")) { sender.sendMessage("§cNo permission."); return true; }
                AsyncStorageHelper.removePunishmentAsync(plugin, uuid, PunishmentType.JAIL, () ->
                    sender.sendMessage(PermissionHelper.PREFIX() + "§f" + playerName + " §7has been unjailed."));
                break;
            case "warn":
                if (!sender.hasPermission("guiban.warn")) { sender.sendMessage("§cNo permission."); return true; }
                if (!PermissionHelper.canBePunishedBy(target, sender)) {
                    sender.sendMessage(PermissionHelper.PREFIX() + "§cYou cannot punish that player.");
                    return true;
                }
                Punishment warnP = new Punishment(uuid, PunishmentType.WARN, reason, System.currentTimeMillis(), expiry, sender.getName());
                AsyncStorageHelper.addPunishmentAsync(plugin, warnP, () -> {
                    int count = plugin.getStorage().getWarnCount(uuid);
                    sender.sendMessage(PermissionHelper.PREFIX() + "§f" + playerName + " §7warned. §8(" + count + " warnings)");
                    if (targetFinal.isOnline()) targetFinal.getPlayer().sendMessage("§cYou have been warned! §7Reason: §f" + reasonFinal);
                    if (plugin.getConfig().getBoolean("warns.enabled", false)) {
                        int max = plugin.getConfig().getInt("warns.max-before-punish", 3);
                        if (count >= max) {
                            PunishmentType punishType = PunishmentType.valueOf(plugin.getConfig().getString("warns.punish-type", "MUTE"));
                            long punishExpiry = TimeUtil.parseTime(plugin.getConfig().getString("warns.punish-duration", "1h"));
                            executePunishment(sender, uuid, playerName, punishType, "Auto: " + count + " warnings", punishExpiry);
                        }
                    }
                });
                break;
            case "ipban":
                if (!sender.hasPermission("guiban.ipban")) { sender.sendMessage("§cNo permission."); return true; }
                String ip;
                if (args[1].matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                    ip = args[1];
                } else {
                    if (!target.isOnline()) {
                        sender.sendMessage(MessageHelper.get("player-offline"));
                        return true;
                    }
                    ip = target.getPlayer().getAddress().getAddress().getHostAddress();
                }
                plugin.getStorage().addIpBan(ip, reason, expiry, sender.getName());
                String banMsg = MessageHelper.get("ban-message").replace("{reason}", reason).replace("{duration}", TimeUtil.formatDuration(expiry)).replace("{appeal}", plugin.getConfig().getString("ban-appeal-url", ""));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getAddress() != null && ip.equals(p.getAddress().getAddress().getHostAddress()))
                        p.kickPlayer(banMsg);
                }
                sender.sendMessage(PermissionHelper.PREFIX() + "§f" + (args[1].contains(".") ? ip : playerName) + " §7(" + ip + ") IP banned.");
                break;
            default:
                sender.sendMessage("§cUnknown subcommand.");
                break;
        }

        return true;
    }

    private void executePunishment(CommandSender sender, UUID uuid, String name, PunishmentType type, String reason, long expiry) {
        Punishment p = new Punishment(uuid, type, reason, System.currentTimeMillis(), expiry, sender.getName());
        String duration = TimeUtil.formatDuration(expiry);

        AsyncStorageHelper.addPunishmentAsync(plugin, p, () -> {
            AuditLogger.log(type.name(), name, sender.getName(), reason, duration);
            String webhook = plugin.getConfig().getString("discord.webhook-url", "");
            if (webhook != null && webhook.length() > 5) {
                String msg = plugin.getConfig().getString("discord." + type.name().toLowerCase() + "-message", "")
                    .replace("{player}", name).replace("{punisher}", sender.getName()).replace("{reason}", reason);
                DiscordWebhook.send(plugin, webhook, msg);
            }
            if (type == PunishmentType.BAN && plugin.getConfig().getBoolean("staff-broadcast", false)) {
                String bc = MessageHelper.get("staff-broadcast").replace("{player}", name).replace("{punisher}", sender.getName()).replace("{reason}", reason);
                Bukkit.getOnlinePlayers().stream().filter(pl -> pl.hasPermission("guiban.broadcast")).forEach(pl -> pl.sendMessage(bc));
            }
            if (sender instanceof Player) SoundHelper.play(plugin, (Player) sender, "punishment-applied");

            sender.sendMessage(PermissionHelper.PREFIX() + "§f" + name + " §7has been " + type.name().toLowerCase() + "ed.");
            sender.sendMessage("§7Reason: §f" + reason);
            sender.sendMessage("§7Duration: §f" + duration);

            String appeal = plugin.getConfig().getString("ban-appeal-url", "");
            if (type == PunishmentType.BAN && Bukkit.getPlayer(uuid) != null) {
                String banMsg = MessageHelper.get("ban-message").replace("{reason}", reason).replace("{duration}", duration).replace("{appeal}", appeal.isEmpty() ? "N/A" : appeal);
                Bukkit.getPlayer(uuid).kickPlayer(banMsg);
            } else if (Bukkit.getPlayer(uuid) != null) {
                Bukkit.getPlayer(uuid).sendMessage(MessageHelper.get("mute-jail-message").replace("{type}", type.name().toLowerCase()).replace("{duration}", duration).replace("{reason}", reason));
            }
        });
    }

    private static PunishmentType parseType(String s) {
        if (s == null) return null;
        switch (s.toLowerCase()) {
            case "ban": return PunishmentType.BAN;
            case "mute": return PunishmentType.MUTE;
            case "jail": return PunishmentType.JAIL;
            default: return null;
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

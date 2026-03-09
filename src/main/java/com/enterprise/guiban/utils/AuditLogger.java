package com.enterprise.guiban.utils;

import com.enterprise.guiban.GUIBAN;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class AuditLogger {

    private static GUIBAN plugin;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private AuditLogger() {}

    public static void init(GUIBAN p) {
        plugin = p;
    }

    public static void log(String action, String target, String punisher, String reason, String duration) {
        if (plugin == null || !plugin.getConfig().getBoolean("audit-log", true)) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File file = new File(plugin.getDataFolder(), "audit.log");
                try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
                    pw.println(FMT.format(Instant.now()) + " | " + action + " | " + target + " | " + punisher + " | " + reason + " | " + duration);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Audit log failed: " + e.getMessage());
            }
        });
    }
}

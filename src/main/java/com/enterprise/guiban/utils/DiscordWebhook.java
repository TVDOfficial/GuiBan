package com.enterprise.guiban.utils;

import com.enterprise.guiban.GUIBAN;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DiscordWebhook {

    private DiscordWebhook() {}

    public static void send(GUIBAN plugin, String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String json = "{\"content\":\"" + escapeJson(content) + "\"}";
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                if (code >= 400) plugin.getLogger().warning("Discord webhook returned " + code);
            } catch (Exception e) {
                plugin.getLogger().warning("Discord webhook failed: " + e.getMessage());
            }
        });
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}

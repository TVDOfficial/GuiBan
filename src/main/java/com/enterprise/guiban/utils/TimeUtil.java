package com.enterprise.guiban.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    public static long parseTime(String input) {
        if (input == null || input.isEmpty() || input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("permanent")) {
            return -1;
        }

        long totalMs = 0;
        Pattern pattern = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        boolean found = false;
        while (matcher.find()) {
            found = true;
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            switch (unit) {
                case "s": totalMs += amount * 1000; break;
                case "m": totalMs += amount * 60 * 1000; break;
                case "h": totalMs += amount * 60 * 60 * 1000; break;
                case "d": totalMs += amount * 24 * 60 * 60 * 1000; break;
                case "w": totalMs += amount * 7 * 24 * 60 * 60 * 1000; break;
            }
        }

        return found ? System.currentTimeMillis() + totalMs : -1;
    }

    public static String formatDuration(long expiryTime) {
        if (expiryTime == -1) return "Permanent";
        long diff = expiryTime - System.currentTimeMillis();
        if (diff <= 0) return "Expired";

        long seconds = diff / 1000 % 60;
        long minutes = diff / (60 * 1000) % 60;
        long hours = diff / (60 * 60 * 1000) % 24;
        long days = diff / (24 * 60 * 60 * 1000);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}

package com.enterprise.guiban.storage;

import java.util.UUID;
import java.util.List;

public interface StorageProvider {

    void initialize();

    void addPunishment(Punishment punishment);

    void removePunishment(UUID uuid, PunishmentType type);

    Punishment getActivePunishment(UUID uuid, PunishmentType type);

    List<Punishment> getHistory(UUID uuid);

    /** All currently active punishments of this type (latest per player, not expired). */
    List<Punishment> getActivePunishments(PunishmentType type);

    default boolean isIpBanned(String ip) { return false; }
    default void addIpBan(String ip, String reason, long expiry, String punisher) {}
    default void removeIpBan(String ip) {}
    default int getWarnCount(UUID uuid) {
        List<Punishment> history = getHistory(uuid);
        int count = 0;
        for (Punishment p : history) {
            if (p.getType() == PunishmentType.WARN && !p.isExpired()) count++;
        }
        return count;
    }
    default void clearWarns(UUID uuid) {
        removePunishment(uuid, PunishmentType.WARN);
    }

    default void cleanupExpired() {}
}

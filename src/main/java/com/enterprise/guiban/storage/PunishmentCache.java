package com.enterprise.guiban.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache for punishment lookups. Invalidated on add/remove.
 */
public class PunishmentCache {

    private static final long TTL_MS = 60_000; // 1 minute for "no punishment" to allow expiry checks

    private final Map<String, CachedEntry<Optional<Punishment>>> activeByUuidType = new ConcurrentHashMap<>();
    private final Map<PunishmentType, CachedEntry<List<Punishment>>> activeByType = new ConcurrentHashMap<>();
    private final Map<UUID, CachedEntry<Integer>> warnCountByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, CachedEntry<List<Punishment>>> historyByUuid = new ConcurrentHashMap<>();

    private static String key(UUID uuid, PunishmentType type) {
        return uuid + ":" + type.name();
    }

    /** Returns Optional.empty() on cache miss, Optional.of(p) or Optional.empty() on hit. */
    public Optional<Punishment> getActivePunishment(UUID uuid, PunishmentType type) {
        CachedEntry<Optional<Punishment>> c = activeByUuidType.get(key(uuid, type));
        if (c == null) return null;
        if (c.isExpired()) {
            activeByUuidType.remove(key(uuid, type));
            return null;
        }
        return c.value;
    }

    public void putActivePunishment(UUID uuid, PunishmentType type, Punishment p) {
        activeByUuidType.put(key(uuid, type), new CachedEntry<>(Optional.ofNullable(p)));
    }

    public List<Punishment> getActivePunishments(PunishmentType type) {
        CachedEntry<List<Punishment>> c = activeByType.get(type);
        if (c == null) return null;
        if (c.isExpired()) {
            activeByType.remove(type);
            return null;
        }
        return c.value;
    }

    public void putActivePunishments(PunishmentType type, List<Punishment> list) {
        activeByType.put(type, new CachedEntry<>(list));
    }

    public Integer getWarnCount(UUID uuid) {
        CachedEntry<Integer> c = warnCountByUuid.get(uuid);
        if (c == null) return null;
        if (c.isExpired()) {
            warnCountByUuid.remove(uuid);
            return null;
        }
        return c.value;
    }

    public void putWarnCount(UUID uuid, int count) {
        warnCountByUuid.put(uuid, new CachedEntry<>(count));
    }

    public List<Punishment> getHistory(UUID uuid) {
        CachedEntry<List<Punishment>> c = historyByUuid.get(uuid);
        if (c == null) return null;
        if (c.isExpired()) {
            historyByUuid.remove(uuid);
            return null;
        }
        return c.value;
    }

    public void putHistory(UUID uuid, List<Punishment> list) {
        historyByUuid.put(uuid, new CachedEntry<>(list));
    }

    public void invalidate(UUID uuid) {
        for (PunishmentType t : PunishmentType.values()) {
            activeByUuidType.remove(key(uuid, t));
        }
        warnCountByUuid.remove(uuid);
        historyByUuid.remove(uuid);
    }

    public void invalidate(UUID uuid, PunishmentType type) {
        activeByUuidType.remove(key(uuid, type));
        activeByType.remove(type);
        if (type == PunishmentType.WARN) {
            warnCountByUuid.remove(uuid);
        }
        historyByUuid.remove(uuid);
    }

    public void invalidateActiveList(PunishmentType type) {
        activeByType.remove(type);
    }

    private static class CachedEntry<T> {
        final T value;
        final long timestamp = System.currentTimeMillis();

        CachedEntry(T value) {
            this.value = value;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TTL_MS;
        }
    }
}

package com.enterprise.guiban.storage;

import com.enterprise.guiban.GUIBAN;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wraps a StorageProvider with caching. Invalidates cache on add/remove.
 */
public class CachedStorageProvider implements StorageProvider {

    private final StorageProvider delegate;
    private final PunishmentCache cache;
    private final GUIBAN plugin;

    public CachedStorageProvider(GUIBAN plugin, StorageProvider delegate) {
        this.plugin = plugin;
        this.delegate = delegate;
        this.cache = new PunishmentCache();
    }

    public StorageProvider getDelegate() {
        return delegate;
    }

    public void invalidateCache(UUID uuid) {
        cache.invalidate(uuid);
    }

    public void invalidateCache(UUID uuid, PunishmentType type) {
        cache.invalidate(uuid, type);
    }

    public void updateCacheHistory(UUID uuid, List<Punishment> list) {
        cache.putHistory(uuid, list);
    }

    public void updateCacheActivePunishments(PunishmentType type, List<Punishment> list) {
        cache.putActivePunishments(type, list);
    }

    @Override
    public void initialize() {
        delegate.initialize();
    }

    @Override
    public void addPunishment(Punishment punishment) {
        delegate.addPunishment(punishment);
        cache.invalidate(punishment.getUuid(), punishment.getType());
    }

    @Override
    public void removePunishment(UUID uuid, PunishmentType type) {
        delegate.removePunishment(uuid, type);
        cache.invalidate(uuid, type);
    }

    @Override
    public Punishment getActivePunishment(UUID uuid, PunishmentType type) {
        java.util.Optional<Punishment> cached = cache.getActivePunishment(uuid, type);
        if (cached != null) return cached.orElse(null);
        Punishment p = delegate.getActivePunishment(uuid, type);
        cache.putActivePunishment(uuid, type, p);
        return p;
    }

    @Override
    public List<Punishment> getHistory(UUID uuid) {
        List<Punishment> cached = cache.getHistory(uuid);
        if (cached != null) return new ArrayList<>(cached);
        List<Punishment> list = delegate.getHistory(uuid);
        cache.putHistory(uuid, list);
        return new ArrayList<>(list);
    }

    @Override
    public List<Punishment> getActivePunishments(PunishmentType type) {
        List<Punishment> cached = cache.getActivePunishments(type);
        if (cached != null) return new ArrayList<>(cached);
        List<Punishment> list = delegate.getActivePunishments(type);
        cache.putActivePunishments(type, list);
        return new ArrayList<>(list);
    }

    @Override
    public boolean isIpBanned(String ip) {
        return delegate.isIpBanned(ip);
    }

    @Override
    public void addIpBan(String ip, String reason, long expiry, String punisher) {
        delegate.addIpBan(ip, reason, expiry, punisher);
    }

    @Override
    public void removeIpBan(String ip) {
        delegate.removeIpBan(ip);
    }

    @Override
    public int getWarnCount(UUID uuid) {
        Integer cached = cache.getWarnCount(uuid);
        if (cached != null) return cached;
        int count = delegate.getWarnCount(uuid);
        cache.putWarnCount(uuid, count);
        return count;
    }

    @Override
    public void clearWarns(UUID uuid) {
        delegate.clearWarns(uuid);
        cache.invalidate(uuid, PunishmentType.WARN);
    }

    @Override
    public void cleanupExpired() {
        delegate.cleanupExpired();
        cache.invalidateActiveList(PunishmentType.BAN);
        cache.invalidateActiveList(PunishmentType.MUTE);
        cache.invalidateActiveList(PunishmentType.JAIL);
    }
}

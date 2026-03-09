package com.enterprise.guiban.storage;

import com.enterprise.guiban.GUIBAN;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Runs storage operations asynchronously. Callbacks run on the main thread.
 */
public final class AsyncStorageHelper {

    private AsyncStorageHelper() {}

    /**
     * Runs addPunishment on async thread, then invokes onComplete on main thread.
     */
    public static void addPunishmentAsync(GUIBAN plugin, Punishment p, Runnable onComplete) {
        StorageProvider delegate = getDelegate(plugin);
        CachedStorageProvider cached = getCached(plugin);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            delegate.addPunishment(p);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (cached != null) cached.invalidateCache(p.getUuid(), p.getType());
                if (onComplete != null) onComplete.run();
            });
        });
    }

    /**
     * Runs removePunishment on async thread, then invokes onComplete on main thread.
     */
    public static void removePunishmentAsync(GUIBAN plugin, UUID uuid, PunishmentType type, Runnable onComplete) {
        StorageProvider delegate = getDelegate(plugin);
        CachedStorageProvider cached = getCached(plugin);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            delegate.removePunishment(uuid, type);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (cached != null) cached.invalidateCache(uuid, type);
                if (onComplete != null) onComplete.run();
            });
        });
    }

    /**
     * Loads ban/mute/jail counts async, then invokes callback on main thread with [banCount, muteCount, jailCount].
     */
    public static void loadPunishmentCountsAsync(GUIBAN plugin, Consumer<int[]> callback) {
        StorageProvider delegate = getDelegate(plugin);
        CachedStorageProvider cached = getCached(plugin);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Punishment> bans = delegate.getActivePunishments(PunishmentType.BAN);
            List<Punishment> mutes = delegate.getActivePunishments(PunishmentType.MUTE);
            List<Punishment> jails = delegate.getActivePunishments(PunishmentType.JAIL);
            int[] counts = { bans.size(), mutes.size(), jails.size() };
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (cached != null) {
                    cached.updateCacheActivePunishments(PunishmentType.BAN, bans);
                    cached.updateCacheActivePunishments(PunishmentType.MUTE, mutes);
                    cached.updateCacheActivePunishments(PunishmentType.JAIL, jails);
                }
                if (callback != null) callback.accept(counts);
            });
        });
    }

    /**
     * Loads getActivePunishments async, then invokes callback on main thread.
     */
    public static void getActivePunishmentsAsync(GUIBAN plugin, PunishmentType type, Consumer<List<Punishment>> callback) {
        StorageProvider delegate = getDelegate(plugin);
        CachedStorageProvider cached = getCached(plugin);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Punishment> list = delegate.getActivePunishments(type);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (cached != null) cached.updateCacheActivePunishments(type, list);
                if (callback != null) callback.accept(list);
            });
        });
    }

    /**
     * Loads getHistory async, then invokes callback on main thread.
     */
    public static void getHistoryAsync(GUIBAN plugin, UUID uuid, Consumer<List<Punishment>> callback) {
        StorageProvider delegate = getDelegate(plugin);
        CachedStorageProvider cached = getCached(plugin);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Punishment> list = delegate.getHistory(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (cached != null) cached.updateCacheHistory(uuid, list);
                if (callback != null) callback.accept(list);
            });
        });
    }

    private static StorageProvider getDelegate(GUIBAN plugin) {
        StorageProvider s = plugin.getStorage();
        if (s instanceof CachedStorageProvider) {
            return ((CachedStorageProvider) s).getDelegate();
        }
        return s;
    }

    private static CachedStorageProvider getCached(GUIBAN plugin) {
        StorageProvider s = plugin.getStorage();
        return s instanceof CachedStorageProvider ? (CachedStorageProvider) s : null;
    }
}

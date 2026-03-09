package com.enterprise.guiban;

import com.enterprise.guiban.command.GuiBanCommand;
import com.enterprise.guiban.listener.ChatListener;
import com.enterprise.guiban.listener.GUIListener;
import com.enterprise.guiban.listener.JailListener;
import com.enterprise.guiban.listener.JoinListener;
import com.enterprise.guiban.listener.ReasonInputListener;
import com.enterprise.guiban.storage.CachedStorageProvider;
import com.enterprise.guiban.storage.MySQLProvider;
import com.enterprise.guiban.ui.ReasonPending;
import com.enterprise.guiban.utils.MessageHelper;
import com.enterprise.guiban.utils.RateLimiter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.enterprise.guiban.storage.SQLiteProvider;
import com.enterprise.guiban.storage.StorageProvider;
import com.enterprise.guiban.storage.YamlProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GUIBAN extends JavaPlugin {

    private StorageProvider storage;
    private com.enterprise.guiban.ui.GUIHandler guiHandler;
    private final Map<UUID, ReasonPending> reasonPendingByPlayer = new ConcurrentHashMap<>();
    private RateLimiter rateLimiter;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        MessageHelper.init(this);
        setupStorage();
        rateLimiter = new RateLimiter(getConfig().getInt("rate-limit", 0));
        this.guiHandler = new com.enterprise.guiban.ui.GUIHandler(this);

        getCommand("guiban").setExecutor(new GuiBanCommand(this));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.enterprise.guiban.placeholders.GUIBanPlaceholders(this).register();
        }
        getServer().getPluginManager().registerEvents(new ReasonInputListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(storage), this);
        getServer().getPluginManager().registerEvents(new JoinListener(storage, this), this);
        getServer().getPluginManager().registerEvents(new JailListener(storage, this), this);
        getServer().getPluginManager().registerEvents(new com.enterprise.guiban.listener.GUIListener(this), this);

        com.enterprise.guiban.utils.AuditLogger.init(this);
        scheduleExpiryTask();

        String storageType = getConfig().getString("storage.type", "YAML").toUpperCase();
        
        getLogger().info("[]=====[Enabling GUIBan]=====[]");
        getLogger().info("| Information:");
        getLogger().info("|   Name: GUIBan");
        getLogger().info("|   Developer: Mathew Pittard");
        getLogger().info("|   Version: " + getDescription().getVersion());
        getLogger().info("|   Storage: " + storageType);
        getLogger().info("| Support:");
        getLogger().info("|   Github: https://github.com/TVDOfficial/GuiBan");
        getLogger().info("[]================================[]");
    }

    private void setupStorage() {
        String type = getConfig().getString("storage.type", "YAML");
        StorageProvider backend;
        if (type.equalsIgnoreCase("SQLITE")) {
            backend = new SQLiteProvider(this);
        } else if (type.equalsIgnoreCase("MYSQL")) {
            backend = new MySQLProvider(this);
        } else {
            backend = new YamlProvider(this);
        }
        backend.initialize();
        storage = new CachedStorageProvider(this, backend);
    }

    public void reloadPluginConfigs() {
        reloadConfig();
        saveResource("messages.yml", false);
        MessageHelper.load();
        if (guiHandler != null) guiHandler.loadConfig();
        rateLimiter = new RateLimiter(getConfig().getInt("rate-limit", 0));
    }

    public StorageProvider getStorage() {
        return storage;
    }

    public com.enterprise.guiban.ui.GUIHandler getGuiHandler() {
        return guiHandler;
    }

    public Map<UUID, ReasonPending> getReasonPendingByPlayer() {
        return reasonPendingByPlayer;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    private void scheduleExpiryTask() {
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> storage.cleanupExpired(), 20 * 60, 20 * 60);
    }
}
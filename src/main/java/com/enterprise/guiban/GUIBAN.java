package com.enterprise.guiban;

import com.enterprise.guiban.command.GuiBanCommand;
import com.enterprise.guiban.listener.ChatListener;
import com.enterprise.guiban.listener.GUIListener;
import com.enterprise.guiban.listener.JailListener;
import com.enterprise.guiban.listener.JoinListener;
import com.enterprise.guiban.storage.MySQLProvider;
import com.enterprise.guiban.storage.SQLiteProvider;
import com.enterprise.guiban.storage.StorageProvider;
import com.enterprise.guiban.storage.YamlProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GUIBAN extends JavaPlugin {

    private StorageProvider storage;
    private com.enterprise.guiban.ui.GUIHandler guiHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupStorage();
        this.guiHandler = new com.enterprise.guiban.ui.GUIHandler(this);

        getCommand("guiban").setExecutor(new GuiBanCommand(this));

        // Pass storage, NOT plugin
        getServer().getPluginManager().registerEvents(new ChatListener(storage), this);
        getServer().getPluginManager().registerEvents(new JoinListener(storage), this);
        getServer().getPluginManager().registerEvents(new JailListener(storage), this);
        getServer().getPluginManager().registerEvents(new com.enterprise.guiban.listener.GUIListener(this), this);

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
        if (type.equalsIgnoreCase("SQLITE")) {
            storage = new SQLiteProvider(this);
        } else if (type.equalsIgnoreCase("MYSQL")) {
            storage = new MySQLProvider(this);
        } else {
            storage = new YamlProvider(this);
        }
        storage.initialize();
    }

    public void reloadPluginConfigs() {
        reloadConfig();
        if (guiHandler != null) {
            guiHandler.loadConfig();
        }
    }

    public StorageProvider getStorage() {
        return storage;
    }

    public com.enterprise.guiban.ui.GUIHandler getGuiHandler() {
        return guiHandler;
    }
}
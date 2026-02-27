package com.redpockets.config;

import com.redpockets.RedPocketsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * 配置管理器
 * 管理所有配置文件的加载和访问
 */
public class ConfigManager {

    private final RedPocketsPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration databaseConfig;

    public ConfigManager(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    /**
     * 加载所有配置文件
     */
    private void loadConfigs() {
        // 保存默认配置
        plugin.saveDefaultConfig();
        plugin.saveResource("database.yml", false);

        // 加载配置
        config = plugin.getConfig();
        databaseConfig = loadDatabaseConfig();
    }

    /**
     * 加载数据库配置
     */
    private FileConfiguration loadDatabaseConfig() {
        File databaseFile = new File(plugin.getDataFolder(), "database.yml");
        if (!databaseFile.exists()) {
            plugin.saveResource("database.yml", false);
        }
        return YamlConfiguration.loadConfiguration(databaseFile);
    }

    /**
     * 重新加载配置文件
     */
    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        databaseConfig = loadDatabaseConfig();
    }

    // ==================== 主配置访问 ====================

    public double getRedPocketMaxAmount() {
        return config.getDouble("redpocket.max-amount", 1000000.0);
    }

    public double getRedPocketMinAmount() {
        return config.getDouble("redpocket.min-amount", 0.01);
    }

    public int getMaxRedPockets() {
        return config.getInt("redpocket.max-redpockets", 100);
    }

    public long getExpirationTime() {
        return config.getLong("redpocket.expiration-time", 86400);
    }

    public String getTitleColor() {
        return config.getString("redpocket.title-color", "&6");
    }

    public String getContentColor() {
        return config.getString("redpocket.content-color", "&e");
    }

    public boolean isGUIEnabled() {
        return config.getBoolean("gui.enabled", true);
    }

    // ==================== 数据库配置访问 ====================

    public String getDatabaseType() {
        return databaseConfig.getString("type", "sqlite");
    }

    public String getMySQLHost() {
        return databaseConfig.getString("mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return databaseConfig.getInt("mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return databaseConfig.getString("mysql.database", "redpockets");
    }

    public String getMySQLUsername() {
        return databaseConfig.getString("mysql.username", "root");
    }

    public String getMySQLPassword() {
        return databaseConfig.getString("mysql.password", "");
    }

    public String getSQLiteFile() {
        return databaseConfig.getString("sqlite.file", "redpockets.db");
    }

    public int getConnectionPoolSize() {
        return databaseConfig.getInt("connection-pool.maximum-pool-size", 10);
    }

    public int getMinimumIdle() {
        return databaseConfig.getInt("connection-pool.minimum-idle", 5);
    }

    public long getConnectionTimeout() {
        return databaseConfig.getLong("connection-pool.connection-timeout", 30000);
    }

    public boolean isAutoBackupEnabled() {
        return databaseConfig.getBoolean("backup.enabled", false);
    }
}

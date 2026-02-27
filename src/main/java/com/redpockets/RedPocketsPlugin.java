package com.redpockets;

import org.bukkit.plugin.java.JavaPlugin;

import com.redpockets.command.RedPocketAdminCommand;
import com.redpockets.command.RedPocketCommand;
import com.redpockets.config.ConfigManager;
import com.redpockets.config.MessageManager;
import com.redpockets.database.DatabaseManager;
import com.redpockets.economy.EconomyManager;
import com.redpockets.gui.CoinRedPocketGUI;
import com.redpockets.gui.ItemRedPocketGUI;
import com.redpockets.gui.RedPocketCreateGUI;
import com.redpockets.gui.StorageGUI;
import com.redpockets.listener.ChatClickListener;
import com.redpockets.listener.ChatInputListener;
import com.redpockets.listener.GUIListener;
import com.redpockets.logging.PluginLogger;
import com.redpockets.manager.ChatInputManager;
import com.redpockets.manager.GUIManager;
import com.redpockets.manager.ItemEditStorageManager;
import com.redpockets.manager.ItemRedPocketPreviewManager;
import com.redpockets.manager.RedPocketManager;
import com.redpockets.scheduler.FoliaScheduler;

/**
 * RedPockets 插件主类
 */
public class RedPocketsPlugin extends JavaPlugin {

    private static RedPocketsPlugin instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private RedPocketManager redPocketManager;
    private EconomyManager economyManager;
    private GUIManager guiManager;
    private ChatInputManager chatInputManager;
    private ItemEditStorageManager itemEditStorageManager;
    private ItemRedPocketPreviewManager previewManager;
    private PluginLogger pluginLogger;
    private FoliaScheduler scheduler;

    // GUI 组件
    private RedPocketCreateGUI redPocketCreateGUI;
    private CoinRedPocketGUI coinRedPocketGUI;
    private ItemRedPocketGUI itemRedPocketGUI;
    private StorageGUI storageGUI;

    // 监听器
    private GUIListener guiListener;
    private ChatInputListener chatInputListener;
    private ChatClickListener chatClickListener;

    @Override
    public void onEnable() {
        instance = this;

        // 初始化日志系统
        pluginLogger = new PluginLogger(this);
        pluginLogger.info("正在启动 RedPockets 插件...");

        // 初始化调度器
        scheduler = new FoliaScheduler(this);

        // 初始化配置管理器
        configManager = new ConfigManager(this);

        // 初始化消息管理器
        messageManager = new MessageManager(this);

        // 初始化数据库管理器
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            pluginLogger.info("数据库连接成功！");
        } catch (Exception e) {
            pluginLogger.severe("数据库初始化失败！");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 初始化红包管理器
        redPocketManager = new RedPocketManager(this);

        // 初始化经济管理器
        economyManager = new EconomyManager(this);
        economyManager.initialize();

        // 初始化物品编辑存储管理器
        itemEditStorageManager = new ItemEditStorageManager(this);
        itemEditStorageManager.initialize();

        // 初始化物品红包预览管理器
        previewManager = new ItemRedPocketPreviewManager(this);
        previewManager.initialize();

        // 初始化GUI管理器
        guiManager = new GUIManager(this);

        // 初始化聊天输入管理器
        chatInputManager = new ChatInputManager(this);
        chatInputListener = new ChatInputListener(this);

        // 初始化聊天点击监听器
        chatClickListener = new ChatClickListener(this);

        // 初始化GUI组件
        redPocketCreateGUI = new RedPocketCreateGUI(this);
        coinRedPocketGUI = new CoinRedPocketGUI(this);
        itemRedPocketGUI = new ItemRedPocketGUI(this);
        storageGUI = new StorageGUI(this);

        // 初始化GUI监听器
        guiListener = new GUIListener(this);

        // 注册命令
        registerCommands();

        // 注册事件监听器
        registerListeners();

        pluginLogger.info("RedPockets 插件启动成功！版本: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        pluginLogger.info("正在关闭 RedPockets 插件...");

        // 关闭GUI
        if (guiManager != null) {
            guiManager.closeAllGUIs();
        }

        // 关闭预览管理器
        if (previewManager != null) {
            previewManager.shutdown();
        }

        // 保存数据
        if (databaseManager != null) {
            databaseManager.close();
            pluginLogger.info("数据库连接已关闭。");
        }

        pluginLogger.info("RedPockets 插件已关闭。");
    }

    /**
     * 注册命令
     */
    private void registerCommands() {
        getCommand("redpocket").setExecutor(new RedPocketCommand(this));
        getCommand("redpocketadmin").setExecutor(new RedPocketAdminCommand(this));
        pluginLogger.info("命令已注册。");
    }

    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(chatInputListener, this);
        getServer().getPluginManager().registerEvents(chatClickListener, this);
        pluginLogger.info("事件监听器已注册。");
    }

    // ==================== Getters ====================

    public static RedPocketsPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RedPocketManager getRedPocketManager() {
        return redPocketManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public PluginLogger getPluginLogger() {
        return pluginLogger;
    }

    public FoliaScheduler getScheduler() {
        return scheduler;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ItemEditStorageManager getItemEditStorageManager() {
        return itemEditStorageManager;
    }

    public ItemRedPocketPreviewManager getPreviewManager() {
        return previewManager;
    }

    // GUI Getters
    public RedPocketCreateGUI getRedPocketCreateGUI() {
        return redPocketCreateGUI;
    }

    public CoinRedPocketGUI getCoinRedPocketGUI() {
        return coinRedPocketGUI;
    }

    public ItemRedPocketGUI getItemRedPocketGUI() {
        return itemRedPocketGUI;
    }

    public StorageGUI getStorageGUI() {
        return storageGUI;
    }

    public com.redpockets.listener.ChatClickListener getChatClickListener() {
        return chatClickListener;
    }
}

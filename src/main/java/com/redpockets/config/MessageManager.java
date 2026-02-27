package com.redpockets.config;

import com.redpockets.RedPocketsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息管理器
 * 管理多语言消息的加载和访问
 */
public class MessageManager {

    private final RedPocketsPlugin plugin;
    private final Map<String, FileConfiguration> messageConfigs;

    public MessageManager(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.messageConfigs = new HashMap<>();
        loadMessages();
    }

    /**
     * 加载所有语言消息文件
     */
    private void loadMessages() {
        // 保存默认语言文件
        plugin.saveResource("messages_zh.yml", false);
        plugin.saveResource("messages_en.yml", false);

        // 加载语言文件
        messageConfigs.put("zh", loadMessageFile("messages_zh.yml"));
        messageConfigs.put("en", loadMessageFile("messages_en.yml"));
    }

    /**
     * 加载单个消息文件
     */
    private FileConfiguration loadMessageFile(String fileName) {
        File messageFile = new File(plugin.getDataFolder(), fileName);
        if (!messageFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(messageFile);
    }

    /**
     * 获取消息
     */
    public String getMessage(String key) {
        String lang = plugin.getConfig().getString("language.default", "zh");
        FileConfiguration config = messageConfigs.get(lang);

        if (config == null) {
            plugin.getPluginLogger().warning("语言配置不存在: " + lang + "，使用默认中文");
            config = messageConfigs.get("zh");
        }

        if (config == null) {
            return "消息未找到: " + key;
        }

        String message = config.getString(key);
        if (message == null) {
            return "消息未找到: " + key;
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * 获取带占位符的消息
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return message;
    }

    /**
     * 获取消息（便捷方法，支持可变参数）
     */
    public String getMessage(String key, String... args) {
        String message = getMessage(key);

        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i]);
        }

        return message;
    }

    /**
     * 获取消息列表
     */
    public List<String> getMessageList(String key) {
        String lang = plugin.getConfig().getString("language.default", "zh");
        FileConfiguration config = messageConfigs.get(lang);

        if (config == null) {
            config = messageConfigs.get("zh");
        }

        if (config == null) {
            return List.of("消息未找到: " + key);
        }

        List<String> messages = config.getStringList(key);
        if (messages.isEmpty()) {
            return List.of("消息未找到: " + key);
        }

        // 转换颜色代码
        messages.replaceAll(msg -> ChatColor.translateAlternateColorCodes('&', msg));

        return messages;
    }

    /**
     * 获取消息前缀
     */
    public String getPrefix() {
        return getMessage("messages.prefix", "&6[红包] &r");
    }

    /**
     * 发送消息给玩家
     */
    public void sendMessage(org.bukkit.entity.Player player, String key) {
        player.sendMessage(getPrefix() + getMessage(key));
    }

    /**
     * 发送带占位符的消息给玩家
     */
    public void sendMessage(org.bukkit.entity.Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(getPrefix() + getMessage(key, placeholders));
    }

    /**
     * 发送消息给玩家（便捷方法）
     */
    public void sendMessage(org.bukkit.entity.Player player, String key, String... args) {
        player.sendMessage(getPrefix() + getMessage(key, args));
    }

    /**
     * 发送成功消息
     */
    public void sendSuccess(org.bukkit.entity.Player player, String key) {
        String color = plugin.getConfig().getString("messages.colors.success", "&a");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', color) + getMessage(key));
    }

    /**
     * 发送错误消息
     */
    public void sendError(org.bukkit.entity.Player player, String key) {
        String color = plugin.getConfig().getString("messages.colors.error", "&c");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', color) + getMessage(key));
    }

    /**
     * 发送信息消息
     */
    public void sendInfo(org.bukkit.entity.Player player, String key) {
        String color = plugin.getConfig().getString("messages.colors.info", "&e");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', color) + getMessage(key));
    }

    /**
     * 发送警告消息
     */
    public void sendWarning(org.bukkit.entity.Player player, String key) {
        String color = plugin.getConfig().getString("messages.colors.warning", "&6");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', color) + getMessage(key));
    }

    /**
     * 发送成功消息（带占位符）
     */
    public void sendSuccess(org.bukkit.entity.Player player, String key, Map<String, String> placeholders) {
        String color = plugin.getConfig().getString("messages.colors.success", "&a");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', color) + getMessage(key, placeholders));
    }

    /**
     * 发送错误消息（带占位符）
     */
    public void sendError(org.bukkit.entity.Player player, String key, Map<String, String> placeholders) {
        String color = plugin.getConfig().getString("messages.colors.error", "&c");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', color) + getMessage(key, placeholders));
    }

    /**
     * 发送信息消息（带占位符）
     */
    public void sendInfo(org.bukkit.entity.Player player, String key, Map<String, String> placeholders) {
        String color = plugin.getConfig().getString("messages.colors.info", "&e");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', color) + getMessage(key, placeholders));
    }

    /**
     * 发送警告消息（带占位符）
     */
    public void sendWarning(org.bukkit.entity.Player player, String key, Map<String, String> placeholders) {
        String color = plugin.getConfig().getString("messages.colors.warning", "&6");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', color) + getMessage(key, placeholders));
    }
}

package com.redpockets.logging;

import com.redpockets.RedPocketsPlugin;
import org.bukkit.Bukkit;

/**
 * 插件日志系统
 * 提供结构化的日志输出
 */
public class PluginLogger {

    private final RedPocketsPlugin plugin;
    private final String prefix;
    private boolean debugEnabled;

    public PluginLogger(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.prefix = ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("messages.prefix", "&6[红包] &r"));
        this.debugEnabled = plugin.getConfig().getString("logging.level", "INFO").equalsIgnoreCase("DEBUG");
    }

    /**
     * 设置调试模式
     */
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
    }

    /**
     * 信息日志
     */
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    /**
     * 警告日志
     */
    public void warning(String message) {
        plugin.getLogger().warning(message);
    }

    /**
     * 错误日志
     */
    public void severe(String message) {
        plugin.getLogger().severe(message);
    }

    /**
     * 调试日志
     */
    public void debug(String message) {
        if (debugEnabled) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * 发送消息给玩家（带前缀）
     */
    public void sendMessage(org.bukkit.entity.Player player, String message) {
        player.sendMessage(prefix + message);
    }

    /**
     * 广播消息
     */
    public void broadcast(String message) {
        Bukkit.broadcastMessage(prefix + message);
    }

    /**
     * ChatColor 工具方法
     */
    private static class ChatColor {
        public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
            return org.bukkit.ChatColor.translateAlternateColorCodes(altColorChar, textToTranslate);
        }
    }
}

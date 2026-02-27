package com.redpockets.manager;

import java.util.function.Consumer;

import org.bukkit.entity.Player;

import com.redpockets.RedPocketsPlugin;
import com.redpockets.listener.ChatInputListener;

/**
 * 聊天输入管理器
 * 对外提供等待玩家文本输入的接口，内部使用单一的 ChatInputListener 处理事件。
 */
public class ChatInputManager {

    private final ChatInputListener listener;

    public ChatInputManager(RedPocketsPlugin plugin) {
        this.listener = new ChatInputListener(plugin);
        // 必须注册监听器，否则 AsyncPlayerChatEvent 永远不会回调
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    public void waitForInput(Player player, Consumer<String> callback) {
        listener.waitForInput(player, callback);
    }

    public void cancelInput(Player player) {
        listener.cancelInput(player);
    }
}

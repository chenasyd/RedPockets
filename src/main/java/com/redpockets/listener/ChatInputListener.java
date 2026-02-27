package com.redpockets.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.redpockets.RedPocketsPlugin;

/**
 * 聊天输入监听器
 * 通过 ChatInputManager 创建并注册。不要在其他地方单独 new 一个实例，
 * 否则会出现两个不同的 pendingInputs 导致回调永远不会触发。
 */
public class ChatInputListener implements Listener {

    private final RedPocketsPlugin plugin;
    private final Map<Player, Consumer<String>> pendingInputs;

    public ChatInputListener(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.pendingInputs = new HashMap<>();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = pendingInputs.get(player);

        if (callback == null) return;

        event.setCancelled(true); // 取消聊天消息

        String message = event.getMessage();
        if (message.equalsIgnoreCase("cancel")) {
            pendingInputs.remove(player);
            // 在主线程发送提示
            plugin.getScheduler().runForEntity(player, () ->
                plugin.getMessageManager().sendInfo(player, "chat_input.cancelled"));
            return;
        }

        pendingInputs.remove(player);
        plugin.getScheduler().runForEntity(player, () -> callback.accept(message));
    }

    /**
     * 等待玩家输入
     */
    public void waitForInput(Player player, Consumer<String> callback) {
        pendingInputs.put(player, callback);

        // 设置超时（30秒 = 600 ticks）- 使用实体调度器
        plugin.getScheduler().runForEntityLater(player, () -> {
            if (pendingInputs.containsKey(player)) {
                pendingInputs.remove(player);
                plugin.getMessageManager().sendWarning(player, "chat_input.timeout");
            }
        }, 600L);
    }

    /**
     * 取消玩家的待输入
     */
    public void cancelInput(Player player) {
        pendingInputs.remove(player);
    }
}

package com.redpockets.listener;

import com.redpockets.RedPocketsPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * 聊天点击监听器
 * 处理玩家点击聊天栏中的抢红包链接
 */
public class ChatClickListener implements Listener {

    private final RedPocketsPlugin plugin;

    public ChatClickListener(RedPocketsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // 检查是否是抢红包命令：/grab <红包ID>
        if (message.toLowerCase().startsWith("/grab ") || message.toLowerCase().startsWith("/rp grab ")) {
            event.setCancelled(true);

            // 使用正则表达式正确解析命令参数，处理红包ID中可能包含空格的情况
            String[] parts = message.split("\\s+", 3);
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                // 参数不足或红包ID为空时，向玩家发送错误提示
                plugin.getMessageManager().sendError(player, "commands.grab.usage");
                return;
            }

            String redPocketId = parts[1].trim();
            if (redPocketId.isEmpty()) {
                plugin.getMessageManager().sendError(player, "commands.grab.invalid_id");
                return;
            }

            tryGrabRedPocket(player, redPocketId);
        }

        // 检查是否是预览红包命令：/redpocket preview <红包ID>
        if (message.toLowerCase().startsWith("/redpocket preview ") || message.toLowerCase().startsWith("/rp preview ")) {
            event.setCancelled(true);

            String[] parts = message.split("\\s+", 4);
            if (parts.length < 3 || parts[2].trim().isEmpty()) {
                return;
            }

            String redPocketId = parts[2].trim();
            if (!redPocketId.isEmpty()) {
                // 打开预览GUI
                plugin.getPreviewManager().openPreviewGUI(player, redPocketId);
            }
        }
    }

    /**
     * 尝试抢红包
     */
    private void tryGrabRedPocket(Player player, String redPocketId) {
        var redPocket = plugin.getRedPocketManager().getRedPocket(redPocketId);
        
        // 如果红包不存在，发送失败消息
        if (redPocket == null) {
            plugin.getMessageManager().sendError(player, "commands.grab.failed");
            return;
        }

        // 如果是物品红包，不在这里发送成功消息（grabItemRedPocket已经发送）
        if (redPocket.getType() == com.redpockets.model.RedPocket.RedPocketType.ITEM) {
            plugin.getRedPocketManager().grabRedPocketWithPayment(redPocketId, player);
            return;
        }

        // 金币红包：发送成功消息
        var amountOpt = plugin.getRedPocketManager().grabRedPocketWithPayment(redPocketId, player);

        if (amountOpt.isPresent()) {
            double amount = amountOpt.get();

            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("amount", String.valueOf(amount));
            plugin.getMessageManager().sendSuccess(player, "commands.grab.success", placeholders);
        } else {
            plugin.getMessageManager().sendError(player, "commands.grab.failed");
        }
    }

    /**
     * 创建可点击的抢红包文本组件
     */
    public TextComponent createClickableGrabText(String redPocketId) {
        String clickText = plugin.getMessageManager().getMessage("gui.redpocket.grab.click_text");
        String hoverText = plugin.getMessageManager().getMessage("gui.redpocket.grab.hover_text");

        // 防止消息键不存在时返回空值
        if (clickText == null || clickText.isEmpty()) {
            clickText = "[点击这里抢红包]";
        }
        if (hoverText == null || hoverText.isEmpty()) {
            hoverText = "点击抢取这个红包";
        }

        TextComponent textComponent = new TextComponent(clickText);
        textComponent.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        textComponent.setBold(true);

        // 设置点击事件：执行命令 /grab <红包ID>
        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/grab " + redPocketId));

        // 设置悬停提示（使用新API避免过时警告）
        textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(new ComponentBuilder(hoverText)
                .color(net.md_5.bungee.api.ChatColor.YELLOW)
                .create())));

        return textComponent;
    }
}

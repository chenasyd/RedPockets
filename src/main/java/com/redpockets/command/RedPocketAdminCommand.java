package com.redpockets.command;

import com.redpockets.RedPocketsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * 红包管理员命令处理器
 */
public class RedPocketAdminCommand implements CommandExecutor {

    private final RedPocketsPlugin plugin;

    public RedPocketAdminCommand(RedPocketsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("redpockets.admin")) {
            plugin.getMessageManager().sendError((Player) sender, "commands.admin.no_permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;

            case "delete":
                handleDelete(sender, args);
                break;

            case "reload":
                handleReload(sender);
                break;

            case "stats":
                handleStats(sender);
                break;

            default:
                sendHelp(sender);
        }

        return true;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        plugin.getMessageManager().sendMessage((Player) sender, "commands.admin.help.header");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("command", "/redpocketadmin delete <id>");
        plugin.getMessageManager().sendMessage((Player) sender, "commands.admin.help.delete", placeholders);

        placeholders.clear();
        placeholders.put("command", "/redpocketadmin reload");
        plugin.getMessageManager().sendMessage((Player) sender, "commands.admin.help.reload", placeholders);

        placeholders.clear();
        placeholders.put("command", "/redpocketadmin stats");
        plugin.getMessageManager().sendMessage((Player) sender, "commands.admin.help.stats", placeholders);

        plugin.getMessageManager().sendMessage((Player) sender, "commands.admin.help.footer");
    }

    /**
     * 处理删除红包
     */
    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendError((Player) sender, "commands.admin.delete.usage");
            return;
        }

        String redPocketId = args[1];
        plugin.getRedPocketManager().deleteRedPocket(redPocketId);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", redPocketId);
        plugin.getMessageManager().sendSuccess((Player) sender, "commands.admin.delete.success", placeholders);
    }

    /**
     * 处理重载配置
     */
    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reloadConfigs();
        plugin.getMessageManager().sendSuccess((Player) sender, "commands.admin.reload.success");
    }

    /**
     * 处理统计信息
     */
    private void handleStats(CommandSender sender) {
        plugin.getMessageManager().sendInfo((Player) sender, "commands.admin.stats.not_implemented");
    }
}

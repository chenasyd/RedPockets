package com.redpockets.command;

import com.redpockets.RedPocketsPlugin;
import com.redpockets.model.RedPocket;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 红包主命令处理器
 */
public class RedPocketCommand implements CommandExecutor {

    private final RedPocketsPlugin plugin;

    public RedPocketCommand(RedPocketsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(player);
                break;

            case "create":
                handleCreate(player, args);
                break;

            case "random":
                handleRandom(player, args);
                break;

            case "average":
                handleAverage(player, args);
                break;

            case "grab":
                handleGrab(player, args);
                break;

            case "check":
                handleCheck(player, args);
                break;

            case "list":
                handleList(player);
                break;

            default:
                sendHelp(player);
        }

        return true;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(Player player) {
        plugin.getMessageManager().sendMessage(player, "commands.help.header");

        Map<String, String> mainCommand = new HashMap<>();
        mainCommand.put("command", "/redpocket");
        plugin.getMessageManager().sendMessage(player, "commands.help.main_command", mainCommand);

        Map<String, String> create = new HashMap<>();
        create.put("command", "/redpocket create <amount> <count> [note]");
        plugin.getMessageManager().sendMessage(player, "commands.help.create", create);

        Map<String, String> random = new HashMap<>();
        random.put("command", "/redpocket random <amount> <count> [note]");
        plugin.getMessageManager().sendMessage(player, "commands.help.random", random);

        Map<String, String> average = new HashMap<>();
        average.put("command", "/redpocket average <amount> <count> [note]");
        plugin.getMessageManager().sendMessage(player, "commands.help.average", average);

        Map<String, String> grab = new HashMap<>();
        grab.put("command", "/redpocket grab <id>");
        plugin.getMessageManager().sendMessage(player, "commands.help.grab", grab);

        Map<String, String> check = new HashMap<>();
        check.put("command", "/redpocket check <id>");
        plugin.getMessageManager().sendMessage(player, "commands.help.check", check);

        Map<String, String> list = new HashMap<>();
        list.put("command", "/redpocket list");
        plugin.getMessageManager().sendMessage(player, "commands.help.list", list);

        plugin.getMessageManager().sendMessage(player, "commands.help.footer");
    }

    /**
     * 处理创建平分红包
     */
    private void handleCreate(Player player, String[] args) {
        // 打开类型选择GUI
        plugin.getRedPocketCreateGUI().openTypeSelectGUI(player);
    }

    /**
     * 处理创建随机红包
     */
    private void handleRandom(Player player, String[] args) {
        if (args.length < 3) {
            Map<String, String> usage = new HashMap<>();
            usage.put("usage", "/redpocket random <amount> <count> [note]");
            plugin.getMessageManager().sendError(player, "commands.random.usage", usage);
            return;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            int count = Integer.parseInt(args[2]);
            String note = args.length > 3 ? String.join(" ", args).substring(args[0].length() + args[1].length() + args[2].length() + 3) : "";

            if (amount <= 0 || count <= 0) {
                plugin.getMessageManager().sendError(player, "commands.random.invalid_params");
                return;
            }

            // 创建随机红包（包含经济验证和扣除）
            RedPocket redPocket = plugin.getRedPocketManager().createRedPocketWithValidation(
                player,
                RedPocket.RedPocketType.RANDOM,
                amount,
                count,
                note
            );

            if (redPocket == null) {
                // 创建失败（经济验证失败或扣除失败）
                return;
            }

            Map<String, String> success = new HashMap<>();
            success.put("id", redPocket.getId());
            success.put("amount", String.valueOf(amount));
            success.put("count", String.valueOf(count));
            plugin.getMessageManager().sendSuccess(player, "commands.random.success", success);

        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendError(player, "commands.random.invalid_number");
        }
    }

    /**
     * 处理创建平分红包
     */
    private void handleAverage(Player player, String[] args) {
        handleCreate(player, args); // 与 create 相同
    }

    /**
     * 处理抢红包
     */
    private void handleGrab(Player player, String[] args) {
        if (args.length < 2) {
            Map<String, String> usage = new HashMap<>();
            usage.put("usage", "/redpocket grab <id>");
            plugin.getMessageManager().sendError(player, "commands.grab.usage", usage);
            return;
        }

        String redPocketId = args[1];
        java.util.Optional<Double> amount = plugin.getRedPocketManager().grabRedPocketWithPayment(redPocketId, player);

        if (amount.isPresent()) {
            Map<String, String> success = new HashMap<>();
            success.put("amount", String.valueOf(amount.get()));
            plugin.getMessageManager().sendSuccess(player, "commands.grab.success", success);
        } else {
            plugin.getMessageManager().sendError(player, "commands.grab.failed");
        }
    }

    /**
     * 处理查看红包
     */
    private void handleCheck(Player player, String[] args) {
        if (args.length < 2) {
            Map<String, String> usage = new HashMap<>();
            usage.put("usage", "/redpocket check <id>");
            plugin.getMessageManager().sendError(player, "commands.check.usage", usage);
            return;
        }

        String redPocketId = args[1];
        RedPocket redPocket = plugin.getRedPocketManager().getRedPocket(redPocketId);

        if (redPocket == null) {
            plugin.getMessageManager().sendError(player, "commands.check.not_found");
            return;
        }

        // 显示红包信息
        plugin.getMessageManager().sendMessage(player, "commands.check.info");
        plugin.getMessageManager().sendMessage(player, "commands.check.id", redPocket.getId());
        plugin.getMessageManager().sendMessage(player, "commands.check.type", redPocket.getType().name());
        plugin.getMessageManager().sendMessage(player, "commands.check.amount", String.valueOf(redPocket.getTotalAmount()));
        plugin.getMessageManager().sendMessage(player, "commands.check.count", String.valueOf(redPocket.getCount()));
        plugin.getMessageManager().sendMessage(player, "commands.check.note", redPocket.getNote() != null ? redPocket.getNote() : "无");
    }

    /**
     * 处理列出红包
     */
    private void handleList(Player player) {
        plugin.getMessageManager().sendInfo(player, "commands.list.not_implemented");
    }
}

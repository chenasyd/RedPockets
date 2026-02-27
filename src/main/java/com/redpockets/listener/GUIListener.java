package com.redpockets.listener;

import com.redpockets.RedPocketsPlugin;
import com.redpockets.gui.*;
import com.redpockets.manager.ItemRedPocketPreviewManager;
import com.redpockets.model.RedPocket;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * GUI 事件监听器
 */
public class GUIListener implements Listener {

    private final RedPocketsPlugin plugin;

    public GUIListener(RedPocketsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String guiType = plugin.getGUIManager().getGUIType(player);

        // 检查是否是预览GUI - 完全禁用所有操作
        if (event.getInventory().getHolder() instanceof ItemRedPocketPreviewManager.PreviewInventoryHolder) {
            event.setCancelled(true);
            return;
        }

        if (guiType == null) return;

        // 物品编辑 GUI 允许自由操作物品
        if (guiType.equals("item_edit")) {
            handleItemEditClick(event);
            return;
        }

        // 其他 GUI 取消操作
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        handleGUIClick(player, guiType, clicked, event.getSlot());
    }

    /**
     * 处理GUI点击事件
     */
    @SuppressWarnings("deprecation")
    private void handleGUIClick(Player player, String guiType, ItemStack clicked, int slot) {
        NBTItem nbt = new NBTItem(clicked);
        String action = nbt.getString("redpocket_action");

        if (action == null || action.isEmpty()) return;

        switch (guiType) {
            case "type_select":
                handleTypeSelectClick(player, action);
                break;

            case "coin_redpocket":
                handleCoinRedPocketClick(player, action, slot);
                break;

            case "item_redpocket":
                handleItemRedPocketClick(player, action);
                break;

            case "storage":
                handleStorageClick(player, slot);
                break;
        }
    }

    /**
     * 处理类型选择GUI点击
     */
    private void handleTypeSelectClick(Player player, String action) {
        switch (action) {
            case "create_coin":
                plugin.getCoinRedPocketGUI().openCoinRedPocketGUI(player);
                break;

            case "create_item":
                plugin.getItemRedPocketGUI().openItemRedPocketGUI(player);
                break;
        }
    }

    /**
     * 处理金币红包GUI点击
     */
    private void handleCoinRedPocketClick(Player player, String action, int slot) {
        CoinRedPocketGUI coinGUI = plugin.getCoinRedPocketGUI();

        switch (action) {
            case "set_amount":
                player.closeInventory();
                plugin.getMessageManager().sendMessage(player, "gui.create.coin.amount.input");
                plugin.getChatInputManager().waitForInput(player, input -> {
                    try {
                        double amount = Double.parseDouble(input);
                        if (amount <= 0) {
                            plugin.getMessageManager().sendError(player, "gui.create.coin.amount.invalid");
                        } else {
                            coinGUI.setPendingAmount(player, amount);
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("amount", String.valueOf(amount));
                            plugin.getMessageManager().sendMessage(player, "gui.create.coin.amount.success", placeholders);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getMessageManager().sendError(player, "gui.create.coin.amount.invalid_number");
                    }
                    plugin.getScheduler().runForEntityLater(player, () -> coinGUI.openCoinRedPocketGUI(player), 1L);
                });
                break;

            case "set_count":
                player.closeInventory();
                plugin.getMessageManager().sendMessage(player, "gui.create.coin.count.input");
                plugin.getChatInputManager().waitForInput(player, input -> {
                    try {
                        int count = Integer.parseInt(input);
                        if (count <= 0) {
                            plugin.getMessageManager().sendError(player, "gui.create.coin.count.invalid");
                        } else {
                            coinGUI.setPendingCount(player, count);
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("count", String.valueOf(count));
                            plugin.getMessageManager().sendMessage(player, "gui.create.coin.count.success", placeholders);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getMessageManager().sendError(player, "gui.create.coin.count.invalid_number");
                    }
                    plugin.getScheduler().runForEntityLater(player, () -> coinGUI.openCoinRedPocketGUI(player), 1L);
                });
                break;

            case "toggle_distribution":
                // 切换分配方式
                coinGUI.toggleDistributionType(player);
                Map<String, String> typePlaceholders = new HashMap<>();
                typePlaceholders.put("type", coinGUI.getDistributionType(player) == com.redpockets.model.RedPocket.RedPocketType.RANDOM ? "随机分配" : "平均分配");
                plugin.getMessageManager().sendSuccess(player, "gui.create.coin.distribution.changed", typePlaceholders);
                // 重新打开GUI显示更新后的状态
                coinGUI.openCoinRedPocketGUI(player);
                break;

            case "set_note":
                player.closeInventory();
                plugin.getMessageManager().sendMessage(player, "gui.create.coin.note.input");
                plugin.getChatInputManager().waitForInput(player, input -> {
                    // 保存备注
                    coinGUI.setPendingNote(player, input);
                    Map<String, String> notePlaceholders = new HashMap<>();
                    notePlaceholders.put("note", input);
                    plugin.getMessageManager().sendMessage(player, "gui.create.coin.note.success", notePlaceholders);
                    plugin.getScheduler().runForEntityLater(player, () -> coinGUI.openCoinRedPocketGUI(player), 1L);
                });
                break;

            case "confirm_send":
                double amount = coinGUI.getPendingAmount(player);
                int count = coinGUI.getPendingCount(player);
                String note = coinGUI.getPendingNote(player);

                if (amount <= 0) {
                    plugin.getMessageManager().sendError(player, "gui.create.coin.amount_not_set");
                    return;
                }

                if (count <= 0) {
                    plugin.getMessageManager().sendError(player, "gui.create.coin.count_not_set");
                    return;
                }

                // 获取当前选择的分配方式
                com.redpockets.model.RedPocket.RedPocketType distributionType = coinGUI.getDistributionType(player);

                // 创建红包（包含经济验证和扣除）
                RedPocket redPocket = plugin.getRedPocketManager().createRedPocketWithValidation(
                    player,
                    distributionType,
                    amount,
                    count,
                    note
                );

                if (redPocket == null) {
                    // 创建失败（经济验证失败或扣除失败）
                    return;
                }

                // 广播红包到所有在线玩家
                plugin.getRedPocketManager().broadcastRedPocket(redPocket);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("id", redPocket.getId());
                plugin.getMessageManager().sendSuccess(player, "gui.create.coin.confirm.success", placeholders);
                placeholders.clear();
                placeholders.put("id", redPocket.getId());
                placeholders.put("amount", String.valueOf(amount));
                placeholders.put("count", String.valueOf(count));
                plugin.getMessageManager().sendMessage(player, "commands.create.success", placeholders);

                coinGUI.clearPlayerData(player);
                player.closeInventory();
                break;
        }
    }

    /**
     * 处理物品红包GUI点击
     */
    private void handleItemRedPocketClick(Player player, String action) {
        ItemRedPocketGUI itemGUI = plugin.getItemRedPocketGUI();

        switch (action) {
            case "open_edit":
                itemGUI.openItemEditGUI(player);
                break;

            case "confirm_send":
                // 检查是否已发送（只读模式）
                if (itemGUI.isReadOnlyMode(player)) {
                    plugin.getMessageManager().sendError(player, "gui.create.item.already_sent");
                    return;
                }

                ItemStack[] items = itemGUI.getPlayerItems(player);
                if (items == null || items.length == 0 || isEmptyArray(items)) {
                    plugin.getMessageManager().sendError(player, "gui.create.item.no_items");
                    return;
                }

                // 统计非空物品数量
                int count = 0;
                for (ItemStack item : items) {
                    if (item != null && !item.getType().isAir()) {
                        count++;
                    }
                }

                if (count <= 0) {
                    plugin.getMessageManager().sendError(player, "gui.create.item.no_items");
                    return;
                }

                // 创建物品红包
                RedPocket redPocket = plugin.getRedPocketManager().createItemRedPocket(player, count, "");

                if (redPocket == null) {
                    return;
                }

                // 广播红包到所有在线玩家
                plugin.getRedPocketManager().broadcastRedPocket(redPocket);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("id", redPocket.getId());
                plugin.getMessageManager().sendSuccess(player, "gui.create.item.confirm.success", placeholders);
                placeholders.clear();
                placeholders.put("id", redPocket.getId());
                placeholders.put("count", String.valueOf(count));
                // 物品红包使用单独的成功消息（不显示金额）
                plugin.getMessageManager().sendMessage(player, "commands.create.success_item", placeholders);

                itemGUI.clearPlayerData(player);
                player.closeInventory();
                break;
        }
    }

    /**
     * 检查物品数组是否为空
     */
    private boolean isEmptyArray(ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 处理物品编辑 GUI 点击（支持自由操作物品）
     */
    private void handleItemEditClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemRedPocketGUI itemGUI = plugin.getItemRedPocketGUI();

        // 检查是否只读模式（已发送红包且未过期）
        if (itemGUI.isReadOnlyMode(player)) {
            // 只读模式下取消所有操作
            event.setCancelled(true);
            return;
        }

        // 正常模式：允许所有物品操作（0-53槽位均为编辑区）
        // 延迟自动保存
        if (event.getAction() != org.bukkit.event.inventory.InventoryAction.NOTHING) {
            plugin.getScheduler().runForEntityLater(player, () -> {
                itemGUI.autoSave(player, event.getInventory());
            }, 1L);
        }
    }

    /**
     * 处理储物间GUI点击
     */
    private void handleStorageClick(Player player, int slot) {
        // 允许玩家取出物品，不允许放入
        if (slot < 45) {
            ItemStack clicked = player.getOpenInventory().getTopInventory().getItem(slot);
            if (clicked != null) {
                StorageGUI storageGUI = plugin.getStorageGUI();
                // 移除物品并给予玩家
                player.getOpenInventory().getTopInventory().setItem(slot, null);
                player.getInventory().addItem(clicked.clone());
                storageGUI.removeFromStorage(player, slot);

                if (storageGUI.isStorageEmpty(player)) {
                    plugin.getMessageManager().sendSuccess(player, "gui.storage.empty");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String guiType = plugin.getGUIManager().getGUIType(player);

        if (guiType == null) return;

        plugin.getGUIManager().unregisterGUI(player);

        // 物品编辑GUI关闭时自动保存（持久化到数据库）并返回上级
        if (guiType.equals("item_edit")) {
            ItemRedPocketGUI itemGUI = plugin.getItemRedPocketGUI();
            itemGUI.autoSave(player, event.getInventory());
            // 延迟打开上级界面
            plugin.getScheduler().runForEntityLater(player, () -> {
                itemGUI.openItemRedPocketGUI(player);
            }, 1L);
            return;
        }

        // 储物间GUI关闭时保存
        if (guiType.equals("storage")) {
            StorageGUI storageGUI = plugin.getStorageGUI();
            storageGUI.saveStorage(player, event.getInventory());
        }
    }
}

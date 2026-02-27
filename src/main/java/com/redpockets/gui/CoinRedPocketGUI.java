package com.redpockets.gui;

import com.redpockets.RedPocketsPlugin;
import com.redpockets.model.RedPocket;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 金币红包创建GUI
 */
public class CoinRedPocketGUI {

    private final RedPocketsPlugin plugin;
    private final Map<Player, Double> pendingAmounts;
    private final Map<Player, Integer> pendingCounts;
    private final Map<Player, String> pendingNotes;
    private final Map<Player, RedPocket.RedPocketType> distributionTypes;

    public CoinRedPocketGUI(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.pendingAmounts = new HashMap<>();
        this.pendingCounts = new HashMap<>();
        this.pendingNotes = new HashMap<>();
        this.distributionTypes = new HashMap<>();
    }

    /**
     * 打开金币红包GUI (9x3)
     */
    public void openCoinRedPocketGUI(Player player) {
        plugin.getScheduler().runForEntity(player, () -> {
            String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getMessageManager().getMessage("gui.create.coin.title"));
            Inventory inv = Bukkit.createInventory(null, 27, title);

            fillCoinGUI(inv, player);

            player.openInventory(inv);
            plugin.getGUIManager().registerGUI(player, inv, "coin_redpocket");
        });
    }

    /**
     * 填充金币红包GUI
     */
    private void fillCoinGUI(Inventory inv, Player player) {
        // 清空中间区域
        for (int i = 9; i <= 17; i++) {
            inv.setItem(i, null);
        }

        // 第11槽位：设置金额
        ItemStack amountItem = createAmountItem(player);
        inv.setItem(11, amountItem);

        // 第12槽位：设置数量
        ItemStack countItem = createCountItem(player);
        inv.setItem(12, countItem);

        // 第13槽位：选择分配方式
        ItemStack distributionItem = createDistributionTypeItem(player);
        inv.setItem(13, distributionItem);

        // 第14槽位：输入备注（可选）
        ItemStack noteItem = createNoteItem(player);
        inv.setItem(14, noteItem);

        // 第15槽位：确认发送
        ItemStack confirmItem = createConfirmItem(player);
        inv.setItem(15, confirmItem);

        // 填充边框
        fillBorder(inv);
    }

    /**
     * 创建金额设置物品
     */
    private ItemStack createAmountItem(Player player) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();

        double amount = pendingAmounts.getOrDefault(player, 0.0);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));
        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.create.coin.amount.title", placeholders));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.create.coin.amount.lore");
        meta.setLore(lore);

        item.setItemMeta(meta);

        NBT.modify(item, nbt -> {
            nbt.setString("redpocket_action", "set_amount");
        });

        return item;
    }

    /**
     * 创建数量设置物品
     */
    private ItemStack createCountItem(Player player) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        int count = pendingCounts.getOrDefault(player, 1);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(count));
        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.create.coin.count.title", placeholders));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.create.coin.count.lore");
        meta.setLore(lore);

        item.setItemMeta(meta);

        NBT.modify(item, nbt -> {
            nbt.setString("redpocket_action", "set_count");
        });

        return item;
    }

    /**
     * 创建备注设置物品
     */
    private ItemStack createNoteItem(Player player) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();

        String note = pendingNotes.getOrDefault(player, "");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("note", note.isEmpty() ? "未设置" : note);
        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.create.coin.note.title", placeholders));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.create.coin.note.lore");
        meta.setLore(lore);

        item.setItemMeta(meta);

        NBT.modify(item, nbt -> {
            nbt.setString("redpocket_action", "set_note");
        });

        return item;
    }

    /**
     * 创建分配方式选择物品
     */
    private ItemStack createDistributionTypeItem(Player player) {
        RedPocket.RedPocketType currentType = distributionTypes.getOrDefault(player, RedPocket.RedPocketType.RANDOM);

        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();

        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.create.coin.distribution.title"));
        meta.setDisplayName(title);

        // 创建lore显示分配方式列表
        List<String> lore = new ArrayList<>();

        // 添加标题
        lore.add(ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.create.coin.distribution.header")));

        // 添加随机方式
        String randomName = plugin.getMessageManager().getMessage("gui.create.coin.distribution.random");
        String randomLore;
        if (currentType == RedPocket.RedPocketType.RANDOM) {
            randomLore = "&a✓ " + randomName;
        } else {
            randomLore = "&f  " + randomName;
        }
        lore.add(ChatColor.translateAlternateColorCodes('&', randomLore));

        // 添加平分方式
        String averageName = plugin.getMessageManager().getMessage("gui.create.coin.distribution.average");
        String averageLore;
        if (currentType == RedPocket.RedPocketType.AVERAGE) {
            averageLore = "&a✓ " + averageName;
        } else {
            averageLore = "&f  " + averageName;
        }
        lore.add(ChatColor.translateAlternateColorCodes('&', averageLore));

        meta.setLore(lore);
        item.setItemMeta(meta);

        NBT.modify(item, nbt -> {
            nbt.setString("redpocket_action", "toggle_distribution");
        });

        return item;
    }

    /**
     * 创建确认发送物品
     */
    private ItemStack createConfirmItem(Player player) {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.create.coin.confirm.title"));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.create.coin.confirm.lore");
        meta.setLore(lore);

        item.setItemMeta(meta);

        NBT.modify(item, nbt -> {
            nbt.setString("redpocket_action", "confirm_send");
        });

        return item;
    }

    /**
     * 填充边框
     */
    private void fillBorder(Inventory inv) {
        ItemStack glass = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17) {
                inv.setItem(i, glass.clone());
            }
        }
    }

    /**
     * 设置待发送金额
     */
    public void setPendingAmount(Player player, double amount) {
        pendingAmounts.put(player, amount);
    }

    /**
     * 获取待发送金额
     */
    public double getPendingAmount(Player player) {
        return pendingAmounts.getOrDefault(player, 0.0);
    }

    /**
     * 设置待发送数量
     */
    public void setPendingCount(Player player, int count) {
        pendingCounts.put(player, count);
    }

    /**
     * 获取待发送数量
     */
    public int getPendingCount(Player player) {
        return pendingCounts.getOrDefault(player, 1);
    }

    /**
     * 设置待发送备注
     */
    public void setPendingNote(Player player, String note) {
        pendingNotes.put(player, note);
    }

    /**
     * 获取待发送备注
     */
    public String getPendingNote(Player player) {
        return pendingNotes.getOrDefault(player, "");
    }

    /**
     * 清除玩家数据
     */
    public void clearPlayerData(Player player) {
        pendingAmounts.remove(player);
        pendingCounts.remove(player);
        pendingNotes.remove(player);
        distributionTypes.remove(player);
    }

    /**
     * 获取当前分配方式
     */
    public RedPocket.RedPocketType getDistributionType(Player player) {
        return distributionTypes.getOrDefault(player, RedPocket.RedPocketType.RANDOM);
    }

    /**
     * 切换分配方式
     */
    public void toggleDistributionType(Player player) {
        RedPocket.RedPocketType currentType = getDistributionType(player);
        if (currentType == RedPocket.RedPocketType.RANDOM) {
            distributionTypes.put(player, RedPocket.RedPocketType.AVERAGE);
        } else {
            distributionTypes.put(player, RedPocket.RedPocketType.RANDOM);
        }
    }
}

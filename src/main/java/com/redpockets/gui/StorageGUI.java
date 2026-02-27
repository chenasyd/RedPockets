package com.redpockets.gui;

import com.redpockets.RedPocketsPlugin;
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
 * 储物间GUI
 * 用于保存未领取完的物品红包
 */
public class StorageGUI {

    private final RedPocketsPlugin plugin;
    private final Map<Player, List<ItemStack>> playerStorage;

    public StorageGUI(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.playerStorage = new HashMap<>();
    }

    /**
     * 打开储物间GUI (9x6)
     */
    public void openStorageGUI(Player player) {
        plugin.getScheduler().runForEntity(player, () -> {
            String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getMessageManager().getMessage("gui.storage.title"));
            Inventory inv = Bukkit.createInventory(null, 54, title);

            // 加载物品
            loadStorageItems(inv, player);

            // 填充控制行
            fillControlRow(inv);

            player.openInventory(inv);
            plugin.getGUIManager().registerGUI(player, inv, "storage");
        });
    }

    /**
     * 加载储物间物品
     */
    private void loadStorageItems(Inventory inv, Player player) {
        List<ItemStack> items = playerStorage.get(player);
        if (items == null) return;

        for (int i = 0; i < items.size() && i < 45; i++) {
            ItemStack item = items.get(i);
            inv.setItem(i, item != null ? item.clone() : null);
        }
    }

    /**
     * 填充控制行
     */
    private void fillControlRow(Inventory inv) {
        ItemStack glass = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass.clone());
        }

        // 信息提示
        ItemStack infoItem = createInfoItem();
        inv.setItem(49, infoItem);
    }

    /**
     * 创建信息物品
     */
    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.storage.info.title"));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.storage.info.lore");
        meta.setLore(lore);

        item.setItemMeta(meta);

        return item;
    }

    /**
     * 添加物品到储物间
     */
    public void addToStorage(Player player, ItemStack item) {
        playerStorage.computeIfAbsent(player, k -> new ArrayList<>()).add(item.clone());
    }

    /**
     * 批量添加物品到储物间
     */
    public void addToStorage(Player player, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null) {
                addToStorage(player, item);
            }
        }
    }

    /**
     * 从储物间移除物品
     */
    public void removeFromStorage(Player player, int slot) {
        List<ItemStack> items = playerStorage.get(player);
        if (items != null && slot >= 0 && slot < items.size()) {
            items.remove(slot);
            if (items.isEmpty()) {
                playerStorage.remove(player);
            }
        }
    }

    /**
     * 保存储物间内容
     */
    public void saveStorage(Player player, Inventory inv) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
                items.add(item.clone());
            }
        }

        if (items.isEmpty()) {
            playerStorage.remove(player);
        } else {
            playerStorage.put(player, items);
        }
    }

    /**
     * 获取储物间物品数量
     */
    public int getStorageSize(Player player) {
        List<ItemStack> items = playerStorage.get(player);
        return items == null ? 0 : items.size();
    }

    /**
     * 检查储物间是否为空
     */
    public boolean isStorageEmpty(Player player) {
        List<ItemStack> items = playerStorage.get(player);
        return items == null || items.isEmpty();
    }

    /**
     * 清除储物间
     */
    public void clearStorage(Player player) {
        playerStorage.remove(player);
    }

    /**
     * 获取储物间所有物品
     */
    public List<ItemStack> getStorageItems(Player player) {
        return new ArrayList<>(playerStorage.getOrDefault(player, new ArrayList<>()));
    }
}

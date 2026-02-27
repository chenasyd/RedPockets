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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物品红包创建GUI
 */
public class ItemRedPocketGUI {

    private final RedPocketsPlugin plugin;
    private final Map<Player, Inventory> playerInventories;
    private final Map<Player, Boolean> playerReadOnlyMode;

    public ItemRedPocketGUI(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.playerInventories = new HashMap<>();
        this.playerReadOnlyMode = new HashMap<>();
    }

    /**
     * 打开物品红包GUI (9x3)
     */
    public void openItemRedPocketGUI(Player player) {
        plugin.getScheduler().runForEntity(player, () -> {
            String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getMessageManager().getMessage("gui.create.item.title"));
            Inventory inv = Bukkit.createInventory(null, 27, title);

            // 填充边框
            fillBorder(inv);

            // 第11槽位：编辑物品
            ItemStack editItem = createEditItemIcon(player);
            inv.setItem(11, editItem);

            // 第14槽位：确认发送
            ItemStack confirmItem = createConfirmItem();
            inv.setItem(14, confirmItem);

            player.openInventory(inv);
            plugin.getGUIManager().registerGUI(player, inv, "item_redpocket");
        });
    }

    /**
     * 打开物品编辑GUI (9x6)
     */
    public void openItemEditGUI(Player player) {
        plugin.getScheduler().runForEntity(player, () -> {
            // 检查是否已发送红包且未过期
            boolean isLocked = plugin.getItemEditStorageManager().isItemsLocked(player.getUniqueId());
            playerReadOnlyMode.put(player, isLocked);

            String titleKey = isLocked ? "gui.create.item.edit.title_readonly" : "gui.create.item.edit.title";
            String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getMessageManager().getMessage(titleKey));
            Inventory inv = Bukkit.createInventory(null, 54, title);

            // 先尝试从数据库加载持久化的物品
            ItemStack[] savedItems = plugin.getItemEditStorageManager().loadPlayerItems(player.getUniqueId());
            if (savedItems != null) {
                // 从数据库加载
                for (int i = 0; i < Math.min(savedItems.length, 54); i++) {
                    ItemStack item = savedItems[i];
                    inv.setItem(i, item != null ? item.clone() : null);
                }
            } else {
                // 从内存中加载
                Inventory savedInv = playerInventories.get(player);
                if (savedInv != null) {
                    for (int i = 0; i < 54; i++) {
                        ItemStack item = savedInv.getItem(i);
                        inv.setItem(i, item != null ? item.clone() : null);
                    }
                }
            }

            player.openInventory(inv);
            plugin.getGUIManager().registerGUI(player, inv, "item_edit");
        });
    }

    /**
     * 创建编辑物品图标
     */
    private ItemStack createEditItemIcon(Player player) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        // 计算当前物品数量
        int itemCount = getItemCount(player);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(itemCount));

        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.create.item.edit_icon.title", placeholders));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.create.item.edit_icon.lore");
        meta.setLore(lore);

        item.setItemMeta(meta);

        NBT.modify(item, nbt -> {
            nbt.setString("redpocket_action", "open_edit");
        });

        return item;
    }

    /**
     * 获取玩家当前的物品数量
     */
    public int getItemCount(Player player) {
        Inventory savedInv = playerInventories.get(player);
        if (savedInv == null) return 0;
        int count = 0;
        for (int i = 0; i < 54; i++) {
            ItemStack item = savedInv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 创建确认发送物品
     */
    private ItemStack createConfirmItem() {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.create.item.confirm.title"));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.create.item.confirm.lore");
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
     * 保存玩家编辑的物品
     */
    public void savePlayerInventory(Player player, Inventory editInv) {
        Inventory saved = Bukkit.createInventory(null, 54);
        for (int i = 0; i < 54; i++) {
            ItemStack item = editInv.getItem(i);
            saved.setItem(i, item != null ? item.clone() : null);
        }
        playerInventories.put(player, saved);
    }

    /**
     * 自动保存编辑内容（在操作后调用）
     * 同时保存到内存和数据库
     */
    public void autoSave(Player player, Inventory editInv) {
        // 保存到内存
        savePlayerInventory(player, editInv);

        // 保存到数据库（持久化），如果已有关联红包则保持关联
        String redPocketId = plugin.getItemEditStorageManager().getRedPocketId(player.getUniqueId());
        long expiresAt = plugin.getItemEditStorageManager().getRedPocketExpiresAt(player.getUniqueId());

        ItemStack[] items = new ItemStack[54];
        for (int i = 0; i < 54; i++) {
            items[i] = editInv.getItem(i);
        }
        plugin.getItemEditStorageManager().savePlayerItems(player.getUniqueId(), items, redPocketId, expiresAt);
    }

    /**
     * 获取玩家保存的物品
     */
    public Inventory getPlayerInventory(Player player) {
        return playerInventories.get(player);
    }

    /**
     * 获取玩家保存的物品数组
     */
    public ItemStack[] getPlayerItems(Player player) {
        Inventory inv = playerInventories.get(player);
        if (inv == null) return new ItemStack[0];
        ItemStack[] items = new ItemStack[54];
        for (int i = 0; i < 54; i++) {
            items[i] = inv.getItem(i);
        }
        return items;
    }

    /**
     * 清除玩家数据（仅在成功发送红包后调用）
     */
    public void clearPlayerData(Player player) {
        // 清除内存中的数据
        playerInventories.remove(player);
        playerReadOnlyMode.remove(player);

        // 注意：不清除数据库中的持久化数据，因为红包可能还在有效期内
        // 物品数据会保留，直到红包过期后被玩家取回
    }

    /**
     * 关联物品到红包（发送红包时调用）
     */
    public void associateRedPocket(Player player, String redPocketId, long expiresAt) {
        ItemStack[] items = new ItemStack[54];
        Inventory savedInv = playerInventories.get(player);
        if (savedInv != null) {
            for (int i = 0; i < 54; i++) {
                items[i] = savedInv.getItem(i);
            }
        }

        // 更新数据库，关联红包ID和过期时间
        plugin.getItemEditStorageManager().savePlayerItems(player.getUniqueId(), items, redPocketId, expiresAt);

        // 更新只读状态
        playerReadOnlyMode.put(player, true);
    }

    /**
     * 检查玩家是否处于只读模式
     */
    public boolean isReadOnlyMode(Player player) {
        return playerReadOnlyMode.getOrDefault(player, false);
    }
}

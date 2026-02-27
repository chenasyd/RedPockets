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
 * 红包创建GUI
 */
public class RedPocketCreateGUI {

    private final RedPocketsPlugin plugin;

    public RedPocketCreateGUI(RedPocketsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开类型选择GUI (9x3)
     */
    public void openTypeSelectGUI(Player player) {
        plugin.getScheduler().runForEntity(player, () -> {
            String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getMessageManager().getMessage("gui.create.type_select.title"));
            Inventory inv = Bukkit.createInventory(null, 27, title);

            // 填充装饰
            fillBorder(inv);

            // 第11槽位：金币红包
            ItemStack coinRedPocket = createCoinRedPocketIcon();
            inv.setItem(11, coinRedPocket);

            // 第15槽位：物品红包
            ItemStack itemRedPocket = createItemRedPocketIcon();
            inv.setItem(15, itemRedPocket);

            player.openInventory(inv);
            plugin.getGUIManager().registerGUI(player, inv, "type_select");
        });
    }

    /**
     * 创建金币红包图标
     */
    private ItemStack createCoinRedPocketIcon() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.create.type_select.coin.title"));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.create.type_select.coin.lore");
        meta.setLore(lore);

        item.setItemMeta(meta);

        // 使用NBT标记
        NBT.modify(item, nbt -> {
            nbt.setString("redpocket_action", "create_coin");
        });

        return item;
    }

    /**
     * 创建物品红包图标
     */
    private ItemStack createItemRedPocketIcon() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.create.type_select.item.title"));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.create.type_select.item.lore");
        meta.setLore(lore);

        item.setItemMeta(meta);

        // 使用NBT标记
        NBT.modify(item, nbt -> {
            nbt.setString("redpocket_action", "create_item");
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
}

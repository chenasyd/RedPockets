package com.redpockets.manager;

import com.redpockets.RedPocketsPlugin;
import com.redpockets.model.RedPocket;
import com.redpockets.model.RedPocketRecord;
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
 * GUI 管理器
 * 管理红包相关的所有GUI
 */
public class GUIManager {

    private final RedPocketsPlugin plugin;
    private final Map<Player, Inventory> openGUIs;
    private final Map<Player, String> guiTypes;

    public GUIManager(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
        this.guiTypes = new HashMap<>();
    }

    /**
     * 打开红包GUI
     */
    public void openRedPocketGUI(Player player, RedPocket redPocket) {
        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.redpocket.title"));
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // 填充红包信息
        fillRedPocketInfo(inv, redPocket);

        player.openInventory(inv);
        openGUIs.put(player, inv);
        guiTypes.put(player, "redpocket");
    }

    /**
     * 填充红包信息到GUI
     */
    private void fillRedPocketInfo(Inventory inv, RedPocket redPocket) {
        // 红包图标（中间位置）
        ItemStack icon = createRedPocketIcon(redPocket);
        inv.setItem(22, icon);

        // 信息面板（左侧）
        ItemStack infoItem = createInfoItem(redPocket);
        inv.setItem(20, infoItem);

        // 记录列表（右侧）
        ItemStack recordsItem = createRecordsItem(redPocket);
        inv.setItem(24, recordsItem);

        // 装饰边框
        fillBorder(inv);
    }

    /**
     * 创建红包图标
     */
    private ItemStack createRedPocketIcon(RedPocket redPocket) {
        Material material = redPocket.getType() == RedPocket.RedPocketType.RANDOM ?
            Material.GOLD_BLOCK : Material.RED_WOOL;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String titleKey = redPocket.getType() == RedPocket.RedPocketType.RANDOM ?
            "gui.redpocket.icon.random" : "gui.redpocket.icon.average";
        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage(titleKey));

        meta.setDisplayName(title);

        String typeColor = plugin.getConfig().getString("display.type-colors." +
            (redPocket.getType() == RedPocket.RedPocketType.RANDOM ? "random" : "average"), "&6");

        List<String> lore = plugin.getMessageManager().getMessageList("gui.redpocket.icon.lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i))
                .replace("{type}", redPocket.getType().name())
                .replace("{amount}", String.valueOf(redPocket.getTotalAmount()))
                .replace("{count}", String.valueOf(redPocket.getCount()))
                .replace("{note}", redPocket.getNote() != null ? redPocket.getNote() : "无"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        // 使用NBT标记
        NBT.modify(item, nbt -> {
            nbt.setString("redpocket_id", redPocket.getId());
            nbt.setString("redpocket_type", "icon");
        });

        return item;
    }

    /**
     * 创建信息物品
     */
    private ItemStack createInfoItem(RedPocket redPocket) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.redpocket.info.title"));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.redpocket.info.lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i))
                .replace("{sender}", Bukkit.getOfflinePlayer(redPocket.getSender()).getName())
                .replace("{type}", redPocket.getType().name())
                .replace("{amount}", String.valueOf(redPocket.getTotalAmount()))
                .replace("{count}", String.valueOf(redPocket.getCount()))
                .replace("{note}", redPocket.getNote() != null ? redPocket.getNote() : "无")
                .replace("{created}", formatTime(redPocket.getCreatedAt()))
                .replace("{expires}", redPocket.getExpiresAt() > 0 ? formatTime(redPocket.getExpiresAt()) : "永久"));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 创建记录物品
     */
    private ItemStack createRecordsItem(RedPocket redPocket) {
        List<RedPocketRecord> records = plugin.getRedPocketManager().getRedPocketRecords(redPocket.getId());

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.redpocket.records.title"));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList("gui.redpocket.records.lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i))
                .replace("{count}", String.valueOf(records.size()))
                .replace("{total}", String.valueOf(records.stream().mapToDouble(RedPocketRecord::getAmount).sum())));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 填充GUI边框
     */
    private void fillBorder(Inventory inv) {
        ItemStack glass = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        // 填充边框
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inv.setItem(slot, glass.clone());
        }

        // 返回按钮
        ItemStack backButton = createButton(Material.ARROW, "gui.redpocket.buttons.back");
        inv.setItem(45, backButton);

        // 抢红包按钮
        ItemStack grabButton = createButton(Material.GOLD_INGOT, "gui.redpocket.buttons.grab");
        inv.setItem(49, grabButton);
    }

    /**
     * 创建按钮
     */
    private ItemStack createButton(Material material, String titleKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String title = ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage(titleKey + ".title"));
        meta.setDisplayName(title);

        List<String> lore = plugin.getMessageManager().getMessageList(titleKey + ".lore");
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }

        item.setItemMeta(meta);

        // 使用NBT标记
        NBT.modify(item, nbt -> {
            nbt.setString("redpocket_type", "button");
            nbt.setString("redpocket_action", titleKey.split("\\.")[3]);
        });

        return item;
    }

    /**
     * 格式化时间
     */
    private String formatTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    }

    /**
     * 注册GUI
     */
    public void registerGUI(Player player, Inventory inv, String type) {
        openGUIs.put(player, inv);
        guiTypes.put(player, type);
    }

    /**
     * 取消注册GUI
     */
    public void unregisterGUI(Player player) {
        openGUIs.remove(player);
        guiTypes.remove(player);
    }

    /**
     * 获取GUI类型
     */
    public String getGUIType(Player player) {
        return guiTypes.get(player);
    }

    /**
     * 关闭所有GUI
     */
    public void closeAllGUIs() {
        for (Player player : openGUIs.keySet()) {
            if (player.isOnline()) {
                player.closeInventory();
            }
        }
        openGUIs.clear();
        guiTypes.clear();
    }

    /**
     * 检查是否是本插件创建的GUI
     */
    public boolean isRedPocketGUI(Inventory inv) {
        return openGUIs.containsValue(inv);
    }
}

package com.redpockets.model;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 红包数据模型
 */
public class RedPocket implements ConfigurationSerializable {

    private final String id;
    private final UUID sender;
    private final RedPocketType type;
    private final double totalAmount;
    private final int count;
    private final String note;
    private final long createdAt;
    private final long expiresAt;
    private boolean isClaimed;

    /**
     * 红包类型
     */
    public enum RedPocketType {
        RANDOM,   // 随机红包
        AVERAGE,  // 平分红包
        ITEM      // 物品红包
    }

    public RedPocket(String id, UUID sender, RedPocketType type, double totalAmount,
                     int count, String note, long createdAt, long expiresAt) {
        this.id = id;
        this.sender = sender;
        this.type = type;
        this.totalAmount = totalAmount;
        this.count = count;
        this.note = note;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.isClaimed = false;
    }

    public RedPocket(Map<String, Object> map) {
        this.id = (String) map.get("id");
        this.sender = UUID.fromString((String) map.get("sender"));
        this.type = RedPocketType.valueOf((String) map.get("type"));
        this.totalAmount = (Double) map.get("totalAmount");
        this.count = (Integer) map.get("count");
        this.note = (String) map.get("note");
        this.createdAt = ((Number) map.get("createdAt")).longValue();
        this.expiresAt = map.containsKey("expiresAt") ?
            ((Number) map.get("expiresAt")).longValue() : 0;
        this.isClaimed = map.containsKey("isClaimed") ?
            (Boolean) map.get("isClaimed") : false;
    }

    // Getters
    public String getId() { return id; }
    public UUID getSender() { return sender; }
    public RedPocketType getType() { return type; }
    public double getTotalAmount() { return totalAmount; }
    public int getCount() { return count; }
    public String getNote() { return note; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isClaimed() { return isClaimed; }
    public void setClaimed(boolean claimed) { this.isClaimed = claimed; }

    /**
     * 检查红包是否过期
     */
    public boolean isExpired() {
        if (expiresAt <= 0) return false;
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * 检查红包是否有效
     */
    public boolean isValid() {
        return !isClaimed && !isExpired();
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("sender", sender.toString());
        map.put("type", type.name());
        map.put("totalAmount", totalAmount);
        map.put("count", count);
        map.put("note", note);
        map.put("createdAt", createdAt);
        map.put("expiresAt", expiresAt);
        map.put("isClaimed", isClaimed);
        return map;
    }
}

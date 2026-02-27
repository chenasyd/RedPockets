package com.redpockets.model;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 红包抢取记录
 */
public class RedPocketRecord implements ConfigurationSerializable {

    private final String id;
    private final String redPocketId;
    private final UUID claimer;
    private final double amount;
    private final long claimedAt;

    public RedPocketRecord(String id, String redPocketId, UUID claimer, double amount, long claimedAt) {
        this.id = id;
        this.redPocketId = redPocketId;
        this.claimer = claimer;
        this.amount = amount;
        this.claimedAt = claimedAt;
    }

    public RedPocketRecord(Map<String, Object> map) {
        this.id = (String) map.get("id");
        this.redPocketId = (String) map.get("redPocketId");
        this.claimer = UUID.fromString((String) map.get("claimer"));
        this.amount = (Double) map.get("amount");
        this.claimedAt = ((Number) map.get("claimedAt")).longValue();
    }

    // Getters
    public String getId() { return id; }
    public String getRedPocketId() { return redPocketId; }
    public UUID getClaimer() { return claimer; }
    public double getAmount() { return amount; }
    public long getClaimedAt() { return claimedAt; }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("redPocketId", redPocketId);
        map.put("claimer", claimer.toString());
        map.put("amount", amount);
        map.put("claimedAt", claimedAt);
        return map;
    }
}

package com.enterprise.guiban.storage;

import java.util.UUID;

public class Punishment {
    private final UUID uuid;
    private final PunishmentType type;
    private final String reason;
    private final long startTime;
    private final long expiryTime; // -1 for permanent
    private final String punisher;

    public Punishment(UUID uuid, PunishmentType type, String reason, long startTime, long expiryTime, String punisher) {
        this.uuid = uuid;
        this.type = type;
        this.reason = reason;
        this.startTime = startTime;
        this.expiryTime = expiryTime;
        this.punisher = punisher;
    }

    public UUID getUuid() { return uuid; }
    public PunishmentType getType() { return type; }
    public String getReason() { return reason; }
    public long getStartTime() { return startTime; }
    public long getExpiryTime() { return expiryTime; }
    public String getPunisher() { return punisher; }

    public boolean isExpired() {
        if (expiryTime == -1) return false;
        return System.currentTimeMillis() > expiryTime;
    }
}

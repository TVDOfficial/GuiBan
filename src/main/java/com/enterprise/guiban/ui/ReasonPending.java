package com.enterprise.guiban.ui;

import com.enterprise.guiban.storage.PunishmentType;

import java.util.UUID;

public final class ReasonPending {
    private final PunishmentType type;
    private final UUID targetUuid;
    private final long expiryTime; // -1 for permanent

    public ReasonPending(PunishmentType type, UUID targetUuid, long expiryTime) {
        this.type = type;
        this.targetUuid = targetUuid;
        this.expiryTime = expiryTime;
    }

    public PunishmentType getType() { return type; }
    public UUID getTargetUuid() { return targetUuid; }
    public long getExpiryTime() { return expiryTime; }
}

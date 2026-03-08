package com.enterprise.guiban.storage;

import java.util.UUID;
import java.util.List;

public interface StorageProvider {

    void initialize();

    void addPunishment(Punishment punishment);

    void removePunishment(UUID uuid, PunishmentType type);

    Punishment getActivePunishment(UUID uuid, PunishmentType type);

    List<Punishment> getHistory(UUID uuid);
}

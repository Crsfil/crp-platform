package com.example.crp.inventory.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Minimal lifecycle states for an enterprise asset.
 * Keep as string in DB for forward compatibility, but validate transitions here.
 */
public enum EquipmentStatus {
    AVAILABLE,
    RESERVED,
    IN_TRANSIT,
    IN_STORAGE,
    LEASED,
    RETURNED,
    REPOSSESSED,
    SOLD,
    DISPOSED,
    LOST,
    DAMAGED;

    public static Set<EquipmentStatus> allowedNext(EquipmentStatus from) {
        if (from == null) {
            return EnumSet.allOf(EquipmentStatus.class);
        }
        return switch (from) {
            case AVAILABLE -> EnumSet.of(RESERVED, IN_TRANSIT, IN_STORAGE, LEASED, DAMAGED, LOST, DISPOSED);
            case RESERVED -> EnumSet.of(AVAILABLE, IN_TRANSIT, IN_STORAGE, LEASED, DAMAGED, LOST);
            case IN_TRANSIT -> EnumSet.of(IN_STORAGE, AVAILABLE, DAMAGED, LOST);
            case IN_STORAGE -> EnumSet.of(AVAILABLE, LEASED, REPOSSESSED, SOLD, DISPOSED, DAMAGED, LOST);
            case LEASED -> EnumSet.of(RETURNED, REPOSSESSED, LOST, DAMAGED);
            case RETURNED -> EnumSet.of(IN_STORAGE, AVAILABLE, SOLD, DISPOSED, DAMAGED);
            case REPOSSESSED -> EnumSet.of(IN_STORAGE, SOLD, DISPOSED);
            case SOLD -> EnumSet.noneOf(EquipmentStatus.class);
            case DISPOSED -> EnumSet.noneOf(EquipmentStatus.class);
            case LOST -> EnumSet.noneOf(EquipmentStatus.class);
            case DAMAGED -> EnumSet.of(IN_STORAGE, AVAILABLE, DISPOSED);
        };
    }

    public static EquipmentStatus parseOrNull(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return EquipmentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}


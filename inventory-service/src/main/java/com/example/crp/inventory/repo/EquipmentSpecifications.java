package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.Equipment;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class EquipmentSpecifications {

    public static Specification<Equipment> statusEquals(String status) {
        if (status == null || status.isBlank()) return null;
        String normalized = status.trim().toUpperCase();
        return (root, q, cb) -> cb.equal(cb.upper(root.get("status")), normalized);
    }

    public static Specification<Equipment> locationIdEquals(Long locationId) {
        if (locationId == null) return null;
        return (root, q, cb) -> cb.equal(root.get("locationId"), locationId);
    }

    public static Specification<Equipment> locationIdIsNull() {
        return (root, q, cb) -> cb.isNull(root.get("locationId"));
    }

    public static Specification<Equipment> responsibleEquals(String responsibleUsername) {
        if (responsibleUsername == null || responsibleUsername.isBlank()) return null;
        String normalized = responsibleUsername.trim();
        return (root, q, cb) -> cb.equal(root.get("responsibleUsername"), normalized);
    }

    public static Specification<Equipment> inventoryNumberEquals(String inventoryNumber) {
        if (inventoryNumber == null || inventoryNumber.isBlank()) return null;
        String normalized = inventoryNumber.trim();
        return (root, q, cb) -> cb.equal(root.get("inventoryNumber"), normalized);
    }

    public static Specification<Equipment> serialNumberEquals(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) return null;
        String normalized = serialNumber.trim();
        return (root, q, cb) -> cb.equal(root.get("serialNumber"), normalized);
    }

    public static Specification<Equipment> manufacturerLike(String manufacturer) {
        if (manufacturer == null || manufacturer.isBlank()) return null;
        String pattern = "%" + manufacturer.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("manufacturer")), pattern);
    }

    public static Specification<Equipment> typeLike(String type) {
        if (type == null || type.isBlank()) return null;
        String pattern = "%" + type.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("type")), pattern);
    }

    public static Specification<Equipment> priceBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return null;
        return (root, q, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("price"), min, max);
            }
            if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("price"), min);
            }
            return cb.lessThanOrEqualTo(root.get("price"), max);
        };
    }
}

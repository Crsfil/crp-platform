package com.example.crp.inventory.security;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.Location;
import com.example.crp.inventory.repo.LocationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EquipmentAccessPolicy {
    private final LocationRepository locationRepository;
    private final LocationAccessPolicy locationAccessPolicy;

    public EquipmentAccessPolicy(LocationRepository locationRepository, LocationAccessPolicy locationAccessPolicy) {
        this.locationRepository = locationRepository;
        this.locationAccessPolicy = locationAccessPolicy;
    }

    public void assertReadAllowed(Authentication auth, Equipment equipment) {
        if (!isReadAllowed(auth, equipment)) {
            throw new org.springframework.security.access.AccessDeniedException("equipment_access_denied");
        }
    }

    public void assertWriteAllowed(Authentication auth, Equipment equipment) {
        if (!isWriteAllowed(auth, equipment)) {
            throw new org.springframework.security.access.AccessDeniedException("equipment_access_denied");
        }
    }

    public void assertWriteAllowed(Authentication auth, Long locationId) {
        if (locationId == null) {
            return;
        }
        Location loc = locationRepository.findById(locationId).orElse(null);
        if (loc == null) {
            throw new org.springframework.security.access.AccessDeniedException("equipment_access_denied");
        }
        locationAccessPolicy.assertWriteAllowed(auth, loc);
    }

    public boolean isReadAllowed(Authentication auth, Equipment equipment) {
        if (equipment == null) {
            return false;
        }
        Long locId = equipment.getLocationId();
        if (locId == null) {
            return true;
        }
        Location loc = locationRepository.findById(locId).orElse(null);
        if (loc == null) {
            return false;
        }
        return locationAccessPolicy.isReadAllowed(auth, loc);
    }

    public boolean isWriteAllowed(Authentication auth, Equipment equipment) {
        if (equipment == null) {
            return false;
        }
        Long locId = equipment.getLocationId();
        if (locId == null) {
            return true;
        }
        Location loc = locationRepository.findById(locId).orElse(null);
        if (loc == null) {
            return false;
        }
        return locationAccessPolicy.isWriteAllowed(auth, loc);
    }

    public List<Equipment> filterReadAllowed(Authentication auth, List<Equipment> equipment) {
        if (equipment == null || equipment.isEmpty()) {
            return List.of();
        }
        Set<Long> locationIds = equipment.stream()
                .map(Equipment::getLocationId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Location> locationsById = new HashMap<>();
        if (!locationIds.isEmpty()) {
            for (Location l : locationRepository.findAllById(locationIds)) {
                locationsById.put(l.getId(), l);
            }
        }
        return equipment.stream()
                .filter(e -> {
                    Long locId = e.getLocationId();
                    if (locId == null) return true;
                    Location loc = locationsById.get(locId);
                    return loc != null && locationAccessPolicy.isReadAllowed(auth, loc);
                })
                .toList();
    }
}

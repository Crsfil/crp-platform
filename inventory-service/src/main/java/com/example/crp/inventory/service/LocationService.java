package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Location;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository repository;
    private final OutboxService outboxService;

    public LocationService(LocationRepository repository, OutboxService outboxService) {
        this.repository = repository;
        this.outboxService = outboxService;
    }

    public List<Location> list() {
        return repository.findAll();
    }

    public Location get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public Location create(Location location, String createdBy, String correlationId) {
        if (location.getCode() == null || location.getCode().isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (location.getName() == null || location.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (location.getType() == null || location.getType().isBlank()) {
            location.setType("OTHER");
        }
        if (location.getStatus() == null || location.getStatus().isBlank()) {
            location.setStatus("ACTIVE");
        }
        String code = location.getCode().trim();
        repository.findByCode(code).ifPresent(existing -> {
            throw new IllegalStateException("location code already exists");
        });
        location.setCode(code);

        Location parent = null;
        if (location.getParentId() != null) {
            parent = repository.findById(location.getParentId()).orElseThrow();
        }

        if (parent != null) {
            String parentRegion = parent.getRegion();
            if ((location.getRegion() == null || location.getRegion().isBlank()) && parentRegion != null && !parentRegion.isBlank()) {
                location.setRegion(parentRegion);
            } else if (parentRegion != null && !parentRegion.isBlank() && location.getRegion() != null && !location.getRegion().isBlank()) {
                if (!parentRegion.trim().equalsIgnoreCase(location.getRegion().trim())) {
                    throw new IllegalStateException("child region must match parent region");
                }
            }
        }

        location.setPath(buildPath(parent == null ? null : parent.getPath(), code));
        location.setLevel(parent == null ? 0 : (parent.getLevel() + 1));
        Location saved = repository.save(location);
        outboxService.enqueue("Location", saved.getId(), "inventory.location.created",
                "InventoryLocationCreated", new Events.InventoryLocationCreated(
                        saved.getId(), saved.getCode(), saved.getName(), saved.getType(), saved.getRegion(), createdBy, correlationId
                ));
        return saved;
    }

    @Transactional
    public Location setStatus(Long id, String status) {
        Location loc = repository.findById(id).orElseThrow();
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        loc.setStatus(status.trim().toUpperCase());
        return repository.save(loc);
    }

    public java.util.List<Location> children(Long parentId) {
        return repository.findByParentIdOrderByCodeAsc(parentId);
    }

    public java.util.List<Location> roots() {
        return repository.findByParentIdOrderByCodeAsc(null);
    }

    public java.util.List<Location> subtree(Long id) {
        Location root = repository.findById(id).orElseThrow();
        String prefix = root.getPath() == null ? root.getCode() : root.getPath();
        return repository.findByPathStartingWithOrderByPathAsc(prefix);
    }

    @Transactional
    public Location reparent(Long id, Long newParentId) {
        Location loc = repository.findById(id).orElseThrow();
        Location newParent = newParentId == null ? null : repository.findById(newParentId).orElseThrow();

        if (newParent != null) {
            String locPath = safePath(loc);
            String parentPath = safePath(newParent);
            if (parentPath.equals(locPath) || parentPath.startsWith(locPath + "/")) {
                throw new IllegalStateException("Cannot set parent to descendant");
            }
        }

        String oldPath = safePath(loc);
        String newPath = buildPath(newParent == null ? null : safePath(newParent), loc.getCode());
        int newLevel = newParent == null ? 0 : (newParent.getLevel() + 1);

        loc.setParentId(newParentId);
        loc.setPath(newPath);
        loc.setLevel(newLevel);
        repository.save(loc);

        // Update descendants paths/levels
        String oldPrefix = oldPath + "/";
        String newPrefix = newPath + "/";
        java.util.List<Location> descendants = repository.findByPathStartingWithOrderByPathAsc(oldPrefix);
        if (!descendants.isEmpty()) {
            for (Location d : descendants) {
                String dp = safePath(d);
                if (!dp.startsWith(oldPrefix)) {
                    continue;
                }
                d.setPath(newPrefix + dp.substring(oldPrefix.length()));
                d.setLevel(computeLevel(d.getPath()));
            }
            repository.saveAll(descendants);
        }
        return loc;
    }

    private static String safePath(Location l) {
        if (l.getPath() != null && !l.getPath().isBlank()) {
            return l.getPath();
        }
        return l.getCode();
    }

    private static String safePath(String pathOrNull, String code) {
        if (pathOrNull != null && !pathOrNull.isBlank()) return pathOrNull;
        return code;
    }

    private static String buildPath(String parentPath, String code) {
        String parent = parentPath == null ? null : parentPath.trim();
        String c = code == null ? null : code.trim();
        if (c == null || c.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (parent == null || parent.isBlank()) {
            return c;
        }
        String path = parent + "/" + c;
        if (path.length() > 512) {
            throw new IllegalArgumentException("location path too long");
        }
        return path;
    }

    private static int computeLevel(String path) {
        if (path == null || path.isBlank()) return 0;
        int count = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') count++;
        }
        return count;
    }
}

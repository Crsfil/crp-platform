package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.Location;
import com.example.crp.inventory.security.LocationAccessPolicy;
import com.example.crp.inventory.service.LocationService;
import com.example.crp.inventory.web.dto.InventoryDtos;
import com.example.crp.inventory.web.dto.InventoryMappers;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationsController {

    private final LocationService service;
    private final LocationAccessPolicy locationAccessPolicy;

    public LocationsController(LocationService service,
                               LocationAccessPolicy locationAccessPolicy) {
        this.service = service;
        this.locationAccessPolicy = locationAccessPolicy;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<InventoryDtos.LocationDto> list(Authentication auth) {
        return service.list().stream()
                .filter(l -> locationAccessPolicy.isReadAllowed(auth, l))
                .map(InventoryMappers::toLocation)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<InventoryDtos.LocationDto> get(@PathVariable("id") Long id, Authentication auth) {
        try {
            Location loc = service.get(id);
            locationAccessPolicy.assertReadAllowed(auth, loc);
            return ResponseEntity.ok(InventoryMappers.toLocation(loc));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).build();
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody InventoryDtos.CreateLocation req,
                                    Authentication auth,
                                    HttpServletRequest servletRequest) {
        try {
            Location l = new Location();
            l.setCode(req.code());
            l.setName(req.name());
            l.setParentId(req.parentId());
            l.setType(req.type());
            l.setAddress(req.address());
            l.setRegion(req.region());
            locationAccessPolicy.assertWriteAllowed(auth, l);
            String createdBy = auth == null ? null : auth.getName();
            String correlationId = servletRequest.getHeader("X-Correlation-Id");
            Location saved = service.create(l, createdBy, correlationId);
            return ResponseEntity.status(201).body(InventoryMappers.toLocation(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> setStatus(@PathVariable("id") Long id,
                                       @RequestParam("status") String status,
                                       Authentication auth) {
        try {
            Location loc = service.get(id);
            locationAccessPolicy.assertWriteAllowed(auth, loc);
            return ResponseEntity.ok(InventoryMappers.toLocation(service.setStatus(id, status)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/children")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.LocationDto>> children(@PathVariable("id") Long id, Authentication auth) {
        try {
            service.get(id); // verify exists
            var filtered = service.children(id).stream()
                    .filter(l -> locationAccessPolicy.isReadAllowed(auth, l))
                    .map(InventoryMappers::toLocation)
                    .toList();
            return ResponseEntity.ok(filtered);
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/roots")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<InventoryDtos.LocationDto> roots(Authentication auth) {
        return service.roots().stream()
                .filter(l -> locationAccessPolicy.isReadAllowed(auth, l))
                .map(InventoryMappers::toLocation)
                .toList();
    }

    @GetMapping("/{id}/subtree")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<InventoryDtos.LocationDto>> subtree(@PathVariable("id") Long id, Authentication auth) {
        try {
            var filtered = service.subtree(id).stream()
                    .filter(l -> locationAccessPolicy.isReadAllowed(auth, l))
                    .map(InventoryMappers::toLocation)
                    .toList();
            return ResponseEntity.ok(filtered);
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/parent")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> reparent(@PathVariable("id") Long id,
                                      @RequestParam(value = "parentId", required = false) Long parentId,
                                      Authentication auth) {
        try {
            Location loc = service.get(id);
            locationAccessPolicy.assertWriteAllowed(auth, loc);
            return ResponseEntity.ok(InventoryMappers.toLocation(service.reparent(id, parentId)));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}

package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.Stocktake;
import com.example.crp.inventory.domain.StocktakeLine;
import com.example.crp.inventory.service.StocktakeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/stocktakes")
public class StocktakeController {

    private final StocktakeService service;

    public StocktakeController(StocktakeService service) {
        this.service = service;
    }

    public record CreateStocktake(@NotNull Long locationId, String title) {}

    public record CountLine(@NotNull Long equipmentId,
                            Boolean present,
                            Long countedLocationId,
                            String countedStatus,
                            String note) {}

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateStocktake req, Authentication auth, HttpServletRequest httpReq) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = httpReq.getHeader("X-Correlation-Id");
            Stocktake st = service.create(req.locationId(), req.title(), user, correlationId, auth);
            return ResponseEntity.status(201).body(st);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<Stocktake> get(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(service.get(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<StocktakeLine>> lines(@PathVariable("id") Long id) {
        try {
            service.get(id);
            return ResponseEntity.ok(service.lines(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/count")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> count(@PathVariable("id") Long id, @Valid @RequestBody CountLine req, Authentication auth) {
        try {
            String user = auth == null ? null : auth.getName();
            return ResponseEntity.ok(service.countLine(id, req.equipmentId(), req.present(), req.countedLocationId(), req.countedStatus(), req.note(), user));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> submit(@PathVariable("id") Long id, Authentication auth, HttpServletRequest httpReq) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = httpReq.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(service.submit(id, user, correlationId));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> close(@PathVariable("id") Long id,
                                   @RequestParam(value = "apply", required = false, defaultValue = "false") boolean apply,
                                   Authentication auth,
                                   HttpServletRequest httpReq) {
        try {
            String user = auth == null ? null : auth.getName();
            String correlationId = httpReq.getHeader("X-Correlation-Id");
            return ResponseEntity.ok(service.close(id, apply, user, correlationId, auth));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public ResponseEntity<List<Stocktake>> listByLocation(@RequestParam("locationId") Long locationId) {
        return ResponseEntity.ok(service.listByLocation(locationId));
    }
}


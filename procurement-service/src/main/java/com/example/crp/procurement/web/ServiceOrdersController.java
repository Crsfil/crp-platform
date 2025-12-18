package com.example.crp.procurement.web;

import com.example.crp.procurement.domain.ProcurementRfq;
import com.example.crp.procurement.domain.ProcurementRfqOffer;
import com.example.crp.procurement.domain.ProcurementServiceOrder;
import com.example.crp.procurement.service.ProcurementServiceOrderService;
import com.example.crp.procurement.web.dto.ProcurementDtos;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/service")
public class ServiceOrdersController {

    private final ProcurementServiceOrderService serviceOrderService;
    private final com.example.crp.procurement.security.MfaPolicy mfaPolicy;

    public ServiceOrdersController(ProcurementServiceOrderService serviceOrderService,
                                   com.example.crp.procurement.security.MfaPolicy mfaPolicy) {
        this.serviceOrderService = serviceOrderService;
        this.mfaPolicy = mfaPolicy;
    }

    @PostMapping("/rfq")
    @PreAuthorize("hasAuthority('PROCUREMENT_TENDER') or hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> createRfq(@RequestBody ProcurementDtos.CreateRfq req,
                                       org.springframework.security.core.Authentication auth) {
        try {
            var rfq = new ProcurementRfq();
            rfq.setServiceType(req.serviceType());
            rfq.setRequestId(req.requestId());
            rfq.setEquipmentId(req.equipmentId());
            rfq.setLocationId(req.locationId());
            rfq.setTitle(req.title());
            rfq.setCreatedBy(auth == null ? null : auth.getName());
            var saved = serviceOrderService.createRfq(rfq, req.supplierIds(), auth);
            return ResponseEntity.status(201).body(toRfqDto(saved, serviceOrderService.listOffers(saved.getId())));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/rfq")
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<ProcurementDtos.RfqDto> listRfq(org.springframework.security.core.Authentication auth) {
        return serviceOrderService.listRfq(auth).stream()
                .map(r -> toRfqDto(r, serviceOrderService.listOffers(r.getId())))
                .toList();
    }

    @PostMapping("/rfq/{id}/offers")
    @PreAuthorize("hasAuthority('PROCUREMENT_TENDER') or hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> addOffer(@PathVariable("id") Long id,
                                      @RequestBody ProcurementDtos.CreateRfqOffer req) {
        try {
            var offer = new ProcurementRfqOffer();
            offer.setSupplierId(req.supplierId());
            offer.setPrice(req.price());
            offer.setCurrency(req.currency());
            offer.setEtaDays(req.etaDays());
            offer.setValidUntil(req.validUntil());
            var saved = serviceOrderService.addOffer(id, offer);
            return ResponseEntity.status(201).body(toOfferDto(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/rfq/{id}/award")
    @PreAuthorize("hasAuthority('PROCUREMENT_TENDER') or hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> award(@PathVariable("id") Long id,
                                   @RequestBody ProcurementDtos.AwardRfq req,
                                   org.springframework.security.core.Authentication auth) {
        try {
            if (!mfaPolicy.isSatisfied(auth)) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "mfa_required"));
            }
            var saved = serviceOrderService.award(id, req.supplierId(), req.reason(), auth);
            return ResponseEntity.ok(toRfqDto(saved, serviceOrderService.listOffers(id)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> createServiceOrder(@RequestBody ProcurementDtos.CreateServiceOrder req,
                                                org.springframework.security.core.Authentication auth) {
        try {
            var order = new ProcurementServiceOrder();
            order.setServiceType(req.serviceType());
            order.setRequestId(req.requestId());
            order.setEquipmentId(req.equipmentId());
            order.setLocationId(req.locationId());
            order.setSupplierId(req.supplierId());
            order.setVendorName(req.vendorName());
            order.setVendorInn(req.vendorInn());
            order.setSlaUntil(req.slaUntil());
            order.setPlannedCost(req.plannedCost());
            order.setCurrency(req.currency());
            order.setNote(req.note());
            order.setCreatedBy(auth == null ? null : auth.getName());
            var saved = serviceOrderService.create(order, auth);
            return ResponseEntity.status(201).body(toServiceOrderDto(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('PROCUREMENT_READ') or hasRole('ADMIN') or @trustedClientAuthorizer.isTrusted(authentication)")
    public List<ProcurementDtos.ServiceOrderDto> listServiceOrders(@RequestParam(value = "equipmentId", required = false) Long equipmentId,
                                                                   org.springframework.security.core.Authentication auth) {
        return serviceOrderService.listServiceOrders(equipmentId, auth).stream().map(this::toServiceOrderDto).toList();
    }

    @PostMapping("/orders/{id}/complete")
    @PreAuthorize("hasAuthority('PROCUREMENT_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<?> complete(@PathVariable("id") Long id,
                                      @RequestBody ProcurementDtos.CompleteServiceOrder req,
                                      org.springframework.security.core.Authentication auth) {
        try {
            if (!mfaPolicy.isSatisfied(auth)) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "mfa_required"));
            }
            var saved = serviceOrderService.complete(id, req.actualCost(), req.completedAt(), req.actDocumentId(), auth == null ? null : auth.getName(), auth);
            return ResponseEntity.ok(toServiceOrderDto(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    private ProcurementDtos.RfqDto toRfqDto(ProcurementRfq r, List<ProcurementRfqOffer> offers) {
        return new ProcurementDtos.RfqDto(
                r.getId(),
                r.getServiceType(),
                r.getRequestId(),
                r.getEquipmentId(),
                r.getLocationId(),
                r.getTitle(),
                r.getStatus(),
                r.getAwardedSupplierId(),
                r.getAwardReason(),
                r.getCreatedAt(),
                r.getCreatedBy(),
                offers == null ? List.of() : offers.stream().map(this::toOfferDto).collect(Collectors.toList())
        );
    }

    private ProcurementDtos.RfqOfferDto toOfferDto(ProcurementRfqOffer o) {
        return new ProcurementDtos.RfqOfferDto(
                o.getId(),
                o.getRfq().getId(),
                o.getSupplierId(),
                o.getPrice(),
                o.getCurrency(),
                o.getEtaDays(),
                o.getValidUntil(),
                o.getStatus(),
                o.getCreatedAt()
        );
    }

    private ProcurementDtos.ServiceOrderDto toServiceOrderDto(ProcurementServiceOrder o) {
        return new ProcurementDtos.ServiceOrderDto(
                o.getId(),
                o.getServiceType(),
                o.getRequestId(),
                o.getEquipmentId(),
                o.getLocationId(),
                o.getSupplierId(),
                o.getVendorName(),
                o.getVendorInn(),
                o.getStatus(),
                o.getSlaUntil(),
                o.getPlannedCost(),
                o.getActualCost(),
                o.getCurrency(),
                o.getCreatedAt(),
                o.getUpdatedAt(),
                o.getActDocumentId(),
                o.getNote()
        );
    }
}

package com.example.crp.procurement.web;

import com.example.crp.procurement.domain.ProcurementRequest;
import com.example.crp.procurement.messaging.Events;
import com.example.crp.procurement.repo.ProcurementRequestRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/requests")
public class ProcurementController {
    private final ProcurementRequestRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    public ProcurementController(ProcurementRequestRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository; this.kafkaTemplate = kafkaTemplate; }

    @GetMapping
    public List<ProcurementRequest> list() { return repository.findAll(); }

    @PostMapping
    public ProcurementRequest create(@Valid @RequestBody ProcurementRequest pr, Authentication auth) {
        pr.setStatus("PENDING");
        pr.setCreatedAt(OffsetDateTime.now());
        ProcurementRequest saved = repository.save(pr);
        kafkaTemplate.send("procurement.requested", new Events.ProcurementRequested(saved.getId(), saved.getEquipmentId(), saved.getRequesterId()));
        return saved;
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PROCUREMENT_APPROVE') or hasRole('ADMIN')")
    public ResponseEntity<ProcurementRequest> approve(@PathVariable Long id, Authentication auth) {
        return repository.findById(id).map(pr -> {
            pr.setStatus("APPROVED");
            ProcurementRequest s = repository.save(pr);
            Long approverId = null; // could be mapped via auth principal
            kafkaTemplate.send("procurement.approved", new Events.ProcurementApproved(s.getId(), s.getEquipmentId(), approverId));
            return ResponseEntity.ok(s);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PROCUREMENT_APPROVE') or hasRole('ADMIN')")
    public ResponseEntity<ProcurementRequest> reject(@PathVariable Long id, Authentication auth) {
        return repository.findById(id).map(pr -> {
            pr.setStatus("REJECTED");
            ProcurementRequest s = repository.save(pr);
            Long approverId = null;
            kafkaTemplate.send("procurement.rejected", new Events.ProcurementRejected(s.getId(), s.getEquipmentId(), approverId));
            return ResponseEntity.ok(s);
        }).orElse(ResponseEntity.notFound().build());
    }
}

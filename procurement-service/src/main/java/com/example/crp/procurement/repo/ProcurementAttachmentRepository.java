package com.example.crp.procurement.repo;

import com.example.crp.procurement.domain.ProcurementAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProcurementAttachmentRepository extends JpaRepository<ProcurementAttachment, UUID> {
    List<ProcurementAttachment> findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(String ownerType, Long ownerId);
}


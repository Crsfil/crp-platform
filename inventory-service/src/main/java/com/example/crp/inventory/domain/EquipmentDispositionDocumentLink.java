package com.example.crp.inventory.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "equipment_disposition_document")
public class EquipmentDispositionDocumentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disposition_id", nullable = false)
    private Long dispositionId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "relation_type", length = 32)
    private String relationType; // CONTRACT, INVOICE, PHOTO, ACT, OTHER

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public Long getDispositionId() { return dispositionId; }
    public void setDispositionId(Long dispositionId) { this.dispositionId = dispositionId; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}


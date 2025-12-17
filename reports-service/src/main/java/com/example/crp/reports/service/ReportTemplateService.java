package com.example.crp.reports.service;

import com.example.crp.reports.domain.ReportTemplate;
import com.example.crp.reports.repo.ReportTemplateRepository;
import com.example.crp.reports.storage.ReportStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Service
public class ReportTemplateService {

    private final ReportTemplateRepository repository;
    private final ReportStorage storage;

    public ReportTemplateService(ReportTemplateRepository repository, ReportStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public List<ReportTemplate> list() {
        return repository.findAll();
    }

    public ReportTemplate get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Optional<URI> downloadUrl(Long id) {
        ReportTemplate t = repository.findById(id).orElseThrow();
        return storage.presignGet(new ReportStorage.StoredReportRef(t.getStorageType(), t.getStorageLocation()));
    }

    public byte[] download(Long id) {
        ReportTemplate t = repository.findById(id).orElseThrow();
        return storage.get(new ReportStorage.StoredReportRef(t.getStorageType(), t.getStorageLocation()));
    }

    @Transactional
    public ReportTemplate upload(String templateId,
                                 String name,
                                 String version,
                                 MultipartFile file,
                                 String createdBy) {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template file", e);
        }
        String fileName = file.getOriginalFilename() == null ? "template.bin" : file.getOriginalFilename();
        String contentType = file.getContentType();

        ReportStorage.StoredReport stored = storage.put("template-" + templateId, fileName, contentType, bytes);

        ReportTemplate template = repository.findByTemplateId(templateId).orElseGet(ReportTemplate::new);
        template.setTemplateId(templateId.trim());
        template.setName(trim(name, 256));
        template.setVersion(trim(version, 64));
        template.setStorageType(stored.storageType());
        template.setStorageLocation(stored.location());
        template.setFileName(stored.fileName());
        template.setContentType(stored.contentType());
        template.setFileSize(stored.sizeBytes());
        template.setSha256(stored.sha256Hex());
        template.setCreatedBy(createdBy);
        return repository.save(template);
    }

    private static String trim(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }
}

package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.EquipmentDocument;
import com.example.crp.inventory.service.EquipmentDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EquipmentDocumentsController.class)
class EquipmentDocumentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EquipmentDocumentService equipmentDocumentService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void upload_returnsCreated() throws Exception {
        EquipmentDocument doc = new EquipmentDocument();
        doc.setId(UUID.randomUUID());
        doc.setEquipmentId(10L);
        doc.setDocType("PASSPORT");
        doc.setFileName("passport.pdf");
        doc.setContentType(MediaType.APPLICATION_PDF_VALUE);
        doc.setSizeBytes(3L);
        doc.setSha256("abc");
        doc.setStorageType("S3");
        doc.setStorageLocation("bucket/key");
        doc.setCreatedAt(OffsetDateTime.now());
        doc.setCreatedBy("u1");

        when(equipmentDocumentService.upload(anyLong(), nullable(String.class), any(), anyString(), nullable(String.class)))
                .thenReturn(doc);

        MockMultipartFile file = new MockMultipartFile("file", "passport.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF".getBytes());

        mockMvc.perform(multipart("/equipment/10/documents")
                        .file(file)
                        .param("docType", "PASSPORT")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentId").value(10L))
                .andExpect(jsonPath("$.docType").value("PASSPORT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_returnsOk() throws Exception {
        when(equipmentDocumentService.list(10L)).thenReturn(java.util.List.of());
        mockMvc.perform(get("/equipment/10/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}


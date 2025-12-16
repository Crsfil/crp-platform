package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.EquipmentDisposition;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.service.EquipmentDispositionDocumentsService;
import com.example.crp.inventory.service.EquipmentDispositionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EquipmentDispositionController.class)
class EquipmentDispositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EquipmentRepository equipmentRepository;

    @MockBean
    private EquipmentDispositionService service;

    @MockBean
    private EquipmentDispositionDocumentsService documentsService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsCreated() throws Exception {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(new com.example.crp.inventory.domain.Equipment()));
        EquipmentDisposition d = new EquipmentDisposition();
        d.setEquipmentId(10L);
        d.setType("SALE");
        d.setStatus("DRAFT");
        when(service.create(eq(10L), anyString(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(d);

        var body = java.util.Map.of("type", "SALE");
        mockMvc.perform(post("/equipment/10/dispositions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentId").value(10L))
                .andExpect(jsonPath("$.type").value("SALE"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saleContract_returnsOk() throws Exception {
        EquipmentDisposition d = new EquipmentDisposition();
        d.setEquipmentId(10L);
        d.setType("SALE");
        d.setStatus("CONTRACTED");
        d.setContractNumber("C-1");
        when(service.contractSale(eq(77L), any(), any(), any(), any(), any())).thenReturn(d);

        var body = java.util.Map.of("saleMethod", "DIRECT", "contractNumber", "C-1");
        mockMvc.perform(post("/equipment/dispositions/77/sale/contract")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTRACTED"))
                .andExpect(jsonPath("$.contractNumber").value("C-1"));
    }
}

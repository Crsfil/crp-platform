package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.EquipmentInspection;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.service.EquipmentInspectionService;
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

@WebMvcTest(EquipmentInspectionController.class)
class EquipmentInspectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EquipmentRepository equipmentRepository;

    @MockBean
    private EquipmentInspectionService service;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsCreated() throws Exception {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(new com.example.crp.inventory.domain.Equipment()));
        EquipmentInspection i = new EquipmentInspection();
        i.setEquipmentId(10L);
        i.setType("RETURN");
        i.setStatus("DRAFT");
        when(service.create(eq(10L), anyString(), any(), any(), any(), any(), any())).thenReturn(i);

        var body = java.util.Map.of("type", "RETURN");
        mockMvc.perform(post("/equipment/10/inspections")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentId").value(10L))
                .andExpect(jsonPath("$.type").value("RETURN"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }
}


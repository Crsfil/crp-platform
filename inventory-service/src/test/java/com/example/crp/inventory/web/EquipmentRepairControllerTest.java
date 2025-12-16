package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.EquipmentRepairOrder;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.service.EquipmentRepairService;
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

@WebMvcTest(EquipmentRepairController.class)
class EquipmentRepairControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EquipmentRepository equipmentRepository;

    @MockBean
    private EquipmentRepairService service;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsCreated() throws Exception {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(new com.example.crp.inventory.domain.Equipment()));
        EquipmentRepairOrder r = new EquipmentRepairOrder();
        r.setEquipmentId(10L);
        r.setStatus("DRAFT");
        when(service.create(eq(10L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(r);

        var body = java.util.Map.of();
        mockMvc.perform(post("/equipment/10/repairs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentId").value(10L))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addLine_returnsCreated() throws Exception {
        com.example.crp.inventory.domain.EquipmentRepairLine l = new com.example.crp.inventory.domain.EquipmentRepairLine();
        l.setRepairId(1L);
        l.setKind("PART");
        l.setDescription("Wheel");
        l.setQuantity(new java.math.BigDecimal("2"));
        l.setUnitCost(new java.math.BigDecimal("10.00"));
        l.setTotalCost(new java.math.BigDecimal("20.00"));
        when(service.addLine(eq(1L), anyString(), anyString(), any(), any(), any())).thenReturn(l);

        var body = java.util.Map.of(
                "kind", "PART",
                "description", "Wheel",
                "quantity", new java.math.BigDecimal("2"),
                "unitCost", new java.math.BigDecimal("10.00")
        );
        mockMvc.perform(post("/equipment/repairs/1/lines")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repairId").value(1L))
                .andExpect(jsonPath("$.totalCost").value(20.00));
    }
}

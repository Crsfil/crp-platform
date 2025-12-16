package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.EquipmentLease;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.service.EquipmentLeaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EquipmentLeaseController.class)
class EquipmentLeaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EquipmentRepository equipmentRepository;

    @MockBean
    private EquipmentLeaseService leaseService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void start_returnsOk() throws Exception {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(new Equipment()));
        EquipmentLease lease = new EquipmentLease();
        lease.setEquipmentId(10L);
        lease.setStatus("ACTIVE");
        lease.setStartAt(OffsetDateTime.now());
        when(leaseService.start(eq(10L), eq(2L), any(), any(), any(), any(), any(), any(), any())).thenReturn(lease);

        var body = java.util.Map.of("customerLocationId", 2L);
        mockMvc.perform(post("/equipment/10/lease/start")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentId").value(10L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void active_returns404_whenMissing() throws Exception {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/equipment/10/lease/active"))
                .andExpect(status().isNotFound());
    }
}


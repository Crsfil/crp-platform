package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.EquipmentMovement;
import com.example.crp.inventory.domain.EquipmentStatusHistory;
import com.example.crp.inventory.repo.EquipmentMovementRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.EquipmentStatusHistoryRepository;
import com.example.crp.inventory.service.EquipmentLifecycleService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EquipmentLifecycleController.class)
class EquipmentLifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EquipmentLifecycleService lifecycleService;

    @MockBean
    private EquipmentMovementRepository movementRepository;

    @MockBean
    private EquipmentStatusHistoryRepository statusHistoryRepository;

    @MockBean
    private EquipmentRepository equipmentRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void movements_returns404_whenEquipmentMissing() throws Exception {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/equipment/10/movements"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void movements_returnsOk() throws Exception {
        Equipment e = new Equipment();
        e.setId(10L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(e));

        EquipmentMovement m = new EquipmentMovement();
        m.setEquipmentId(10L);
        m.setFromLocationId(1L);
        m.setToLocationId(2L);
        m.setMovedAt(OffsetDateTime.now());
        when(movementRepository.findTop200ByEquipmentIdOrderByMovedAtDesc(10L)).thenReturn(java.util.List.of(m));

        mockMvc.perform(get("/equipment/10/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].equipmentId").value(10L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void statusHistory_returnsOk() throws Exception {
        Equipment e = new Equipment();
        e.setId(10L);
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(e));

        EquipmentStatusHistory h = new EquipmentStatusHistory();
        h.setEquipmentId(10L);
        h.setFromStatus("AVAILABLE");
        h.setToStatus("RESERVED");
        h.setChangedAt(OffsetDateTime.now());
        when(statusHistoryRepository.findTop200ByEquipmentIdOrderByChangedAtDesc(10L)).thenReturn(java.util.List.of(h));

        mockMvc.perform(get("/equipment/10/status-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].toStatus").value("RESERVED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void transfer_returnsOk() throws Exception {
        Equipment updated = new Equipment();
        updated.setId(10L);
        updated.setLocationId(2L);
        when(lifecycleService.transfer(anyLong(), anyLong(), nullable(String.class), nullable(String.class), anyString(), nullable(String.class), any()))
                .thenReturn(updated);

        var body = java.util.Map.of("toLocationId", 2L);
        mockMvc.perform(post("/equipment/10/transfer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.locationId").value(2L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_returnsOk() throws Exception {
        Equipment updated = new Equipment();
        updated.setId(10L);
        updated.setStatus("IN_STORAGE");
        when(lifecycleService.changeStatus(anyLong(), anyString(), nullable(String.class), anyString(), nullable(String.class), any()))
                .thenReturn(updated);

        var body = java.util.Map.of("status", "IN_STORAGE");
        mockMvc.perform(post("/equipment/10/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_STORAGE"));
    }
}

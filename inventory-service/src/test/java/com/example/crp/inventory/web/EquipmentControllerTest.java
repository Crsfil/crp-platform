package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.repo.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;


@WebMvcTest(EquipmentController.class)
class EquipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockBean
    private EquipmentRepository equipmentRepository;

    private Equipment equipmentToSave;

    @BeforeEach
    public void setUp() {
        this.objectMapper = new ObjectMapper();
        equipmentToSave = new Equipment();
        equipmentToSave.setModel("Asus");

        Equipment savedEquipment = new Equipment();
        savedEquipment.setId(1L);
        savedEquipment.setModel("Asus");

        when(equipmentRepository.save(any(Equipment.class))).thenReturn(savedEquipment);
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void testList_shouldReturnEquipmentList() throws Exception {
        Equipment kamaz = new Equipment();
        kamaz.setId(22L);
        kamaz.setModel("KAMAZ");

        Equipment maz = new Equipment();
        maz.setId(33L);
        maz.setModel("MAZ");

        Equipment gazel = new Equipment();
        gazel.setId(44L);
        gazel.setModel("GAZEL");

        List<Equipment> equipmentList = List.of(kamaz,maz,gazel);
        when(equipmentRepository.findAll()).thenReturn(equipmentList);
        mockMvc.perform(get("/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(22L))
                .andExpect(jsonPath("$[1].model").value("MAZ"))
                .andExpect(jsonPath("$[2].model").value("GAZEL"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_whenDataIsValidAndUserIsAuthorized_shouldReturnCreated() throws Exception {
        mockMvc.perform(post("/equipment")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(equipmentToSave)))

                .andExpect(status().isCreated()) //201
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.model").value("Asus"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_whenDataIsNotValidAndUserIsAuthorized_shouldReturnNotCreated() throws Exception {
        Equipment wrongValidParam = new Equipment();
        wrongValidParam.setId(null);
        wrongValidParam.setStatus(" ");
        mockMvc.perform(post("/equipment")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongValidParam)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailCreatingEquipmentWhenUnauthorized() throws Exception {
        mockMvc.perform(post("/equipment")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(equipmentToSave)))

                .andExpect(status().isUnauthorized()); // 401
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_whenEquipmentExists_shouldReturnOk() throws Exception {
        Long equipmentId = 1L;
        String newStatus = "ACTIVE";
        Equipment existingEquipment = new Equipment();
        existingEquipment.setId(equipmentId);
        existingEquipment.setModel("Asus");
        existingEquipment.setStatus("INACTIVE");

        Equipment updatedEquipment = new Equipment();
        updatedEquipment.setId(equipmentId);
        updatedEquipment.setModel("Asus");
        updatedEquipment.setStatus(newStatus);

        when(equipmentRepository.findById(equipmentId)).thenReturn(Optional.of(existingEquipment));
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(updatedEquipment);

        mockMvc.perform(patch("/equipment/{id}/status", equipmentId)
                        .param("status", newStatus)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(equipmentId))
                .andExpect(jsonPath("$.status").value(newStatus));
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_whenEquipmentNotFound_shouldReturnNotFound() throws Exception {
        Long nonExistentId = 999L;
        String newStatus = "ACTIVE";

        when(equipmentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/equipment/{id}/status", nonExistentId)
                        .param("status", newStatus)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


}
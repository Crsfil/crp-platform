package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.Location;
import com.example.crp.inventory.security.LocationAccessPolicy;
import com.example.crp.inventory.service.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationsController.class)
class LocationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationService locationService;

    @MockBean
    private LocationAccessPolicy locationAccessPolicy;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_returnsOk() throws Exception {
        Location l = new Location();
        l.setCode("MSK-WH-1");
        l.setName("Main warehouse");
        l.setType("WAREHOUSE");
        l.setStatus("ACTIVE");
        when(locationService.list()).thenReturn(java.util.List.of(l));
        when(locationAccessPolicy.isReadAllowed(any(), any())).thenReturn(true);

        mockMvc.perform(get("/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("MSK-WH-1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsCreated() throws Exception {
        Location saved = new Location();
        saved.setCode("SPB-WH-1");
        saved.setName("SPB warehouse");
        saved.setType("WAREHOUSE");
        saved.setStatus("ACTIVE");
        when(locationService.create(any(Location.class), anyString(), nullable(String.class))).thenReturn(saved);
        // allow ABAC filter if enabled
        when(locationAccessPolicy.isReadAllowed(any(), any())).thenReturn(true);

        var body = java.util.Map.of(
                "code", "SPB-WH-1",
                "name", "SPB warehouse",
                "type", "WAREHOUSE"
        );

        mockMvc.perform(post("/locations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SPB-WH-1"));
    }
}

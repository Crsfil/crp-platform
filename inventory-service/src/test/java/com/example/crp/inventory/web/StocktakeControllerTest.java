package com.example.crp.inventory.web;

import com.example.crp.inventory.domain.Stocktake;
import com.example.crp.inventory.domain.StocktakeLine;
import com.example.crp.inventory.service.StocktakeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StocktakeController.class)
class StocktakeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StocktakeService stocktakeService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsCreated() throws Exception {
        Stocktake st = new Stocktake();
        st.setLocationId(10L);
        st.setStatus("OPEN");
        when(stocktakeService.create(eq(10L), any(), any(), any(), any())).thenReturn(st);

        var body = java.util.Map.of("locationId", 10L, "title", "Count");
        mockMvc.perform(post("/inventory/stocktakes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void lines_returnsOk() throws Exception {
        StocktakeLine l = new StocktakeLine();
        l.setStocktakeId(1L);
        when(stocktakeService.get(1L)).thenReturn(new Stocktake());
        when(stocktakeService.lines(1L)).thenReturn(List.of(l));

        mockMvc.perform(get("/inventory/stocktakes/1/lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void submit_returnsOk() throws Exception {
        Stocktake st = new Stocktake();
        st.setStatus("SUBMITTED");
        when(stocktakeService.submit(eq(1L), any(), any())).thenReturn(st);

        mockMvc.perform(post("/inventory/stocktakes/1/submit").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }
}


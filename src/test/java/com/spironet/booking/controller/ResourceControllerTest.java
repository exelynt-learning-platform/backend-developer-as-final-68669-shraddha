package com.spironet.booking.controller;

import com.spironet.booking.AbstractIntegrationTest;
import com.spironet.booking.dto.resource.ResourceRequest;
import com.spironet.booking.entity.ResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceControllerTest extends AbstractIntegrationTest {

    @Test
    void list_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_asUser_returnsSeededResourcesPaginated() throws Exception {
        String token = obtainToken(USER1_USERNAME, USER_PASSWORD);

        mockMvc.perform(get("/api/resources")
                        .param("page", "0")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void create_asUser_isForbidden() throws Exception {
        String token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        ResourceRequest request = validRequest();

        mockMvc.perform(post("/api/resources")
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_asAdmin_isCreated() throws Exception {
        String token = obtainToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        ResourceRequest request = validRequest();

        mockMvc.perform(post("/api/resources")
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Test Room"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_asAdmin_withMissingName_returnsBadRequest() throws Exception {
        String token = obtainToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        ResourceRequest request = validRequest();
        request.setName(" ");

        mockMvc.perform(post("/api/resources")
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void create_asAdmin_withNegativePrice_returnsBadRequest() throws Exception {
        String token = obtainToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        ResourceRequest request = validRequest();
        request.setPricePerHour(new BigDecimal("-5.00"));

        mockMvc.perform(post("/api/resources")
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDelete_asAdmin_succeeds() throws Exception {
        String token = obtainToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        String createResponse = mockMvc.perform(post("/api/resources")
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        ResourceRequest updateRequest = validRequest();
        updateRequest.setName("Updated Room Name");

        mockMvc.perform(put("/api/resources/{id}", id)
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Room Name"));

        mockMvc.perform(delete("/api/resources/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/resources/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_asUser_isForbidden() throws Exception {
        String userToken = obtainToken(USER1_USERNAME, USER_PASSWORD);

        mockMvc.perform(delete("/api/resources/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_withNonExistentId_returnsNotFound() throws Exception {
        String token = obtainToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/resources/{id}", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    private ResourceRequest validRequest() {
        ResourceRequest request = new ResourceRequest();
        request.setName("Test Room");
        request.setType(ResourceType.ROOM);
        request.setDescription("A room for testing");
        request.setLocation("Building 9");
        request.setCapacity(6);
        request.setPricePerHour(new BigDecimal("20.00"));
        request.setActive(true);
        return request;
    }
}

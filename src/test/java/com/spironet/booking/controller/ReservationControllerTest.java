package com.spironet.booking.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.spironet.booking.AbstractIntegrationTest;
import com.spironet.booking.dto.reservation.ReservationCreateRequest;
import com.spironet.booking.dto.reservation.ReservationUpdateRequest;
import com.spironet.booking.entity.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationControllerTest extends AbstractIntegrationTest {

    private static final Long ROOM_RESOURCE_ID = 1L;
    private static final Long VEHICLE_RESOURCE_ID = 2L;

    @Test
    void create_asUser_takesIdentityFromJwtNotRequestBody() throws Exception {
        String token = obtainToken(USER1_USERNAME, USER_PASSWORD);

        JsonNode created = createReservation(token, ROOM_RESOURCE_ID, hoursFromNow(24), hoursFromNow(26));

        assertThatEquals(created, "username", USER1_USERNAME);
        assertThatEquals(created, "status", "PENDING");
        // 2 hours at 25.00/hr = 50.00
        assertThatEquals(created, "price", "50.0");
    }

    @Test
    void create_withEndBeforeStart_returnsBadRequest() throws Exception {
        String token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setResourceId(ROOM_RESOURCE_ID);
        request.setStartTime(hoursFromNow(26));
        request.setEndTime(hoursFromNow(24));

        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withPastStartTime_returnsBadRequest() throws Exception {
        String token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setResourceId(ROOM_RESOURCE_ID);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(hoursFromNow(2));

        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("startTime"));
    }

    @Test
    void create_forOverlappingSlotOnSameResource_returnsBadRequest() throws Exception {
        String user1Token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        String user2Token = obtainToken(USER2_USERNAME, USER_PASSWORD);

        createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(48), hoursFromNow(50));

        ReservationCreateRequest overlapping = new ReservationCreateRequest();
        overlapping.setResourceId(ROOM_RESOURCE_ID);
        overlapping.setStartTime(hoursFromNow(49));
        overlapping.setEndTime(hoursFromNow(51));

        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user2Token))
                        .content(objectMapper.writeValueAsString(overlapping)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_asUser_returnsOnlyOwnReservations() throws Exception {
        String user1Token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        String user2Token = obtainToken(USER2_USERNAME, USER_PASSWORD);

        createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(72), hoursFromNow(73));
        createReservation(user2Token, VEHICLE_RESOURCE_ID, hoursFromNow(72), hoursFromNow(73));

        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.equalTo(USER1_USERNAME))));
    }

    @Test
    void list_asAdmin_returnsReservationsFromAllUsers() throws Exception {
        String user1Token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        String user2Token = obtainToken(USER2_USERNAME, USER_PASSWORD);
        String adminToken = obtainToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(96), hoursFromNow(97));
        createReservation(user2Token, VEHICLE_RESOURCE_ID, hoursFromNow(96), hoursFromNow(97));

        mockMvc.perform(get("/api/reservations")
                        .param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == '" + USER1_USERNAME + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.username == '" + USER2_USERNAME + "')]").exists());
    }

    @Test
    void getById_otherUsersReservation_asUser_isForbidden() throws Exception {
        String user1Token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        String user2Token = obtainToken(USER2_USERNAME, USER_PASSWORD);

        JsonNode reservation = createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(120), hoursFromNow(121));
        long id = reservation.get("id").asLong();

        mockMvc.perform(get("/api/reservations/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user2Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_ownReservation_succeeds() throws Exception {
        String user1Token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        JsonNode reservation = createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(122), hoursFromNow(123));
        long id = reservation.get("id").asLong();

        mockMvc.perform(get("/api/reservations/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user1Token)))
                .andExpect(status().isOk());
    }

    @Test
    void filter_byStatusAndPriceRange() throws Exception {
        String user1Token = obtainToken(USER1_USERNAME, USER_PASSWORD);

        // 2 hours on the 25.00/hr room => price 50.00
        JsonNode toCancel = createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(200), hoursFromNow(202));
        cancelReservation(user1Token, toCancel.get("id").asLong());

        // 1 hour on the 15.00/hr vehicle => price 15.00
        createReservation(user1Token, VEHICLE_RESOURCE_ID, hoursFromNow(200), hoursFromNow(201));

        mockMvc.perform(get("/api/reservations")
                        .param("status", "CANCELLED")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.equalTo("CANCELLED"))));

        mockMvc.perform(get("/api/reservations")
                        .param("minPrice", "40")
                        .param("maxPrice", "60")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].price", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.comparesEqualTo(50.0))));
    }

    @Test
    void pagination_and_sorting_areApplied() throws Exception {
        String user1Token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(300), hoursFromNow(301));
        createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(302), hoursFromNow(303));
        createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(304), hoursFromNow(305));

        mockMvc.perform(get("/api/reservations")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "startTime,desc")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void cancel_ownReservation_asUser_succeeds() throws Exception {
        String token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        JsonNode reservation = createReservation(token, ROOM_RESOURCE_ID, hoursFromNow(400), hoursFromNow(401));
        long id = reservation.get("id").asLong();

        mockMvc.perform(patch("/api/reservations/{id}/cancel", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(patch("/api/reservations/{id}/cancel", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancel_otherUsersReservation_asUser_isForbidden() throws Exception {
        String user1Token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        String user2Token = obtainToken(USER2_USERNAME, USER_PASSWORD);
        JsonNode reservation = createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(402), hoursFromNow(403));

        mockMvc.perform(patch("/api/reservations/{id}/cancel", reservation.get("id").asLong())
                        .header(HttpHeaders.AUTHORIZATION, bearer(user2Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_asUser_isForbidden_butAdminCanFullyUpdate() throws Exception {
        String user1Token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        String adminToken = obtainToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        JsonNode reservation = createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(500), hoursFromNow(501));
        long id = reservation.get("id").asLong();

        ReservationUpdateRequest updateRequest = new ReservationUpdateRequest();
        updateRequest.setResourceId(ROOM_RESOURCE_ID);
        updateRequest.setStartTime(hoursFromNow(500));
        updateRequest.setEndTime(hoursFromNow(501));
        updateRequest.setStatus(ReservationStatus.CONFIRMED);

        mockMvc.perform(put("/api/reservations/{id}", id)
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user1Token))
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/reservations/{id}", id)
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void delete_asUser_isForbidden_butAdminCanDelete() throws Exception {
        String user1Token = obtainToken(USER1_USERNAME, USER_PASSWORD);
        String adminToken = obtainToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        JsonNode reservation = createReservation(user1Token, ROOM_RESOURCE_ID, hoursFromNow(600), hoursFromNow(601));
        long id = reservation.get("id").asLong();

        mockMvc.perform(delete("/api/reservations/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user1Token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reservations/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    private JsonNode createReservation(String token, Long resourceId, LocalDateTime start, LocalDateTime end) throws Exception {
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setResourceId(resourceId);
        request.setStartTime(start);
        request.setEndTime(end);

        String response = mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response);
    }

    private void cancelReservation(String token, long id) throws Exception {
        mockMvc.perform(patch("/api/reservations/{id}/cancel", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private LocalDateTime hoursFromNow(int hours) {
        return LocalDateTime.now().plusHours(hours).withNano(0);
    }

    private void assertThatEquals(JsonNode node, String field, String expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, node.get(field).asText());
    }
}

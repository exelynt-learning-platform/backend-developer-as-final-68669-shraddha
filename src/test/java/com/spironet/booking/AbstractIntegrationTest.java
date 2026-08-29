package com.spironet.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "Admin@123";
    protected static final String USER1_USERNAME = "user1";
    protected static final String USER2_USERNAME = "user2";
    protected static final String USER_PASSWORD = "User@123";

    protected String obtainToken(String username, String password) throws Exception {
        String payload = objectMapper.writeValueAsString(new LoginPayload(username, password));

        String response = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("accessToken").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    private record LoginPayload(String username, String password) {
    }
}

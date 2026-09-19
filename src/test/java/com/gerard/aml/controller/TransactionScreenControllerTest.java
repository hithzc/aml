package com.gerard.aml.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerard.aml.domain.TransactionScreenRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "aml.rules.file=classpath:valid-rules.json")
class TransactionScreenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void screenTransactionHighValueIsReviewed() throws Exception {
        TransactionScreenRequest request = new TransactionScreenRequest(
                "TX-10001",
                "C-12345",
                new BigDecimal("150000"),
                "EUR",
                "SE",
                "GB",
                "ONLINE"
        );

        mockMvc.perform(post("/api/v1/transactions/screen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REVIEW"))
                .andExpect(jsonPath("$.matchedRules[0]").value("HIGH_VALUE_TRANSACTION"))
                .andExpect(jsonPath("$.transactionId").value("TX-10001"));
    }
}

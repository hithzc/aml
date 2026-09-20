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

    private static final String ENDPOINT = "/api/v1/transactions/screen";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void screenTransactionHighValueIsReviewed() throws Exception {
        TransactionScreenRequest request = new TransactionScreenRequest(
                "TX-10001", "C-12345", new BigDecimal("150000"), "EUR", "SE", "GB", "ONLINE"
        );

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REVIEW"))
                .andExpect(jsonPath("$.matchedRules[0]").value("HIGH_VALUE_TRANSACTION"))
                .andExpect(jsonPath("$.transactionId").value("TX-10001"));
    }

    @Test
    void screenTransactionLowValueSameCountryIsCleared() throws Exception {
        TransactionScreenRequest request = new TransactionScreenRequest(
                "TX-10002", "C-12345", new BigDecimal("100"), "EUR", "SE", "SE", "BRANCH"
        );

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedRules").isEmpty());
    }

    @Test
    void screenTransactionMatchesMultipleRules() throws Exception {
        TransactionScreenRequest request = new TransactionScreenRequest(
                "TX-10003", "C-12345", new BigDecimal("200000"), "EUR", "SE", "GB", "ONLINE"
        );

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedRules.length()").value(3))
                .andExpect(jsonPath("$.matchedRules")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                "HIGH_VALUE_TRANSACTION",
                                "HIGH_RISK_COUNTRY",
                                "SUSPICIOUS_ONLINE_TRANSACTION"
                        )));
    }

    @Test
    void screenTransactionWithBlankTransactionIdReturns400() throws Exception {
        TransactionScreenRequest request = new TransactionScreenRequest(
                "", "C-12345", new BigDecimal("1000"), "EUR", "SE", "SE", "BRANCH"
        );

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details.transactionId").exists());
    }

    @Test
    void screenTransactionWithNegativeAmountReturns400() throws Exception {
        TransactionScreenRequest request = new TransactionScreenRequest(
                "TX-10004", "C-12345", new BigDecimal("-500"), "EUR", "SE", "SE", "BRANCH"
        );

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").exists());
    }

    @Test
    void screenTransactionWithMissingRequiredFieldsReturns400() throws Exception {
        String incompleteJson = """
            {
              "amount": 1000
            }
            """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleteJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.transactionId").exists())
                .andExpect(jsonPath("$.details.customerId").exists())
                .andExpect(jsonPath("$.details.currency").exists())
                .andExpect(jsonPath("$.details.originCountry").exists())
                .andExpect(jsonPath("$.details.destinationCountry").exists())
                .andExpect(jsonPath("$.details.channel").exists());
    }
}
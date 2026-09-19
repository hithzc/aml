package com.gerard.aml.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionScreenResponse {
    private String transactionId;
    private Decision decision;
    private List<String> matchedRules;
}

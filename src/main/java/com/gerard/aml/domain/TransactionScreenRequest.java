package com.gerard.aml.domain;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionScreenRequest {
    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotBlank(message = "originCountry is required")
    private String originCountry;

    @NotBlank(message = "destinationCountry is required")
    private String destinationCountry;

    @NotBlank(message = "channel is required")
    private String channel;
}


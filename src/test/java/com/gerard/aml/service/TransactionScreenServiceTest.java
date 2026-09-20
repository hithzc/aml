package com.gerard.aml.service;

import com.gerard.aml.domain.Decision;
import com.gerard.aml.domain.TransactionScreenRequest;
import com.gerard.aml.domain.TransactionScreenResponse;
import com.gerard.aml.rule.RuleEngine;
import com.gerard.aml.rule.RuleEvaluationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionScreenServiceTest {

    @Mock
    private RuleEngine ruleEngine;

    private TransactionScreenService service;

    private TransactionScreenRequest sampleRequest() {
        return new TransactionScreenRequest(
                "TX-1", "C-1", new BigDecimal("1000"), "EUR", "SE", "SE", "BRANCH"
        );
    }

    @Test
    void screen_returnsClearDecisionWhenNoRulesMatch() {
        service = new TransactionScreenService(ruleEngine);
        when(ruleEngine.evaluate(any())).thenReturn(List.of());

        TransactionScreenResponse response = service.screen(sampleRequest());

        assertThat(response.getDecision()).isEqualTo(Decision.CLEAR);
        assertThat(response.getMatchedRules()).isEmpty();
    }

    @Test
    void screen_returnsReviewDecisionWhenAtLeastOneRuleMatches() {
        service = new TransactionScreenService(ruleEngine);
        when(ruleEngine.evaluate(any())).thenReturn(List.of("HIGH_VALUE_TRANSACTION"));

        TransactionScreenResponse response = service.screen(sampleRequest());

        assertThat(response.getDecision()).isEqualTo(Decision.REVIEW);
        assertThat(response.getMatchedRules()).containsExactly("HIGH_VALUE_TRANSACTION");
    }

    @Test
    void screen_returnsReviewDecisionWhenMultipleRulesMatch() {
        service = new TransactionScreenService(ruleEngine);
        when(ruleEngine.evaluate(any())).thenReturn(
                List.of("HIGH_VALUE_TRANSACTION", "HIGH_RISK_COUNTRY"));

        TransactionScreenResponse response = service.screen(sampleRequest());

        assertThat(response.getDecision()).isEqualTo(Decision.REVIEW);
        assertThat(response.getMatchedRules())
                .containsExactlyInAnyOrder("HIGH_VALUE_TRANSACTION", "HIGH_RISK_COUNTRY");
    }

    @Test
    void screen_preservesTransactionIdFromRequest() {
        service = new TransactionScreenService(ruleEngine);
        when(ruleEngine.evaluate(any())).thenReturn(List.of());
        TransactionScreenRequest request = sampleRequest();

        TransactionScreenResponse response = service.screen(request);

        assertThat(response.getTransactionId()).isEqualTo(request.getTransactionId());
    }

    @Test
    void screen_propagatesRuleEvaluationExceptionFromRuleEngine() {
        service = new TransactionScreenService(ruleEngine);
        when(ruleEngine.evaluate(any()))
                .thenThrow(new RuleEvaluationException("boom", new RuntimeException()));

        assertThatThrownBy(() -> service.screen(sampleRequest()))
                .isInstanceOf(RuleEvaluationException.class)
                .hasMessageContaining("boom");
    }
}

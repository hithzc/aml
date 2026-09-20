package com.gerard.aml.rule;

import com.gerard.aml.domain.TransactionScreenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleEngineTest {

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();
    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine(resourceLoader, "classpath:valid-rules.json");
        ruleEngine.validateRulesOnStartup();
    }

    @Test
    void evaluate_shouldMatchHighValueTransactionRule() {
        TransactionScreenRequest tx = new TransactionScreenRequest(
                "TX-1", "C-1", new BigDecimal("150000"), "EUR", "SE", "SE", "BRANCH"
        );

        List<String> matched = ruleEngine.evaluate(tx);

        assertThat(matched).contains("HIGH_VALUE_TRANSACTION");
        assertThat(matched).doesNotContain("HIGH_RISK_COUNTRY");
    }

    @Test
    void evaluate_shouldMatchHighRiskCountryRule() {
        TransactionScreenRequest tx = new TransactionScreenRequest(
                "TX-2", "C-2", new BigDecimal("100"), "EUR", "SE", "GB", "BRANCH"
        );

        List<String> matched = ruleEngine.evaluate(tx);

        assertThat(matched).contains("HIGH_RISK_COUNTRY");
        assertThat(matched).doesNotContain("HIGH_VALUE_TRANSACTION");
    }

    @Test
    void evaluate_shouldMatchSuspiciousOnlineTransactionRule() {
        TransactionScreenRequest tx = new TransactionScreenRequest(
                "TX-3", "C-3", new BigDecimal("6000"), "EUR", "SE", "GB", "ONLINE"
        );

        List<String> matched = ruleEngine.evaluate(tx);

        assertThat(matched).contains("SUSPICIOUS_ONLINE_TRANSACTION");
        assertThat(matched).contains("HIGH_RISK_COUNTRY");
    }

    @Test
    void evaluate_shouldMatchMultipleRulesSimultaneously() {
        TransactionScreenRequest tx = new TransactionScreenRequest(
                "TX-4", "C-4", new BigDecimal("200000"), "EUR", "SE", "GB", "ONLINE"
        );

        List<String> matched = ruleEngine.evaluate(tx);

        assertThat(matched).containsExactlyInAnyOrder(
                "HIGH_VALUE_TRANSACTION",
                "HIGH_RISK_COUNTRY",
                "SUSPICIOUS_ONLINE_TRANSACTION"
        );
    }

    @Test
    void evaluate_shouldReturnEmptyListWhenNoRulesMatch() {
        TransactionScreenRequest tx = new TransactionScreenRequest(
                "TX-5", "C-5", new BigDecimal("50"), "EUR", "SE", "SE", "BRANCH"
        );

        List<String> matched = ruleEngine.evaluate(tx);

        assertThat(matched).isEmpty();
    }

    @Test
    void loadRules_shouldThrowWhenFileDoesNotExist() {
        RuleEngine brokenEngine = new RuleEngine(resourceLoader, "classpath:does-not-exist.json");

        assertThatThrownBy(brokenEngine::validateRulesOnStartup)
                .isInstanceOf(RuleEvaluationException.class)
                .hasMessageContaining("Unable to load AML rules from file");
    }

    @Test
    void validateRulesOnStartup_shouldThrowOnDuplicateRuleNames() {
        RuleEngine duplicateEngine = new RuleEngine(resourceLoader, "classpath:duplicate-rules.json");

        assertThatThrownBy(duplicateEngine::validateRulesOnStartup)
                .isInstanceOf(RuleEvaluationException.class)
                .hasMessageContaining("Duplicate rule name detected");
    }

    @Test
    void validateRulesOnStartup_shouldThrowWhenExpressionDoesNotEvaluateToBoolean() {
        RuleEngine invalidEngine = new RuleEngine(resourceLoader, "classpath:non-boolean-rules.json");

        assertThatThrownBy(invalidEngine::validateRulesOnStartup)
                .isInstanceOf(RuleEvaluationException.class)
                .hasMessageContaining("must evaluate to a boolean result");
    }

    @Test
    void validateRulesOnStartup_shouldThrowOnInvalidExpressionSyntax() {
        RuleEngine invalidEngine = new RuleEngine(resourceLoader, "classpath:invalid-syntax-rules.json");

        assertThatThrownBy(invalidEngine::validateRulesOnStartup)
                .isInstanceOf(RuleEvaluationException.class);
    }
}
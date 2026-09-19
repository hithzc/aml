package com.gerard.aml.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gerard.aml.rule.RuleEvaluationException;
import com.gerard.aml.rule.RuleEngine;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class TransactionScreenControllerFailureTest {

    @Test
    void invalidRuleConfigFailsStartup() {
        RuleEngine ruleEngine = new RuleEngine(new DefaultResourceLoader(), "classpath:invalid-rules.json");

        RuleEvaluationException exception = assertThrows(RuleEvaluationException.class, ruleEngine::validateRulesOnStartup);
        assertTrue(exception.getMessage().contains("Failed to validate rule"));
    }
}

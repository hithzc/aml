package com.gerard.aml.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerard.aml.domain.TransactionScreenRequest;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResourceLoader resourceLoader;
    private final String rulesFile;
    private volatile List<Rule> cachedRules;

    public RuleEngine(ResourceLoader resourceLoader,
                      @Value("${aml.rules.file:classpath:rules.json}") String rulesFile) {
        this.resourceLoader = resourceLoader;
        this.rulesFile = rulesFile;
    }

    @PostConstruct
    public void validateRulesOnStartup() {
        List<Rule> rules = loadRules();
        validateUniqueRuleNames(rules);

        TransactionScreenRequest validationRequest = new TransactionScreenRequest(
                "TX-STARTUP",
                "C-STARTUP",
                new BigDecimal("150000"),
                "EUR",
                "SE",
                "GB",
                "ONLINE"
        );
        StandardEvaluationContext ctx = new StandardEvaluationContext(validationRequest);

        for (Rule rule : rules) {
            try {
                Expression expression = parser.parseExpression(rule.getExpression());
                Object value = expression.getValue(ctx);
                if (!(value instanceof Boolean)) {
                    throw new RuleEvaluationException(
                            "Rule '" + rule.getName() + "' must evaluate to a boolean result, but returned: " + value,
                            new IllegalStateException("Non-boolean rule result: " + value)
                    );
                }
            } catch (RuleEvaluationException e) {
                throw e;
            } catch (Exception e) {
                throw new RuleEvaluationException(
                        "Failed to validate rule '" + rule.getName() + "' with expression: " + rule.getExpression(),
                        e
                );
            }
        }

        this.cachedRules = List.copyOf(rules);
        log.info("Loaded and validated {} AML rules from '{}'", rules.size(), rulesFile);
    }

    public List<String> evaluate(TransactionScreenRequest tx) {
        List<String> matched = new ArrayList<>();
        StandardEvaluationContext ctx = new StandardEvaluationContext(tx);
        List<Rule> rules = (cachedRules != null) ? cachedRules : loadRules();

        for (Rule rule : rules) {
            try {
                Expression exp = parser.parseExpression(rule.getExpression());
                Object value = exp.getValue(ctx);
                if (value instanceof Boolean && (Boolean) value) {
                    matched.add(rule.getName());
                }
            } catch (Exception e) {
                log.error("Failed to evaluate rule '{}' with expression '{}' for transactionId '{}'",
                        rule.getName(), rule.getExpression(), tx.getTransactionId(), e);
                throw new RuleEvaluationException("Rule evaluation failed for rule: " + rule.getName(), e);
            }
        }

        return matched;
    }

    private void validateUniqueRuleNames(List<Rule> rules) {
        Set<String> seenNames = new HashSet<>();
        for (Rule rule : rules) {
            if (rule == null || rule.getName() == null || rule.getName().isBlank()) {
                throw new RuleEvaluationException(
                        "Rule name cannot be null or blank.",
                        new IllegalArgumentException("Rule name cannot be null or blank.")
                );
            }
            if (!seenNames.add(rule.getName())) {
                throw new RuleEvaluationException(
                        "Duplicate rule name detected: '" + rule.getName() + "'. Rule names must be unique.",
                        new IllegalArgumentException("Duplicate rule name: " + rule.getName())
                );
            }
        }
    }

    private List<Rule> loadRules() {
        try {
            Resource resource = resourceLoader.getResource(rulesFile);
            try (InputStream in = resource.getInputStream()) {
                Rule[] rules = objectMapper.readValue(in, Rule[].class);
                return List.of(rules);
            }
        } catch (Exception e) {
            log.error("Unable to load AML rules from '{}'", rulesFile, e);
            throw new RuleEvaluationException("Unable to load AML rules from file: " + rulesFile, e);
        }
    }
}

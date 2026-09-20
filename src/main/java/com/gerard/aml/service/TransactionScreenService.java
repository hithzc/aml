package com.gerard.aml.service;

import com.gerard.aml.domain.Decision;
import com.gerard.aml.domain.TransactionScreenRequest;
import com.gerard.aml.domain.TransactionScreenResponse;
import com.gerard.aml.rule.RuleEngine;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TransactionScreenService {

    private final RuleEngine ruleEngine;

    public TransactionScreenService(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public TransactionScreenResponse screen(TransactionScreenRequest request) {

        List<String> matchedRules = ruleEngine.evaluate(request);
        Decision decision = matchedRules.isEmpty() ? Decision.CLEAR : Decision.REVIEW;

        return new TransactionScreenResponse(request.getTransactionId(), decision, matchedRules);
    }
}

# AML Transaction Screening API

This project is a Spring Boot app that checks a financial transaction against a list of AML (anti-money-laundering) rules. Each rule comes from a JSON file and is run using SpEL (a Spring expression language). If any rule matches, the transaction is marked REVIEW; otherwise it's CLEAR.

## 1. Build the project

Requirements:
- Java 17

Build the project:

```bash
./mvnw clean package
```

This compiles the code, runs the tests, and produces a JAR in `target/`.

## 2. Run the application

Start the application locally:

```bash
RULES_FILE="file:$(pwd)/aml-rules.json" ./mvnw spring-boot:run
```
You can also point to a different rules file by changing the `RULES_FILE` environment variable.

Default application URL:

```text
http://localhost:8080
```

curl example to screen a transaction:

```bash
curl -X POST http://localhost:8080/api/v1/transactions/screen \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId":"TX-10001",
    "customerId":"C-12345",
    "amount":150000,
    "currency":"EUR",
    "originCountry":"SE",
    "destinationCountry":"GB",
    "channel":"ONLINE"
  }'
```


## 3. Run the tests

```bash
./mvnw clean test
```
TransactionScreenServiceTest verfiy how the rules are evaluated and the correct decision is returned. 
TransactionScreenControllerTest verifies that the REST API returns the correct HTTP status codes and response bodies.
AMLApplicationStartupTests verifies that the application fails to start when the rules file is invalid.
RuleEngineTest verifies that the rule engine correctly evaluates rules and returns the expected results.

For example, the following scenarios are covered by unit tests:
- `A transaction matching no rules.` TransactionScreenServiceTest.screen_returnsClearDecisionWhenNoRulesMatch
- `A transaction matching one rule.` TransactionScreenServiceTest.screen_returnsReviewDecisionWhenOneRuleMatches
- `A transaction matching multiple rules.` TransactionScreenServiceTest.screen_returnsReviewDecisionWhenMultipleRulesMatch
- `Invalid input.`  TransactionScreenControllerTest.screenTransactionWithMissingRequiredFieldsReturns400
- `Configuration-dependent behaviour.` AmlApplicationStartupTests


## 4. How rules are configured

Rules live in `aml-rules.json` You can also point to a different rules file by changing the `RULES_FILE` environment variable.:

```json
[
  {
    "name": "HIGH_VALUE_TRANSACTION",
    "expression": "amount >= 100000"
  },
  {
    "name": "HIGH_RISK_COUNTRY",
    "expression": "originCountry != destinationCountry"
  },
  {
    "name": "SUSPICIOUS_ONLINE_TRANSACTION",
    "expression": "channel == 'ONLINE' && amount > 5000 && originCountry != destinationCountry"
  }
]
```


## 5. Architecture and design

### Project structure
```text
src/
  main/
    java/com/gerard/aml/
      controller/
      domain/
      rule/
      service/
      AmlApplication
    resources/
      application.yml
  test/
    java/com/gerard/aml/
      controller/
      rule/
      service/
      AmlApplicationStartupTests
    resources/
      duplicate-rules.json
      invalid-rules.json
      invalid-syntax-rules.json
      non-boolean-rules.json
      valid-rules.json
aml-rules.json      
```

- `controller` — REST API entry point, bean validation, and response formatting
- `service` — processing the request with the rule engine and returning a decision
- `rule` — rule loading and evaluation engine
- `domain` — request and response DTOs
- `resources` — configuration and rule files

### Design choices

-  Rule defined in JSON, not hardcoded: rules can be changed without redeploying the app.
-  SpEL: short, readable expressions that work naturally with Spring.
-  Startup validation: bad rules (duplicates, errors, wrong return type) stop the app immediately, instead of failing silently later in production.


### Request flow

1. HTTP request enters `TransactionScreenController`
2. Controller validates JSON with Bean Validation
3. `TransactionScreenService` calls `RuleEngine.evaluate(request)`
4. `RuleEngine` loads the rule definitions from the configured file
5. Each rule expression is evaluated using SpEL against the transaction request
6. If at least one rule matches, the decision is `REVIEW`; otherwise `CLEAR`


### API Design
#### POST /api/v1/transactions/screen

Request body example:

```json
{
  "transactionId": "TX-10001",
  "customerId": "C-12345",
  "amount": 150000,
  "currency": "EUR",
  "originCountry": "SE",
  "destinationCountry": "GB",
  "channel": "ONLINE"
}
```

Example response:

```json
{
  "transactionId": "TX-10001",
  "decision": "REVIEW",
  "matchedRules": ["HIGH_VALUE_TRANSACTION"]
}
```

## 6. Improvement for production


This project intentionally does not include:

- user authentication/authorization
- database persistence for transactions or rule history
- a rule authoring UI or admin portal
- audit trail / explainability dashboard for every screening decision


If this were going into production, I would prioritize:

- persistent rule versioning and auditing
- immutable rule snapshots and deployment promotion flow
- richer error handling and structured operational logs
- metrics, tracing, and alerting for rule evaluation failures
- database-backed transaction history and review workflow
- geo-distributed or multi-instance deployment with shared configuration
- stronger security controls and authenticated API access
- support for rule authoring and review approvals by compliance teams
- integration with external sanctions and watchlist data sources
- performance benchmarking and rule optimization for scale




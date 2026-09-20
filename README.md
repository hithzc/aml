# AML Transaction Screening API

This project is a Spring Boot application that evaluates a financial transaction against a set of AML rule expressions. Each rule is loaded from a JSON file, evaluated with Spring Expression Language (SpEL), and the result is either `REVIEW` or `CLEAR`.

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


## 3. Run the tests

Run the suite:

```bash
./mvnw test
```


## 4. Architecture and design

- `controller` — REST API entry point
- `service` — orchestration of request processing
- `rule` — rule loading and evaluation engine
- `domain` — request and response DTOs
- `resources` — configuration and rule files

### Request flow

1. HTTP request enters `TransactionScreenController`
2. Controller validates JSON with Bean Validation
3. `TransactionScreenService` calls `RuleEngine.evaluate(request)`
4. `RuleEngine` loads the rule definitions from the configured file
5. Each rule expression is evaluated using SpEL against the transaction request
6. If at least one rule matches, the decision is `REVIEW`; otherwise `CLEAR`

### Design choices

- Rules are externalized to JSON rather than hardcoded in Java so non-developers can tune them without a code deployment
- SpEL is used for rule expressions because it is compact and well integrated with Spring
- Startup validation ensures malformed rules or duplicate names fail fast before the app is considered healthy
- The response is intentionally small and easy for downstream systems to consume

## 5. Important assumptions

This project is intentionally a lightweight sample/POC, not a full production AML platform.

Important assumptions:

- Transaction screening is rule-based and deterministic, not probabilistic or risk-scoring based
- All rule expressions operate on a single request object, not historical account or customer data
- Rule files are trusted and managed by the application owner
- The source of truth for risk logic is the JSON rule file, not a database
- The app is running in a single instance for local or small-scale usage

## 6. How rules are configured

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

## 7. API

### POST /api/v1/transactions/screen

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

curl example:

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

Example response:

```json
{
  "transactionId": "TX-10001",
  "decision": "REVIEW",
  "matchedRules": ["HIGH_VALUE_TRANSACTION"]
}
```

## 8. What was deliberately not implemented

This project intentionally does not include:

- user authentication/authorization
- database persistence for transactions or rule history
- asynchronous processing or event-driven queues
- a rule authoring UI or admin portal
- multi-tenant configuration
- external policy engine integration (e.g. Drools, OPA)
- audit trail / explainability dashboard for every screening decision
- full sanctions screening or PEP matching against external systems

Those concepts are common in production AML systems, but they are outside the scope of this sample implementation.

## 9. What I would improve for production

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

## 10. Project structure

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


# AGENT.md

## Project purpose

This repository is a compact Spring Boot application for AML transaction screening.

It accepts a transaction payload, loads AML rules from a JSON file, evaluates the transaction with Spring Expression Language (SpEL), and returns a decision:
- `CLEAR` when no rules match
- `REVIEW` when one or more rules match

The application is intentionally small and readable, designed as a demo / prototype / learning project rather than a full enterprise AML platform.

## Repository layout

```text
.
├── AGENT.md
├── AI_README.md
├── AI_USAGE.md
├── README.md
├── aml-rules.json
├── pom.xml
├── mvnw
├── mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/com/gerard/aml/
│   │   │   ├── AmlApplication.java
│   │   │   ├── controller/
│   │   │   ├── domain/
│   │   │   ├── rule/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── rules.json
│   └── test/
│       ├── java/com/gerard/aml/
│       │   ├── AmlApplicationStartupTests.java
│       │   ├── controller/
│       │   ├── rule/
│       │   └── service/
│       └── resources/
│           ├── valid-rules.json
│           ├── invalid-rules.json
│           ├── duplicate-rules.json
│           ├── non-boolean-rules.json
│           └── invalid-syntax-rules.json
└── target/
```

## Core runtime flow

1. `AmlApplication` starts Spring Boot.
2. `RuleEngine` is created as a Spring bean.
3. In `@PostConstruct`, the app loads the configured rule file and validates each rule.
4. `TransactionScreenController` receives POST requests.
5. `TransactionScreenService` evaluates the transaction using `RuleEngine`.
6. `RuleEngine` evaluates SpEL rules against the request object.
7. Matching rule names are returned in the response.

## Main classes

### `AmlApplication`
Entry point for the Spring Boot app.

### `TransactionScreenController`
REST endpoint:
- `POST /api/v1/transactions/screen`

### `TransactionScreenService`
Business orchestration layer.
- converts rule matches into a `TransactionScreenResponse`
- decides `CLEAR` vs `REVIEW`

### `RuleEngine`
This is the critical engine class.

Responsibilities:
- load rules from the configured file
- validate the rules at startup
- reject invalid expressions, duplicate names, blank names, and non-boolean results
- evaluate active rules against a transaction request

### `TransactionScreenRequest`
Incoming JSON payload model.
- includes `transactionId`, `customerId`, `amount`, `currency`, `originCountry`, `destinationCountry`, `channel`
- annotated with validation constraints

### `TransactionScreenResponse`
Output model returned by the API.
- includes `transactionId`, `decision`, `matchedRules`

## Rules configuration

Default rule configuration:

```yaml
aml:
  rules:
    file: ${RULES_FILE:classpath:rules.json}
```

This means:
- if `RULES_FILE` is not set, the default is `classpath:rules.json`
- if `RULES_FILE` is set, it can point to an external file like `file:/path/to/rules.json`

Default rule file:
`src/main/resources/rules.json`

Example rule:

```json
[
  {
    "name": "HIGH_VALUE_TRANSACTION",
    "expression": "amount >= 100000"
  }
]
```

Important rules for rule authoring:
- rule names must be unique
- names cannot be blank
- expressions must parse successfully
- expressions must evaluate to a boolean
- string literals must be quoted in SpEL, e.g. `channel == 'ONLINE'`
- avoid `channel == ONLINE` because it is treated as an object/property lookup, not a string literal

## Startup validation behavior

The app intentionally fails fast.

`RuleEngine.validateRulesOnStartup()` validates all rules before the application is considered healthy.

Checks include:
- file exists and can be read
- rule names are unique
- rule names are not blank
- expression syntax is valid
- expression evaluates to a boolean using a representative valid transaction

This prevents startup in a broken rule configuration state.

## Test map

### Startup and configuration tests
- `src/test/java/com/gerard/aml/AmlApplicationStartupTests.java`
  - starts with valid rules
  - fails with invalid rules

### HTTP/controller tests
- `src/test/java/com/gerard/aml/controller/TransactionScreenControllerTest.java`
  - high-value review case
  - clear/no-match case
  - multi-rule case
  - validation failures for blank ID, negative amount, missing fields

### Service tests
- `src/test/java/com/gerard/aml/service/TransactionScreenServiceTest.java`
  - no rules -> `CLEAR`
  - one rule -> `REVIEW`
  - multiple rules -> `REVIEW`
  - preserves transactionId
  - propagates evaluation exceptions

### Rule engine tests
- `src/test/java/com/gerard/aml/rule/RuleEngineTest.java`
  - single match
  - multi-match
  - no-match
  - missing file
  - duplicate rule names
  - non-boolean result
  - invalid expression syntax

## Build, test, and run commands

Build:

```bash
./mvnw clean package
```

Run app:

```bash
./mvnw spring-boot:run
```

Run tests:

```bash
./mvnw test
```

Run with custom file:

```bash
RULES_FILE="file:$(pwd)/aml-rules.json" ./mvnw spring-boot:run
```

## Common pitfalls for agents

1. Missing default rules file
   - If `application.yml` is missing the fallback, Spring may fail before validation runs.
   - Use `classpath:rules.json` as the default.

2. SpEL string literal mistakes
   - Use: `channel == 'ONLINE'`
   - Not: `channel == ONLINE`

3. Test resource precedence
   - In tests, `src/test/resources` can override `src/main/resources` when using `classpath:...` resource names.
   - Be explicit when you need a specific rules file.

4. Duplicate rule names
   - Duplicate names are a startup failure.
   - They are not silently tolerated because rule names are identifiers for matched rules.

5. Invalid rule files
   - Broken regex / SpEL syntax or invalid field references should fail fast during startup.

## Design principles in this project

- simple layered architecture
- externalized rule definitions
- explicit validation before app startup
- small, readable tests
- no database persistence or security layer, by design

## Deliberately out of scope

This project intentionally does not include:
- user authentication / authorization
- database persistence
- audit logs
- external sanctions or watchlists
- queueing / async processing
- multi-tenant configuration
- advanced rule governance

## Production improvements if this were expanded

If this were moving toward production, key improvements would include:
- versioned rule storage with review workflow
- database-backed transaction history and audit trail
- metrics, tracing, and alerting
- stronger security controls and API auth
- rule authoring UI / admin workflow
- integration with external AML data sources

## Working conventions for agents

When making changes:
- prefer targeted edits and targeted tests
- keep the rule file format stable
- validate startup behavior for both good and bad rule configs
- do not silently accept invalid or duplicate rule names
- avoid broad refactors unless necessary for the task

## Summary

This repository is a small, rule-driven AML transaction screening app built with Spring Boot. The most important thing to preserve is fail-fast validation of rule files and clear, predictable behavior when rules match or fail to match. The central logic lives in `RuleEngine`, and the major risk area is configuration and SpEL correctness.

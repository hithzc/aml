# AML Transaction Screening API

A Spring Boot REST API for screening financial transactions against file-based AML rules.

## Overview

This project exposes a transaction screening endpoint that evaluates incoming transaction data using Spring Expression Language (SpEL) and a JSON rule file. If any rule matches, the response returns `REVIEW`; otherwise it returns `CLEAR`.

## Features

- REST API for transaction screening
- JSON-based SpEL rules loaded from file
- Real-time rule loading on each evaluation
- Request validation with Bean Validation
- Centralized API error handling

## Project structure

```text
src/
  main/
    java/com/gerard/aml/
      config/
      controller/
      domain/
      rule/
      service/
      AmlApplication.java
    resources/
      application.yml
      rules.json
  test/
    java/com/gerard/aml/controller/
```

## Prerequisites

- Java 21 recommended (matches the project runtime expectations used during development)
- Maven or Maven Wrapper

## Run locally

```bash
./mvnw clean test
RULES_FILE="file:$(pwd)/aml-rules.json" ./mvnw spring-boot:run
```

The application starts on the default Spring Boot port:

```text
http://localhost:8080
```

## API

### POST /api/v1/transactions/screen

Request body:

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

curl example (POST to localhost:8080):

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

The endpoint returns a JSON decision (e.g., {"transactionId":"TX-10001","decision":"REVIEW","matchedRules":[...]}).

Example success response:

```json
{
  "transactionId": "TX-10001",
  "decision": "REVIEW",
  "matchedRules": ["HIGH_VALUE_TRANSACTION"]
}
```

## Rule configuration

Rules are stored in `override-rules.json`:

```json
[
  {
    "name": "HIGH_VALUE_TRANSACTION",
    "expression": "amount >= 100000"
  }
]
```

The application can also read an external rule file via the `RULES_FILE` environment variable:

```bash
RULES_FILE="file:$(pwd)/aml-rules.json" ./mvnw spring-boot:run
```


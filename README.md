# Project Turnstile — Task 1: Login Risk Engine

## Stack
- Java 17
- Spring Boot 3.3.5
- Maven
- PostgreSQL 15 + Flyway
- H2 in PostgreSQL compatibility mode for tests
- JdbcTemplate, JUnit 5

## Implemented Task 1 features
- `POST /api/v1/risk/login`
- `POST /api/v1/risk/decisions/{id}/step-up`
- `GET /api/v1/risk/decisions?page=0&size=20`
- Four configurable risk signals and decision bands
- Immediate `BAD_CREDENTIALS` denial without scoring
- Event ID idempotency for sequential/repeated requests
- Device trust on ALLOW and successful step-up
- `[eventTime - 15 minutes, eventTime)` failed-attempt window
- First successful login skips impossible travel
- 20 labelled JSON scenario records
- 500-customer / 20,000-event data generator

## Start PostgreSQL
`docker compose up -d postgres`

The test suite does not require Docker:
`mvn test`

Run the service:
`mvn spring-boot:run`

## Create a customer
The assessment does not define a customer-registration endpoint. For local testing:
```sql
INSERT INTO customer(id, full_name) VALUES (1001, 'Demo Customer');
```

## Login request
```json
{
  "eventId": "login-0001",
  "customerId": 1001,
  "deviceId": "browser-device-abc123",
  "countryCode": "IN",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "credentialValid": true,
  "eventTime": "2026-11-14T06:00:00Z"
}
```

## Step-up request
`POST /api/v1/risk/decisions/1/step-up`
```json
{"passed": true}
```

## Generate demo data
Start the application with:
`mvn spring-boot:run -Dspring-boot.run.arguments=--turnstile.generator.enabled=true`

The generator inserts 500 customers and processes 20,000 deterministic login events.

## Decision logic
- `< 40`: ALLOW
- `40..69`: STEP_UP
- `>= 70`: DENY
- Invalid credentials: DENY, score 0, reason `BAD_CREDENTIALS`
- Failed burst window: lower boundary inclusive, event-time upper boundary exclusive
- Only valid-credential attempts are recorded as PASSED; invalid credentials are FAILED.

## Indexes and EXPLAIN
The migration creates:
- `idx_login_passed_customer_time(customer_id, outcome, event_time DESC)`
- `idx_login_failed_customer_time(customer_id, outcome, event_time DESC)`
- `idx_decision_created(created_at DESC, id DESC)`

Run after loading representative data and updating statistics:
```sql
ANALYZE login_attempt;
EXPLAIN SELECT latitude, longitude, event_time
FROM login_attempt
WHERE customer_id = 1001 AND outcome = 'PASSED'
  AND event_time < now()
ORDER BY event_time DESC LIMIT 1;

EXPLAIN SELECT count(*)
FROM login_attempt
WHERE customer_id = 1001 AND outcome = 'FAILED'
  AND event_time >= now() - interval '15 minutes'
  AND event_time < now();
```
Capture the actual plan in the submission README. Index selection depends on row counts and planner statistics; do not claim an index scan without recording the actual PostgreSQL output.

## Idempotency note
The service checks `eventId` before processing, serializes calls inside one JVM, and the database enforces a unique constraint. A multi-instance deployment should add a database advisory lock or an idempotency reservation row with retry-on-unique-violation. This is called out rather than claiming JVM synchronization solves distributed races.

## Known scope boundary
This is Task 1 only. Redis sessions, payment scoring, JWT authorization, masking, revocation, and audit are intentionally deferred to Tasks 2 and 3.

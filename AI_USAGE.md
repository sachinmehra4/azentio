# AI usage disclosure

- Assistant: ChatGPT.
- Used for project scaffolding, implementation suggestions, and test-case review.
- Key prompt: create a Java 17 / Spring Boot login risk scoring service with configurable rules, PostgreSQL/Flyway, H2 tests, idempotency, step-up, and labelled scenarios.
- Human review required: inspect each class, run `mvn test`, verify PostgreSQL EXPLAIN plans with representative data, and explain all design decisions.
- Known limitation: synchronized service methods serialize duplicate requests only inside one application instance. A clustered deployment needs database-level idempotency coordination.
- Do not submit code that has not been compiled, tested, and understood.

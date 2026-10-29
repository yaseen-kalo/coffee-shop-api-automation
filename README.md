# Coffee Shop API Automation

A test automation framework for a Coffee Shop REST API, built with Java, Rest Assured and TestNG.
The API is a mock made with WireMock that runs inside the tests, so you need no other server.

## What it covers

- 14 endpoints: auth (register, login, me), coffees (list, get, create, update, change stock, delete) and orders (place, list, get, cancel, change status)
- Two roles, admin and customer, with Bearer tokens
- Positive, negative and boundary value tests (data-driven with TestNG DataProviders)
- Role access matrix, and JSON schema checks
- Server failures: 500 errors, slow responses with timeouts, dropped connections
- 6 end-to-end flows with stateful mocks (WireMock Scenarios)
- About 240 tests in total

## Tech

Java 17, Maven, TestNG, Rest Assured, Gson, WireMock, JSON Schema Validator, Datafaker, Allure, GitHub Actions

## Design: API Object Model

Like Page Object Model, but for APIs.

```
Test -> Api class -> Rest Assured -> Mock server
```

- **Tests:** only steps and assertions. No raw `given()`.
- **Api classes** (`src/main`): one method per endpoint. They send the request and return the `Response`. They never assert.
- **Models:** plain Java classes (POJOs) that match the JSON.
- **Factories:** build request bodies. One valid payload, and small methods that change one field.
- **Stubs:** all WireMock rules live here. Only these classes know WireMock.
- **Tokens:** no hardcoded tokens in tests.

## Project structure

```
src/main/java/com/codecart/coffee/
    clients/    Api classes (auth, coffees, orders)
    models/     request and response models
    utils/      Tokens
src/test/java/com/codecart/coffee/
    base/       BaseTest (starts the mock, forces Gson, resets stubs)
    mocks/      stub classes
    data/       factories and shared DataProviders
    tests/      auth, coffees, orders, roles, failures, e2e
src/test/resources/schemas/   JSON schemas
```

## How to run

You need Java 17 and Maven.

```bash
mvn clean test                    # all tests
mvn clean test -Dgroups=smoke     # smoke tests only
mvn clean test -Dgroups=e2e       # end-to-end flows only
```

Test groups: `smoke`, `regression`, `e2e`.

## Allure report

```bash
mvn clean test
allure serve target/allure-results
```

Install Allure on a Mac with `brew install allure`.

## CI

GitHub Actions runs:

- smoke tests on every push to a branch
- all tests after a merge to `master`
- all tests every night at about 12:00 CET
- all tests by hand with the "Run workflow" button

Allure results are saved as a build artifact.

## Note

The tests run against my own WireMock mock. They prove that my framework, flows and checks work.
They cannot find bugs in a real backend. The mock has no database, so I use WireMock Scenarios
for flows that need memory. A switch to run the same tests against a real API is planned, but not built yet.
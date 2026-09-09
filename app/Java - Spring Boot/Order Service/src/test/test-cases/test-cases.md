# Order Service — Frozen Behavior Baseline

**Status: FROZEN.** This folder (`src/test/test-cases/`) is the frozen behavior
specification for the Order Service, captured before any modernization change
(Java/Spring Boot upgrade, CVE remediation, or future Azure migration). It was
built from an isolated snapshot of the source tree (not the live workspace) so
that in-flight modernization tasks running in parallel could not influence the
captured behavior.

Everything under `src/test/test-cases/` (this file and `testdata/`) is
immutable from this point forward. Any required correction must go through an
explicit unfreeze → amend → re-validate → re-freeze cycle; there is no side
channel for editing it in place. Phase 3 (`*PostMigrationIT` tests) may only
be **appended** elsewhere in the test tree — never merged into this folder.

## 1. Scope and Source Snapshot

| Item | Value |
|---|---|
| Snapshot source | `app/Java - Spring Boot/Order Service` (copied to an isolated temp folder before analysis) |
| Backend module | Maven project `com.contoso.demo:order-service:1.0.0` |
| Backend stack (as captured) | Java 8, Spring Boot 2.7.18 (Web, Data JPA, Validation), H2 (in-memory), Maven |
| Frontend module | `frontend/` — React 18.3 + Vite 5, plain `fetch`-based API client, no state-management/UI libraries |
| Backend test tool | Maven Surefire via JUnit 5 (`mvn test`) |
| Frontend test tool | Node.js built-in test runner (`npm test` → `node --test`) |
| Frontend build tool | Vite (`npm run build`) |

## 2. Backend Test Inventory (frozen — all passing at capture time)

Command: `mvn test` (run from `app/Java - Spring Boot/Order Service`)

Captured result: **BUILD SUCCESS — Tests run: 20, Failures: 0, Errors: 0, Skipped: 0**

| Test class | Test method | Scenario | Expected outcome |
|---|---|---|---|
| `OrderRepositoryTest` (`@DataJpaTest`) | `findByCustomerReturnsOnlyMatchingOrders` | Save orders for two customers, query by customer | Only the matching customer's orders (2) are returned |
| `OrderRepositoryTest` | `createdAtIsPopulatedOnSave` | Save an `Order` built via the `(customer, amount)` constructor | `createdAt` non-null, `status` defaults to `PENDING` |
| `OrderRepositoryTest` | `createdAtIsPopulatedForRestStyleOrderOnSave` | Save an `Order` built via no-arg constructor + setters (REST-style) | `createdAt` populated by `@PrePersist` even when not set by caller |
| `OrderRepositoryTest` | `statusIsPersisted` | Save an order with `status=COMPLETED`, reload by id | Persisted status round-trips as `COMPLETED` |
| `OrderServiceTest` (`@SpringBootTest`, full context) | `totalForCustomerSumsAmounts` | Persist two orders for `charlie` via the real repository/H2 | `totalForCustomer` sums to `25.50` |
| `OrderServiceUnitTest` (Mockito, no Spring context) | `totalForCustomerReturnsZeroWhenNoOrders` | No orders for customer | Returns `BigDecimal.ZERO` |
| `OrderServiceUnitTest` | `totalForCustomerSumsAllAmounts` | Two orders for `alice` (`120.50` + `80.00`) | Returns `200.50` |
| `OrderServiceUnitTest` | `findByIdDelegatesToRepository` | Repository returns an order for id `1` | Service returns the same order wrapped in `Optional` |
| `OrderServiceUnitTest` | `createPersistsOrder` | Create with any inbound `status` | Service forces `status=PENDING` before calling `repository.save` |
| `OrderServiceUnitTest` | `updateStatusPersistsTheRequestedStatus` | Update status of an existing order (id `4`) to `PROCESSING` | Returns updated order with new status; `save` invoked once |
| `OrderServiceUnitTest` | `updateStatusReturnsEmptyWhenOrderIsMissing` | Update status of unknown id `99` | Returns `Optional.empty()`; `save` never invoked |
| `OrderControllerTest` (`@WebMvcTest`, `OrderService` mocked) | `listReturnsAllOrders` | `GET /api/orders` | `200 OK`, JSON array with 2 items, `$[0].customer == "alice"` |
| `OrderControllerTest` | `getByIdReturns404WhenMissing` | `GET /api/orders/99` when service returns empty | `404 Not Found` |
| `OrderControllerTest` | `totalForCustomerReturnsValue` | `GET /api/orders/customer/alice/total` | `200 OK`, body `"200.50"` |
| `OrderControllerTest` | `createReturns201` | `POST /api/orders` with valid JSON body | `201 Created`, response `customer == "dave"` |
| `OrderControllerTest` | `createRejectsInvalidPayload` | `POST /api/orders` with `{}` | `400 Bad Request` |
| `OrderControllerTest` | `createRejectsBlankCustomerAndNonPositiveAmount` | `POST /api/orders` with `{"customer":"   ","amount":0}` | `400 Bad Request` |
| `OrderControllerTest` | `updateStatusReturnsUpdatedOrder` | `PATCH /api/orders/4/status?status=PROCESSING` | `200 OK`, `$.status == "PROCESSING"` |
| `OrderControllerTest` | `updateStatusReturns404WhenOrderIsMissing` | `PATCH /api/orders/99/status?status=COMPLETED` | `404 Not Found` |
| `OrderControllerTest` | `updateStatusRejectsUnknownStatus` | `PATCH /api/orders/1/status?status=UNKNOWN` | `400 Bad Request` (enum bind failure) |

Fixture references: [`testdata/sample-requests.json`](testdata/sample-requests.json), [`testdata/seeded-orders.json`](testdata/seeded-orders.json).

## 3. Order API Contract (frozen)

Base path: `/api/orders`

| Method | Path | Description | Success | Failure modes |
|---|---|---|---|---|
| GET | `/api/orders` | List all orders | `200 OK` + JSON array | — |
| GET | `/api/orders/{id}` | Get order by id | `200 OK` + order JSON | `404` when id not found |
| GET | `/api/orders/customer/{customer}/total` | Sum of order amounts for a customer | `200 OK` + plain decimal string body (e.g. `200.50`) | Returns `0` (as `BigDecimal.ZERO`) when the customer has no orders — not an error |
| POST | `/api/orders` | Create an order | `201 Created` + created order JSON, `status` forced to `PENDING` regardless of request body | `400 Bad Request` when `customer` is blank/missing or `amount` is missing/`< 0.01` |
| PATCH | `/api/orders/{id}/status?status={value}` | Update order status | `200 OK` + updated order JSON | `404` when id not found; `400` when `status` is not one of `PENDING`/`PROCESSING`/`COMPLETED` |

### Order JSON shape

```json
{
  "id": 1,
  "customer": "alice",
  "amount": 120.50,
  "createdAt": "2026-06-09T12:00:00.000+00:00",
  "status": "PENDING"
}
```

### Validation rules (frozen)

- `customer`: must not be blank (`@NotBlank`).
- `amount`: must not be null and must be `>= 0.01` (`@NotNull @DecimalMin("0.01")`).
- `status` path enum: request-parameter values outside `PENDING`, `PROCESSING`, `COMPLETED` fail Spring enum binding and surface as `400 Bad Request`.
- New orders always start as `PENDING`; the API does not honor a caller-supplied initial status.

## 4. H2 Persistence Assumptions (frozen)

- Datasource: `jdbc:h2:mem:orders;DB_CLOSE_DELAY=-1`, driver `org.h2.Driver`, user `sa`, no password.
- `spring.jpa.hibernate.ddl-auto=update` — schema is derived from the `Order` entity at startup; no migration tool (Flyway/Liquibase) is used.
- Primary key generation: `GenerationType.IDENTITY` (auto-increment `id`).
- `createdAt` is always populated via `@PrePersist` (`initializeCreatedAt`) when not explicitly supplied, regardless of which constructor/setters are used to build the entity.
- `status` is persisted as a `STRING`-mapped enum (`EnumType.STRING`), not ordinal.
- `DataSeeder` (`CommandLineRunner`) inserts 3 rows on every fresh application startup (see [`testdata/seeded-orders.json`](testdata/seeded-orders.json)): two `alice` orders (`PENDING` amount `120.50`, `PROCESSING` amount `80.00`) and one `bob` order (`COMPLETED` amount `42.99`). This seed data is a side effect of full application startup (e.g. `mvn spring-boot:run`), not of the unit/slice/integration tests above, which each start from an empty in-memory schema (`@DataJpaTest`, `@WebMvcTest` with mocked service, or a fresh `@SpringBootTest` context).
- The in-memory H2 instance is not shared across the JVM/test lifecycle — each test class run starts from a clean database.

## 5. Web / CORS Configuration (frozen)

- `WebConfig` (implements the deprecated `WebMvcConfigurerAdapter`, a known upgrade target) registers CORS for `/api/**`: `allowedOrigins("*")`, `allowedMethods("GET", "POST", "PATCH")`.
- Server binds to `127.0.0.1:8080` (`server.address` / `server.port` in `application.properties`).
- H2 console is enabled (`spring.h2.console.enabled=true`) but restricted to local access (`web-allow-others=false`).

No behavior change to the permitted CORS origin, methods, or route coverage is in scope for modernization; the frozen contract requires `/api/**` to keep accepting `GET`, `POST`, `PATCH` from any origin.

## 6. Frontend Test & Build Inventory (frozen — all passing at capture time)

| Command | Location | Purpose | Captured result |
|---|---|---|---|
| `npm install` | `frontend/` | Install dependencies (React 18, Vite 5, `@vitejs/plugin-react`) | Completes; `npm audit` reports 6 known vulnerabilities (2 moderate, 4 high) in build tooling — tracked separately as a CVE-remediation concern, not a behavior regression |
| `npm test` (→ `node --test`) | `frontend/` | Unit tests for pure view-logic helpers in `src/orderView.js` | **4/4 passed**: `search matches customers case-insensitively and IDs exactly`, `status filtering and each sort mode can be combined`, `summary reports totals over the loaded order list`, `deriving visible orders does not mutate the loaded list` |
| `npm run build` (→ `vite build`) | `frontend/` | Production bundle build | Succeeds — 36 modules transformed, emits `dist/index.html`, `dist/assets/*.css`, `dist/assets/*.js` |

Fixture reference: [`testdata/frontend-orders-fixture.json`](testdata/frontend-orders-fixture.json) mirrors the literal fixture embedded in `frontend/src/orderView.test.js`.

### Frontend view-logic contract (`frontend/src/orderView.js`, frozen)

- `getVisibleOrders(orders, { search, status, sort })`:
  - Search matches `customer` case-insensitively as a substring, OR exact string match on `id`.
  - `status` filter is exact match; `"ALL"` disables the filter.
  - `sort` supports `newest` (default/fallback for unknown values), `oldest`, `amount-high`, `amount-low`.
  - Does not mutate the input `orders` array.
- `getOrderSummary(orders)` returns `{ totalOrders, activeOrders, totalValue, averageValue }` where `activeOrders` counts `PENDING` + `PROCESSING` statuses only; on an empty list all four fields are `0`.
- `frontend/src/api.js` calls `/api/orders` (base path) with plain `fetch`; non-OK responses raise an `Error` built from a JSON `message`/`error` field, else response text, else a generic `Request failed (<status> <statusText>)` message.

## 7. External Dependency Infra-Decision Table (frozen)

This plan has **no provisioned Azure environment**: no Azure resources exist and no Azure credentials are available. Per the setupBaseline task requirements, every external dependency the Order Service touches is frozen to a **MOCKED** (or locally embedded, in-process equivalent) strategy for the post-migration verification phase. No test — baseline or post-migration — may attempt a live Azure connection.

| External dependency | Current (pre-migration) implementation | Decision | Justification |
|---|---|---|---|
| Relational database | H2 in-memory (`jdbc:h2:mem:orders`) | **MOCKED** — keep using the embedded H2 in-memory database for all test execution (`@DataJpaTest`, `@SpringBootTest`, controller slice tests) | No Azure SQL/Postgres resource is provisioned; H2 already behaves as a fully in-process, disposable substitute matching the "MOCKED" tier and requires no credentials |
| Any future Azure Database for PostgreSQL/SQL target | Not present in current code | **MOCKED** (not applicable / deferred) | No migration to a managed Azure database is in this plan's scope; if introduced later, tests must use an embedded/in-memory or Testcontainers-style substitute, never a live Azure connection, until credentials are explicitly provisioned |
| Azure Storage / Blob (if referenced by future modernization) | Not present in current code | **MOCKED** (not applicable) | No such dependency exists in the captured source; recorded here to keep the decision table exhaustive for the verification phase |
| Azure App Configuration / Key Vault (if referenced by future modernization) | Not present in current code | **MOCKED** (not applicable) | Same as above — no such dependency exists today |
| Frontend → Backend HTTP calls (`frontend/src/api.js`) | Direct `fetch` to `/api/orders*` on the same-origin dev proxy (Vite dev proxy to `localhost:8080`) | **MOCKED for frontend unit tests** (already the case — `orderView.test.js` tests pure functions with static fixtures, no network calls); backend remains the real in-process Spring Boot test context for backend tests | Frontend unit tests never hit the network today; this behavior must be preserved unchanged |
| Log4j Core / Commons Text (transitive logging deps, flagged for separate CVE remediation) | Direct Maven dependencies, not a network external dependency | **N/A — not an external service dependency** | Out of scope for the mock-strategy table; tracked under the separate CVE remediation task |

**Rule for Phase 3 (`*PostMigrationIT`):** every scenario in section 2 and section 6 must be re-provable against the migrated implementation using only the MOCKED/embedded substitutes listed above — no `*PostMigrationIT` test may require a live Azure subscription, resource, or credential.

## 8. Traceability

| Frozen artifact | Source it was captured from (in the isolated snapshot) |
|---|---|
| Section 2 table | `src/test/java/com/contoso/demo/orderservice/repository/OrderRepositoryTest.java`, `.../service/OrderServiceTest.java`, `.../service/OrderServiceUnitTest.java`, `.../web/OrderControllerTest.java` |
| Section 3 contract | `src/main/java/com/contoso/demo/orderservice/web/OrderController.java`, `.../model/Order.java`, `.../model/OrderStatus.java`, `README.md` (API Reference) |
| Section 4 persistence | `src/main/resources/application.properties`, `src/main/java/com/contoso/demo/orderservice/model/Order.java`, `.../config/DataSeeder.java` |
| Section 5 web/CORS | `src/main/java/com/contoso/demo/orderservice/config/WebConfig.java`, `src/main/resources/application.properties` |
| Section 6 frontend | `frontend/package.json`, `frontend/src/orderView.js`, `frontend/src/orderView.test.js`, `frontend/src/api.js` |
| Section 7 infra decisions | Task requirements for `002-setup-behavior-baseline` (no provisioned Azure environment) |

## 9. Non-Goals (explicitly out of scope, unchanged from `README.md`)

Authentication, durable production storage, customer/product/line-item models, status history, server-side querying/pagination, analytics endpoints, charts, bulk actions, exports, and notifications are out of scope for the Order Service and therefore also out of scope for this baseline and any `*PostMigrationIT` verification derived from it.

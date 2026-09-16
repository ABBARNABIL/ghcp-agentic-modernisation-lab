# Java Upgrade Result

> **Executive Summary**\
> This report documents the successful upgrade of the Order Service backend from Java 8 / Spring Boot 2.7.18 to **Java 25** / **Spring Boot 4.1.1**, passing through a Spring Boot 3.5.16 / Java 21 intermediate hop to safely complete the `javax`→`jakarta` namespace migration and the `WebMvcConfigurerAdapter`→`WebMvcConfigurer` replacement before the Spring Boot 4 major-version jump. The upgrade eliminates the unsupported Java 8 / Spring Boot 2.7 (OSS-support-ended) baseline, adopts the Jakarta EE 11 API baseline, and removes all deprecated-API usage flagged for this task. All 20 pre-existing backend tests pass unchanged (100%, matching the pre-migration baseline) on an actual JDK 25.0.2 distribution, a full `mvn clean package` produces a runnable JAR, and the existing REST endpoints, validation semantics, H2-backed persistence behavior, and CORS configuration are preserved exactly as before.

## 1. Upgrade Improvements

Successfully upgraded the Order Service backend from Java 8 (EOL) and Spring Boot 2.7.18 (OSS support ended) to Java 25 (current LTS) and Spring Boot 4.1.1 (latest stable Spring Boot 4.x), via a Spring Boot 3.5.16 / Java 21 intermediate hop. The `javax.*` namespace was fully migrated to `jakarta.*`, the deprecated `WebMvcConfigurerAdapter` was replaced with `WebMvcConfigurer`, and the Spring Boot 4.0 test-infrastructure modularization (`@MockBean` removal, `@WebMvcTest`/`@DataJpaTest` package relocation) was addressed.

| Area | Before | After | Improvement |
| ---- | ------ | ----- | ----------- |
| JDK | Java 8 (EOL) | Java 25 (LTS) | Modern runtime, long-term security support, access to current language/VM features |
| Spring Boot | 2.7.18 (OSS support ended) | 4.1.1 (latest stable 4.x) | Actively supported framework line, Jakarta EE 11 baseline, Spring Framework 7.x |
| Persistence API | `javax.persistence.*` | `jakarta.persistence.*` | Required namespace for Spring Boot 3.x/4.x; no functional change |
| Validation API | `javax.validation.*` | `jakarta.validation.*` | Required namespace for Spring Boot 3.x/4.x; no functional change |
| MVC configuration | `WebMvcConfigurerAdapter` (deprecated, then removed) | `WebMvcConfigurer` (interface) | Drop-in replacement; identical CORS behavior (`/api/**`, origin `*`, GET/POST/PATCH) |
| Web starter | `spring-boot-starter-web` (deprecated in 4.x) | `spring-boot-starter-webmvc` | Aligns with official Spring Boot 4.0 starter renaming |
| Test mocking | `@MockBean` (removed in Spring Boot 4.0) | `@MockitoBean` | Required replacement; identical mocking semantics |
| Test infrastructure | `@WebMvcTest`/`@DataJpaTest` via `spring-boot-starter-test` | Dedicated `spring-boot-starter-webmvc-test` / `spring-boot-starter-data-jpa-test` | Matches Spring Boot 4's modularized test-starter design |
| Maven compiler target | 8 | 25 | Compiles/targets the requested final Java version |

### Key Benefits

**Performance & Security**
- Eliminated exposure to Java 8 and Spring Boot 2.7 end-of-support risk
- Java 25 LTS receives ongoing security patches; Spring Boot 4.1.1 is the actively maintained release line
- CVE scan of the framework-managed dependencies actually touched by this upgrade (spring-boot-starter-webmvc, spring-boot-starter-data-jpa, spring-boot-starter-validation, hibernate-core, h2) found **zero new CVEs** introduced by the upgrade itself

**Developer Productivity**
- Modern Jakarta EE 11 / Spring Framework 7.x APIs with continued official support and documentation
- Access to Java 25 language/runtime improvements for future development
- Clean starter-module alignment reduces future upgrade friction

**Future-Ready Foundation**
- Runtime and framework now on the latest stable, fully supported lines requested by the user
- H2 database version now follows the Spring Boot BOM automatically (2.4.240 resolved)
- No outstanding deprecated-API usage remains in the code paths covered by this task's scope

## 2. Build and Validation

### Build Validation

| Field      | Value |
| ---------- | ----- |
| Status     | ✅ Success |
| Compiler   | Java 25.0.2 (Eclipse/Microsoft build; actual JDK 25 distribution, not JDK 26) |
| Build Tool | Apache Maven 3.9.16 (system install; no wrapper present in project) |
| Result     | `mvn clean package` — all main and test sources compiled successfully, packaged runnable JAR `order-service-1.0.0.jar` (56.8 MB) produced with no errors |

### Test Validation

| Field          | Value |
| -------------- | ----- |
| Status         | ✅ Success |
| Total Tests    | 20 |
| Passed         | 20 |
| Failed         | 0 |
| Test Framework | JUnit 5 + Mockito + Spring Boot Test (MockMvc, DataJpaTest) |

| Test class | Tests | Result |
| ---------- | ----- | ------ |
| OrderRepositoryTest | 4 | ✅ Passed |
| OrderServiceTest | 1 | ✅ Passed |
| OrderServiceUnitTest | 6 | ✅ Passed |
| OrderControllerTest | 9 | ✅ Passed |

Baseline (pre-upgrade, JDK 8, Spring Boot 2.7.18): 20/20 passed. Post-upgrade (JDK 25, Spring Boot 4.1.1): 20/20 passed — 100% pass rate, no regressions, matches baseline exactly.

---

## 3. Limitations

- **CVE remediation intentionally out of scope** (By design)
  - `log4j-core:2.14.1` (7 CVEs, including Log4Shell CVE-2021-44228) and `commons-text:1.9` (CVE-2022-42889) remain pinned at their pre-existing intentionally-vulnerable versions.
  - These are explicit demo pins reserved for a separate, dependent task (`003-remediate-dependency-cves`) per this task's delegation scope and the pom.xml's own comment ("Intentionally pinned to a vulnerable version for CVE remediation exercises. Do not use in production.").
  - Not a limitation of the Java/Spring Boot upgrade itself — confirmed via CVE scan that no new vulnerabilities were introduced by the framework upgrade.

- **Line coverage not measured** (Tooling gap, not a regression)
  - The project does not have a JaCoCo (or equivalent) plugin configured, so a numeric line-coverage percentage could not be produced for this report.
  - All 20 pre-existing tests continue to pass with no reduction in test count or scope.

None of the above are functional regressions in the upgraded application; both are pre-existing conditions outside this task's explicit scope.

---

## 4. Recommended next steps

I. **Complete dependent CVE remediation task**: Run task `003-remediate-dependency-cves` to upgrade `log4j-core` (→ 2.25.4) and `commons-text` (→ 1.10.0) and resolve the 8 known CVEs intentionally left in place for that task.

II. **Consider adding a code-coverage tool**: Add JaCoCo (or equivalent) to the Maven build to track line/branch coverage going forward; current line coverage could not be measured because no coverage plugin is configured.

III. **Adopt modern Java 25 / Spring Boot 4 features where valuable**: e.g., virtual threads, pattern matching, and the newly modularized Spring Boot starters, as the codebase evolves.

IV. **Review Jackson 3 default**: Spring Boot 4 prefers Jackson 3 by default; no source-level impact was found for this project's simple JSON model, but keep this in mind if custom (de)serializers are added later.

---

## 5. Additional details

<details>
<summary>Click to expand for upgrade details</summary>

### Project Details

| Field                 | Value                            |
| --------------------- | -------------------------------- |
| Session ID            | 20260909171704                     |
| Upgrade executed by   | nabilabbar                       |
| Upgrade performed by  | GitHub Copilot                   |
| Project path          | app/Java - Spring Boot/Order Service |
| Repository            | ABBARNABIL/ghcp-agentic-modernisation-lab |
| Build tool (before)   | Maven 3.9.16 (system)            |
| Build tool (after)    | Maven 3.9.16 (system, unchanged) |
| Files modified        | 7                                 |
| Lines added / removed | +42 / -32                        |
| Branch created        | modernize/java-20260909170843 (provided by coordinator, not created by this agent) |

### Code Changes

1. **`pom.xml`**
   - **Changes:** `spring-boot-starter-parent` 2.7.18 → 3.5.16 → 4.1.1; `java.version`/compiler source-target 8 → 21 → 25; `spring-boot-starter-web` → `spring-boot-starter-webmvc`; added `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test` (test scope); `log4j-core`/`commons-text` pins left unchanged (out of scope)

2. **`src/main/java/.../model/Order.java`**
   - **Changes:** `javax.persistence.*` → `jakarta.persistence.*`; `javax.validation.constraints.*` → `jakarta.validation.constraints.*`

3. **`src/main/java/.../web/OrderController.java`**
   - **Changes:** `javax.validation.Valid` → `jakarta.validation.Valid`

4. **`src/main/java/.../config/WebConfig.java`**
   - **Changes:** `extends WebMvcConfigurerAdapter` → `implements WebMvcConfigurer`; `addCorsMappings` body unchanged (`/api/**`, `allowedOrigins("*")`, `allowedMethods("GET","POST","PATCH")`)

5. **`src/test/java/.../web/OrderControllerTest.java`**
   - **Changes:** `@MockBean` → `@MockitoBean`; `@WebMvcTest` import updated to `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`

6. **`src/test/java/.../repository/OrderRepositoryTest.java`**
   - **Changes:** `@DataJpaTest` import updated to `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`

7. **`README.md`**
   - **Changes:** Tech Stack table and Prerequisites updated to reflect Java 25 / Spring Boot 4.1.1

All changes are committed to `modernize/java-20260909170843` and are ready for review. The frozen baseline folder `src/test/test-cases/` was not modified.

### Automated tasks

- Framework/runtime version upgrade (Java 8 → 21 → 25, Spring Boot 2.7.18 → 3.5.16 → 4.1.1)
- `javax.*` → `jakarta.*` namespace migration
- Deprecated API replacement (`WebMvcConfigurerAdapter` → `WebMvcConfigurer`, `@MockBean` → `@MockitoBean`)
- Starter dependency renames/additions for Spring Boot 4 modularization
- Documentation updates (README.md)
- CVE scan of the dependencies actually managed by this upgrade

### Potential Issues

#### CVEs

**Scan Status**: ⚠️ Partial resolution (by design — remediation of pre-existing intentional pins deferred to task `003-remediate-dependency-cves`, out of scope for this task)

**Scanned**: 7 direct/managed dependencies touched by this upgrade | **Found**: 8 (all in pre-existing intentional pins) | **Auto-fixed**: 0 (out of scope) | **Remaining**: 8

| Severity | CVE ID | Dependency | Before | After | Status |
| -------- | ------ | ---------- | ------ | ----- | ------ |
| Critical | CVE-2021-44228 | org.apache.logging.log4j:log4j-core | 2.14.1 | N/A | ❌ Deferred to task 003 |
| Critical | CVE-2021-45046 | org.apache.logging.log4j:log4j-core | 2.14.1 | N/A | ❌ Deferred to task 003 |
| High | CVE-2021-45105 | org.apache.logging.log4j:log4j-core | 2.14.1 | N/A | ❌ Deferred to task 003 |
| Medium | CVE-2021-44832 | org.apache.logging.log4j:log4j-core | 2.14.1 | N/A | ❌ Deferred to task 003 |
| Medium | CVE-2025-68161 | org.apache.logging.log4j:log4j-core | 2.14.1 | N/A | ❌ Deferred to task 003 |
| Medium | CVE-2026-34477 | org.apache.logging.log4j:log4j-core | 2.14.1 | N/A | ❌ Deferred to task 003 |
| Medium | CVE-2026-34480 | org.apache.logging.log4j:log4j-core | 2.14.1 | N/A | ❌ Deferred to task 003 |
| Critical | CVE-2022-42889 | org.apache.commons:commons-text | 1.9 | N/A | ❌ Deferred to task 003 |

No CVEs were found in `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `hibernate-core:7.4.5.Final`, or `h2:2.4.240` — confirming the Java 25 / Spring Boot 4.1.1 upgrade itself introduced no new vulnerabilities.

</details>

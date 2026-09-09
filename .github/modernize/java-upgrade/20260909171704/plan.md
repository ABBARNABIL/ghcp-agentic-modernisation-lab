# Upgrade Plan: Order Service (20260909171704)

- **Generated**: 2026-09-09T19:22:06+02:00
- **HEAD Branch**: modernize/java-20260909170843
- **HEAD Commit ID**: N/A (delegated branch, see note below)

> Note: This session was delegated by a coordinator (TaskId `001-upgrade-java25-spring-boot4`). The working branch `modernize/java-20260909170843` was already created and checked out by the coordinator before this agent started. This agent does not create/switch branches and commits directly on the current HEAD.

## Available Tools

**JDKs**
- JDK 1.8.0_492: `C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot\bin` (current project JDK, used by step 2 baseline)
- JDK 21.0.10: `C:\Users\nabilabbar\AppData\Local\jdks\jdk-21.0.10\bin` (used by step 3, Spring Boot 3.5.x intermediate)
- JDK 25.0.2: `C:\Users\nabilabbar\AppData\Local\jdks\jdk-25.0.2\bin` (used by step 4 and Final Validation, target JDK — actual JDK 25 distribution, not JDK 26)

**Build Tools**
- Maven 3.9.16: `C:\Program Files\apache-maven-3.9.16\bin` (no Maven wrapper present in the project; using system Maven directly)

## Guidelines

- Scope is limited to `app/Java - Spring Boot/Order Service` backend (Maven project) only. Frontend (`frontend/`) is out of scope for this task.
- Migrate all `javax.persistence`, `javax.validation`, and `javax.validation.Valid` imports to `jakarta.*`.
- Replace the deprecated `WebMvcConfigurerAdapter` with `WebMvcConfigurer`, preserving the existing `/api/**` CORS route coverage and permitted methods (`GET`, `POST`, `PATCH`) exactly as-is. Do NOT broaden the existing permissive CORS origin (`allowedOrigins("*")`).
- Preserve existing REST endpoints, validation semantics, and H2-backed persistence behavior.
- Final committed state MUST be Java 25 with Spring Boot 4.x — intermediate versions may be used as a safe pass-through but must not be the stopping point.
- Validate using an actual installed JDK 25 distribution (not JDK 26).
- Do not modify the frozen baseline folder `src/test/test-cases/` (contains `test-cases.md` and `testdata/`).
- CVE remediation for the intentionally-pinned vulnerable dependencies (`log4j-core:2.14.1`, `commons-text:1.9`) is explicitly out of scope for this task — it is handled by a separate dependent task (`003-remediate-dependency-cves`). These pins must be preserved as-is unless the Spring Boot/Java upgrade itself breaks compilation because of them (not expected).
- Out of scope: Azure resource provisioning, Azure deployment artifacts/IaC, React 18→19 upgrade.
- No user interaction — fully autonomous headless run.

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Options

- Working branch: modernize/java-20260909170843 (provided by coordinator, already checked out)
- Run tests before and after the upgrade: true

## Upgrade Goals

- Java: 8 → 25
- Spring Boot: 2.7.18 → 4.x (target: 4.1.1, latest stable Spring Boot 4.x release)

## Technology Stack

| Technology/Dependency          | Current | Min Compatible | Why Incompatible                                                                 |
| ------------------------------ | ------- | --------------- | --------------------------------------------------------------------------------- |
| Java                            | 8       | 25              | User requested                                                                     |
| Spring Boot (parent)            | 2.7.18  | 4.1.1            | User requested; 4.x requires Java 17+, Jakarta EE 11, Spring Framework 7.x        |
| Spring Boot starter-web ⚠️ deprecated in 4.x | (managed) | spring-boot-starter-webmvc | Renamed/deprecated starter per official Spring Boot 4.0 migration guide           |
| javax.persistence               | (JPA 2.x via SB2) | jakarta.persistence | Namespace removed in Spring Boot 3.x+ (Jakarta EE 9+ baseline)                    |
| javax.validation                | (Bean Validation 2.x via SB2) | jakarta.validation | Namespace removed in Spring Boot 3.x+                                             |
| WebMvcConfigurerAdapter ⚠️ deprecated/removed | extends WebMvcConfigurerAdapter | implements WebMvcConfigurer | Removed in Spring Framework 5+ (class was already deprecated before this baseline) |
| @MockBean / @SpyBean (spring-boot-test)      | org.springframework.boot.test.mock.mockito.MockBean | @MockitoBean (org.springframework.test.context.bean.override.mockito) | Removed in Spring Boot 4.0 (confirmed via official migration guide)               |
| maven-compiler-plugin           | 3.15.0  | 3.15.0 (already sufficient) | Already modern; only `<source>`/`<target>` values need to change                  |
| maven-clean-plugin              | 3.5.0   | 3.5.0 (already sufficient) | No change needed                                                                   |
| H2 (runtime, BOM-managed)       | 2.1.214 (via SB2 BOM) | (managed by SB4 BOM) | Version follows Spring Boot BOM automatically; no explicit pin in pom.xml         |
| log4j-core ⚠️ EOL/vulnerable (intentional) | 2.14.1  | N/A (out of scope) | Intentionally pinned for a separate CVE-remediation task (003); not touched here |
| commons-text ⚠️ vulnerable (intentional)   | 1.9     | N/A (out of scope) | Intentionally pinned for a separate CVE-remediation task (003); not touched here |
| Maven                            | 3.9.16 (system) | 3.9+ | Already satisfies recommended minimum; no wrapper present                        |

## Derived Upgrades

- Spring Boot 4.x → Java 17+ required (target 25 satisfies this); Jakarta EE 11 baseline (Servlet 6.1, Jakarta Persistence 3.2, Jakarta Validation 3.1).
- Spring Boot 4.x → Spring Framework 7.x (brings `WebMvcConfigurer` unchanged package location; `WebMvcConfigurerAdapter` has been removed since Spring Framework 5, so the project must move directly to `WebMvcConfigurer`).
- Spring Boot 4.x → `@MockBean`/`@SpyBean` removed → must migrate `OrderControllerTest` to `@MockitoBean`.
- Spring Boot 4.x → `spring-boot-starter-web` deprecated → rename to `spring-boot-starter-webmvc` (official recommendation in the Spring Boot 4.0 migration guide "Deprecated Starters" table).
- Intermediate hop: Spring Boot 2.7.18 → 3.5.16 (final stable 3.x line) to perform the `javax`→`jakarta` migration and `WebMvcConfigurerAdapter`→`WebMvcConfigurer` change in a lower-risk, well-trodden step before the Spring Boot 4 major jump.
- No Kotlin present in this project → Kotlin version rule does not apply.
- No build-tool wrapper present → no wrapper file to upgrade; system Maven 3.9.16 already satisfies the recommended minimum for Java 25.

## Impact Analysis

### Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
|------|-----------|---------|--------|--------|--------|
| pom.xml | spring-boot-starter-parent | 2.7.18 | upgrade | 3.5.16 | Intermediate hop: final stable Spring Boot 3.x line, Java 17+ baseline |
| pom.xml | java.version / maven.compiler.source / maven.compiler.target (properties) | 8 | upgrade | 21 | Required by Spring Boot 3.5.x (Java 17+); JDK 21 already installed, avoids extra JDK install |
| pom.xml (maven-compiler-plugin config) | source/target | 8 | upgrade | 21 | Match java.version at this hop |
| pom.xml | spring-boot-starter-parent | 3.5.16 | upgrade | 4.1.1 | Final target: latest stable Spring Boot 4.x |
| pom.xml | java.version / maven.compiler.source / maven.compiler.target (properties) | 21 | upgrade | 25 | User requested final Java target |
| pom.xml (maven-compiler-plugin config) | source/target | 21 | upgrade | 25 | Match java.version at final hop |
| pom.xml | spring-boot-starter-web | (managed) | replace | spring-boot-starter-webmvc | Official Spring Boot 4.0 deprecated-starter replacement |
| pom.xml | log4j-core | 2.14.1 | keep as-is | 2.14.1 | Intentionally pinned vulnerable version for separate task 003; not a target-version compatibility blocker |
| pom.xml | commons-text | 1.9 | keep as-is | 1.9 | Intentionally pinned vulnerable version for separate task 003; not a target-version compatibility blocker |

### Source Code Changes

| File | Location | Current | Required Change | Reason |
|------|----------|---------|----------------|--------|
| model/Order.java | imports | `javax.persistence.*` (Column, Entity, EnumType, Enumerated, GeneratedValue, GenerationType, Id, PrePersist, Table) | Replace with `jakarta.persistence.*` equivalents | Jakarta EE 9+ namespace (Spring Boot 3.x baseline) |
| model/Order.java | imports | `javax.validation.constraints.DecimalMin`, `javax.validation.constraints.NotBlank`, `javax.validation.constraints.NotNull` | Replace with `jakarta.validation.constraints.*` equivalents | Jakarta EE 9+ namespace |
| web/OrderController.java | import | `javax.validation.Valid` | Replace with `jakarta.validation.Valid` | Jakarta EE 9+ namespace |
| config/WebConfig.java | class declaration | `public class WebConfig extends WebMvcConfigurerAdapter` | Change to `public class WebConfig implements WebMvcConfigurer` | `WebMvcConfigurerAdapter` removed; `WebMvcConfigurer` is a default-method interface, drop-in replacement — `addCorsMappings` override kept identical (mapping `/api/**`, `allowedOrigins("*")`, `allowedMethods("GET","POST","PATCH")`) |
| test/web/OrderControllerTest.java | import + annotation | `org.springframework.boot.test.mock.mockito.MockBean` / `@MockBean` | Replace with `org.springframework.test.context.bean.override.mockito.MockitoBean` / `@MockitoBean` | `@MockBean`/`@SpyBean` removed in Spring Boot 4.0 (confirmed via official migration guide) |

### Configuration Changes

None required. `application.properties` keys (`spring.datasource.*`, `spring.jpa.*`, `spring.h2.console.*`, `server.address`, `server.port`) are unaffected by the Spring Boot 3.x/4.x property-rename lists reviewed (MongoDB/persistence-exception-translation renames do not apply to this project).

### CI/CD Changes

None. No Dockerfile, GitHub Actions workflow, or other CI/CD pipeline files exist in this repository that reference a JDK/Java version.

### Documentation Changes

| File | Location | Current | Required Change |
|------|----------|---------|----------------|
| README.md | Tech Stack table | `Language: Java 8`, `Framework: Spring Boot 2.7.18 (Web, Data JPA, Validation)` | Update to `Language: Java 25`, `Framework: Spring Boot 4.1.1 (Web MVC, Data JPA, Validation)` |
| README.md | Prerequisites | "JDK 17+ and Maven 3.6+ (the build currently targets Java 8 for the initial modernization baseline)" | Update to reflect Java 25 target (drop the Java-8-baseline caveat) |

### Risks & Warnings

- **Test-module modularization in Spring Boot 4**: The migration guide states each main starter now has a dedicated test-starter companion (e.g. `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`) and that `spring-boot-starter-test` is brought in transitively by those. `@WebMvcTest` and `@DataJpaTest` (used by `OrderControllerTest` and `OrderRepositoryTest`) may have moved to these dedicated test modules. **Mitigation**: keep the existing explicit `spring-boot-starter-test` dependency first; if compilation/test-compilation fails to resolve `@WebMvcTest`/`@DataJpaTest`, add `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test` (test scope) — verify with an actual build rather than assuming.
- **Jackson 3 default in Spring Boot 4**: Spring Boot 4 prefers Jackson 3 (`tools.jackson` packages) by default. This project does not directly import any `com.fasterxml.jackson.*` classes, so no source change is anticipated, but JSON (de)serialization behavior for `BigDecimal`/`Date` fields should be re-verified by the existing MockMvc `jsonPath` assertions in `OrderControllerTest` during Final Validation.
- **CVE-pinned dependencies retained**: `log4j-core:2.14.1` and `commons-text:1.9` are intentionally vulnerable pins reserved for a separate task (`003-remediate-dependency-cves`). They are explicit `<version>` overrides that are OLDER than what Spring Boot 4's BOM would manage — per the CVE version-pin-protection rule these look like they could be either a pin or an intentional demo artifact; the pom.xml comment confirms it is an intentional demo pin, not a security patch, so it is safe to leave untouched for this task.
- **Intermediate JDK 21 for the 3.5.16 hop**: Spring Boot 3.5.x supports Java 17–24; JDK 17 is not installed on this system but JDK 21 is, and is within the supported range, so it is used instead of installing an additional JDK (minimizes environment changes while remaining fully compatible).

## Upgrade Steps

- Step 1: Setup Environment
  - **Rationale**: Mandatory first step; confirm all required JDKs/build tools are available before making any code changes.
  - **Changes to Make**: None (no files changed). Verify JDK 1.8.0_492 (baseline), JDK 21.0.10 (intermediate), JDK 25.0.2 (target), and Maven 3.9.16 are all present via `#appmod-list-jdks` / `#appmod-list-mavens`. No installation needed — all required tools already exist on the system.
  - **Verification**: `#appmod-list-jdks` / `#appmod-list-mavens` — Expected: all 3 JDKs (8, 21, 25) and Maven 3.9.16 listed.

- Step 2: Setup Baseline
  - **Rationale**: Mandatory; capture pre-upgrade compile/test results with the current JDK 8 to form the acceptance baseline (documented baseline: 20/20 backend tests passing).
  - **Changes to Make**: None (no files changed).
  - **Verification**: Command: `mvn clean compile test-compile -q && mvn clean test -q`, JDK: `C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot`, Expected: BUILD SUCCESS, 20/20 tests passing (matches assessment baseline).

- Step 3: Upgrade to Spring Boot 3.5.16 / Java 21 — Jakarta migration + WebMvcConfigurer
  - **Rationale**: Lower-risk intermediate hop that completes the mandatory `javax`→`jakarta` namespace migration and the `WebMvcConfigurerAdapter`→`WebMvcConfigurer` replacement on a well-established Spring Boot 3.x line, before attempting the larger Spring Boot 4 major-version jump.
  - **Changes to Make**: Apply the Dependency Changes rows for `spring-boot-starter-parent`→3.5.16 and `java.version`/compiler plugin→21; apply all Source Code Changes rows for `javax.*`→`jakarta.*` (Order.java, OrderController.java) and `WebConfig.java` (`WebMvcConfigurerAdapter`→`WebMvcConfigurer`).
  - **Verification**: Command: `mvn clean test-compile -q`, JDK: `C:\Users\nabilabbar\AppData\Local\jdks\jdk-21.0.10`, Expected: BUILD SUCCESS (compile only; full test run deferred to next step/Final Validation).

- Step 4: Upgrade to Spring Boot 4.1.1 / Java 25 — final target
  - **Rationale**: Completes the user-requested final target state (Java 25 + Spring Boot 4.x). Applies the starter rename and the mandatory `@MockBean`→`@MockitoBean` test migration required by Spring Boot 4.0's removal of `@MockBean`/`@SpyBean`.
  - **Changes to Make**: Apply Dependency Changes rows for `spring-boot-starter-parent`→4.1.1, `java.version`/compiler plugin→25, `spring-boot-starter-web`→`spring-boot-starter-webmvc`; apply Source Code Changes row for `OrderControllerTest.java` (`@MockBean`→`@MockitoBean`). Address the "Test-module modularization" risk if compilation fails (add `spring-boot-starter-webmvc-test`/`spring-boot-starter-data-jpa-test` only if needed).
  - **Verification**: Command: `mvn clean test-compile -q`, JDK: `C:\Users\nabilabbar\AppData\Local\jdks\jdk-25.0.2`, Expected: BUILD SUCCESS (compile only; full test run in Final Validation).

- Step 5: Final Validation
  - **Rationale**: Mandatory final step — verify all Upgrade Success Criteria are met on the actual JDK 25 distribution, resolve all deferred/TODO items, and confirm 100% test pass rate (≥ baseline 20/20).
  - **Changes to Make**: Apply Documentation Changes (README.md Tech Stack + Prerequisites). Resolve any remaining TODOs/workarounds from Steps 3–4. Run a CVE scan (`#appmod-validate-cves-for-java`) restricted to the direct dependencies actually touched/managed by this upgrade (Spring Boot BOM-managed deps) to confirm the framework upgrade itself introduces no new CVEs; explicitly document that `log4j-core`/`commons-text` pins remain out of scope (task 003).
  - **Verification**: Command: `mvn clean package -q` then `mvn clean test -q`, JDK: `C:\Users\nabilabbar\AppData\Local\jdks\jdk-25.0.2`, Expected: BUILD SUCCESS, 20/20 tests passing (100%, ≥ baseline), packaged JAR produced.

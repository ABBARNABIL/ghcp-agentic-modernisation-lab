# Modernization Plan: Order Service Java 25 and Spring Boot 4

**Project**: Order Service

## Technical Framework

- **Language**: Java 8 to Java 25
- **Framework**: Spring Boot 2.7.18 to Spring Boot 4.x
- **Build Tool**: Maven
- **Database**: H2 2.1.214
- **Key Dependencies**: Spring Web, Spring Data JPA, Spring Validation, Log4j Core, Commons Text

## Overview

This modernization moves the Order Service backend from its unsupported Java 8 and
Spring Boot 2.7.18 baseline to the validated Java 25 / Spring Boot 4.x target. It
will preserve the existing order API and H2-backed behavior while adopting the
required Jakarta APIs, retiring the deprecated MVC configuration adapter, and
remediating confirmed dependency vulnerabilities.

The work is limited to the backend and its frontend build-tooling security
dependencies. Azure infrastructure provisioning and deployment are not included
because neither is defined in the assessment or requested scope. The React 18 to
19 upgrade remains explicitly deferred as a separate, independently versioned
change.

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| Order Service | Java 8 / Boot 2.7 | No deployment | N/A | Code-only modernization |

## Execution Approach

The executable task graph begins with the framework and runtime upgrade, which
includes the `javax.*` to `jakarta.*` migration and deprecated MVC configuration
replacement. A frozen behavior baseline is captured independently. Security
remediation follows the upgrade, then mocked integration verification confirms the
upgraded backend and frontend build workflow without requiring Azure resources.

Task scope, dependencies, risks, and acceptance criteria are authoritative in
`tasks.json`.

## Target-State Guardrails

- Target Java 25 with Spring Boot 4.x, which supplies the compatible Spring
  Framework 7.x and Jakarta API baseline.
- Preserve the existing REST endpoints and order persistence behavior during the
  Hibernate and Jakarta transition.
- Revalidate using a real JDK 25 distribution; the assessment used JDK 26 only as
  a forward-compatibility proxy.
- Treat the Java CVE findings as version-correlated until an authenticated Java
  dependency scanner is available; retain the confirmed `npm audit` advisories.
- Do not provision Azure resources, create deployment assets, or upgrade React to
  19 within this plan.

## Open Questions & Questionnaire

- [x] Q: Which Spring Boot target should be used? → A: Spring Boot 4.x and Java 25.
- [x] Q: Should the plan include environment or infrastructure provisioning? → A:
  No; scope is code migration only and no infrastructure is defined.
- [x] Q: Should integration testing be included? → A: Yes; use mocked dependencies
  because no Azure infrastructure is available.
- [x] Q: Should security and CVE remediation be included? → A: Yes; required by
  the assessment and the modernization plan policy.
- [x] Q: Which Azure deployment target should be used? → A: No deployment;
  migration only.

# Order Service — Java 25 & Spring Boot Modernization Assessment

**Scope:** Full repository, primary focus `app/Java - Spring Boot/Order Service`.
**Date:** 2026-09-09. **Method:** Read-only inspection + executed build/test/dependency/security commands (no source or dependency-version changes made). Final `git status --short` contains only the new, untracked `.github/modernize/assessment/` artifacts; generated build outputs are ignored.

## 1. Current State (evidence-based)

| Item | Value | Evidence |
|---|---|---|
| Language level | Java 8 | `pom.xml` lines 20-22 (`<java.version>8</java.version>`, `maven.compiler.source/target=8`) |
| Framework | Spring Boot 2.7.18 (parent) | `pom.xml` line 10 |
| Build tool | Maven | `pom.xml`; installed Maven 3.9.16 (`mvn -v`) |
| Compiler plugin | `maven-compiler-plugin` 3.15.0, `maven-clean-plugin` 3.5.0 | `pom.xml` lines 74-86 |
| DB | H2 2.1.214 (resolved) | `mvn dependency:tree` output |
| Explicitly vulnerable pin | `log4j-core` 2.14.1, with author comment "Intentionally pinned to a vulnerable version for CVE remediation exercises. Do not use in production." | `pom.xml` lines 46, 50 |
| Other pinned dep | `commons-text` 1.9 | `pom.xml` line 56 |
| Frontend | React 18.3.1, Vite ^5.4.11 (resolves to 5.4.21) | `frontend/package.json` lines 13-18; `frontend/package-lock.json` lines 1651-1652 |
| Repo-wide scope check | Only one application exists in the repo: `app/Java - Spring Boot/Order Service` (backend + `frontend/`). No other app stacks, no `.github/workflows` CI, no pre-existing `.github/modernize` directory. | `Get-ChildItem app`, `Get-ChildItem .github` (both prior to this assessment) |

## 2. Environment / Tooling Used

- `java -version` (default/PATH): OpenJDK 1.8.0_492 (Temurin) — this is what `mvn` uses by default.
- No JDK 25 was installed in this environment. The only alternate JDK found was **JDK 26.0.2** (Oracle) at `C:\Program Files\Java\jdk-26.0.2`. This was used via `JAVA_HOME`/`PATH` override as the nearest available forward-compatibility proxy for "Java 25 readiness" testing. **Results below should be re-validated against an actual JDK 25 distribution.**
- Maven 3.9.16, Node v24.16.0, npm 11.13.0.
- Network reachability confirmed: `repo.maven.apache.org:443` ✅, `ossindex.sonatype.org:443` ✅, `services.nvd.nist.gov:443` ✅, `registry.npmjs.org:443` ✅ (all TCP-reachable; see §5 for what this did/did not enable).

## 3. Commands Executed and Exact Outcomes

All commands run from `app/Java - Spring Boot/Order Service` unless noted. **No pom.xml, package.json, or package-lock.json was modified.**

| Command | JDK used | Outcome |
|---|---|---|
| `mvn clean test` | 1.8.0_492 (declared baseline) | **BUILD SUCCESS** — `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`; completed in 22.858 s |
| `mvn package -DskipTests` | 1.8.0_492 (declared baseline) | **BUILD SUCCESS** — produced and Spring Boot-repackaged `target/order-service-1.0.0.jar` |
| `mvn -v` | 26.0.2 | Maven 3.9.16 confirmed |
| `mvn clean compile` | 26.0.2 (pom unmodified: source/target=8) | **BUILD SUCCESS**. Warnings emitted (see §4.1) |
| `mvn test` | 26.0.2 (bytecode compiled at level 8, executed on JVM 26) | **BUILD SUCCESS** — `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0` (repository, service, unit, and web-controller test classes all passed) |
| `mvn dependency:tree` | 26.0.2 | **BUILD SUCCESS** — full resolved tree captured (Spring Boot 2.7.18 BOM, Hibernate 5.6.15.Final, Tomcat embed 9.0.83, Spring Framework 5.3.31, JUnit 5.8.2, Mockito 4.5.1, etc.) |
| `mvn org.codehaus.mojo:versions-maven-plugin:2.18.0:display-dependency-updates` (ad hoc, not added to pom) | 26.0.2 | **BUILD SUCCESS** — reported newer versions available (see §6) |
| `mvn org.codehaus.mojo:versions-maven-plugin:2.18.0:display-plugin-updates` (ad hoc) | 26.0.2 | **BUILD SUCCESS** — project's own plugin versions (3.15.0 / 3.5.0) already exceed every version suggested; **no plugin-version blocker** |
| `mvn org.sonatype.ossindex.maven:ossindex-maven-plugin:3.2.0:audit` (ad hoc, not added to pom) | 26.0.2 | Network reached `ossindex.sonatype.org`, but failed: **`HTTP 401 Unauthorized`** — see §5 (tooling limitation, not a "no vulnerabilities" result) |
| `npm ci` (frontend) | n/a | **Success** — "added 62 packages, audited 63 packages", 0 install errors |
| `npm test` (frontend, `node --test`) | n/a | **Success** — 4/4 tests pass |
| `npm run build` (frontend, `vite build`) | n/a | **Success** — produced `dist/` (gitignored) |
| `npm audit` (frontend) | n/a | **Completed successfully** — 6 vulnerable packages (2 moderate, 4 high) reported with concrete GHSA IDs (see §5.1) |
| `npm outdated` (frontend) | n/a | Completed — react/react-dom 18.3.1 → 19.2.8 available |

## 4. Java 25 Migration Findings

### 4.1 Confirmed via executed build (evidence, not speculation)
Compiling with JDK 26 against the **unmodified** pom (`source`/`target` = 8) produced:
```
[WARNING] bootstrap class path is not set in conjunction with -source 8
[WARNING] source value 8 is obsolete and will be removed in a future release
[WARNING] target value 8 is obsolete and will be removed in a future release
[INFO] .../config/WebConfig.java uses or overrides a deprecated API.
```
- The compiler does not state *which* future release will drop level-8 support; this is a compiler-emitted warning, not a hard failure today.
- **Positive finding:** `mvn test` (20/20 tests) executed successfully **at runtime on JDK 26** with the app still built at Java-8 bytecode level and Spring Boot 2.7.18 — i.e., no runtime incompatibility was observed for this specific, small application's test surface. This does not guarantee full production-scale compatibility (Spring Boot 2.7.x's officially documented support ceiling is Java 19), but it is empirical evidence, not merely a claim.

### 4.2 Namespace blocker (javax → jakarta)
- `src/main/java/.../model/Order.java` lines 3-14: 12 `javax.persistence.*` / `javax.validation.constraints.*` imports.
- `src/main/java/.../web/OrderController.java` line 17: 1 `javax.validation.Valid` import.
- These must migrate to `jakarta.*` as part of the Spring Boot 3 upgrade (Spring Boot 2.7.x still ships javax-based Jakarta EE 8 artifacts, confirmed by `jakarta.persistence:jakarta.persistence-api:jar:2.2.3` and `jakarta.validation:jakarta.validation-api:jar:2.0.2` in the resolved dependency tree — these coordinates carry the `javax.*` package despite the `jakarta` groupId).

### 4.3 No other Java-language blockers found
- Grep across all `src/main` and `src/test` `.java` files found no use of removed JDK APIs (no `SecurityManager`, `sun.*`, `javax.xml.bind`, `Applet`, finalizers, etc.) — the codebase is small (8 main classes) and does not exercise legacy/removed API surface.

### 4.4 Build-plugin versions are NOT a blocker
- `maven-compiler-plugin` 3.15.0 and `maven-clean-plugin` 3.5.0 (pom.xml lines 74-86) already exceed the versions required for Java 21/25 builds; `versions:display-plugin-updates` found no newer applicable version for this project.

## 5. Security / Dependency Findings — CONFIRMED vs. VERSION-AGE / SCANNER-LIMITED

### 5.1 CONFIRMED (completed automated scan with concrete advisory IDs)
`npm audit` completed successfully against `frontend/package-lock.json` and reported 6 vulnerable packages (2 moderate, 4 high):

| Package | Severity | Advisory | Note |
|---|---|---|---|
| `baseline-browser-mapping` <2.11.0 | Moderate | GHSA-w5vr-8v7q-w6rv | Process-termination DoS on invalid input |
| `browserslist` ≤4.28.6 | High | GHSA-c83g-rgw3-j3cx, GHSA-73wf-gq98-2v4g | Unbounded memory growth / crash via untrusted stats file |
| `esbuild` ≤0.24.2 | Moderate | GHSA-67mh-4wv8-2f99 | Dev server accepts arbitrary requests |
| `nanoid` ≤3.3.17 | High | GHSA-28wg-ghj8-5hjv, GHSA-2v37-7h3g-55p8 | Infinite loop DoS |
| `postcss` ≤8.5.22 | High | GHSA-fxqj-rqcc-2cmp, GHSA-r28c-9q8g-f849 | sourceMappingURL path traversal / arbitrary file disclosure |
| `vite` ≤6.4.2 | High | GHSA-4w7w-66w2-5vf9, GHSA-v6wh-96g9-6wx3, GHSA-fx2h-pf6j-xcff (plus the transitive `esbuild` advisory above) | Development-server access-control, file-serving, and path-traversal issues |

`vite` is a **direct devDependency** (`frontend/package.json` lines 16-18); the other five packages are transitive in its build-tooling graph. They are not runtime dependencies shipped in the production `dist/` bundle, but the Vite/esbuild findings can affect use of the development server. `npm audit` reported ordinary fixes for five packages and a semver-major move to `vite@8.2.2` for the Vite/esbuild path; no fix command was run.

### 5.2 Version-identified against public CVE records (NOT independently confirmed by a live scanner here — see §5.3)
- **`log4j-core` 2.14.1** (`pom.xml` line 50, explicitly commented at line 46 as intentionally vulnerable): this exact version is within the publicly documented vulnerable range for **CVE-2021-44228 (Log4Shell)**, CVE-2021-45046, and CVE-2021-45105. This is a version-number correlation with public NVD records and the pom's own comment — no live CVE-database scan of this artifact completed in this session (see limitation below).
- **`commons-text` 1.9** (`pom.xml` line 56): within the publicly documented vulnerable range (1.5–1.9) for **CVE-2022-42889 (Text4Shell)**. Same caveat as above — version correlation, not live-scan-confirmed.
- **`h2database:h2` 2.1.214** (`pom.xml` lines 40-43; version resolved from the Spring Boot BOM): flagged only as a **version-age observation** (newer 2.5.250 available per §6) — no specific CVE was verified to apply to exactly 2.1.214 with confidence in this session; do not treat as confirmed-vulnerable.

### 5.3 Scanner limitations (explicit)
- `ossindex-maven-plugin:audit` reached `ossindex.sonatype.org` over the network but returned **HTTP 401 Unauthorized** — Sonatype OSS Index now requires an authenticated account/API token even for basic lookups; none was configured in this environment. **Java-side CVE confirmation via live scan was not achieved.**
- No OWASP `dependency-check-maven` plugin is configured anywhere in the repository (`pom.xml` contains no such plugin). It was not invoked ad hoc because a first-time NVD data-feed download typically requires an NVD API key for acceptable performance (2024 NVD policy change) and none was available; running it unauthenticated risked a very long/rate-limited hang with no completion guarantee.
- `npm audit`, `npm ci`, `npm test`, `npm run build`, `mvn dependency:tree`, and the `versions-maven-plugin` goals all completed successfully with no connectivity issues.

## 6. Dependency Update Data (from `versions-maven-plugin`, ad hoc, read-only)

Direct/explicit dependencies with newer versions available on Maven Central:
- `com.h2database:h2` 2.1.214 → 2.5.250
- `org.apache.logging.log4j:log4j-core` 2.14.1 → 2.17.2 (latest stable 2.x) / 3.0.0-beta3 (next major, pre-release)
- `org.apache.commons:commons-text` 1.9 → 1.15.0

Frontend (`npm outdated`):
- `react` 18.3.1 → 19.2.8
- `react-dom` 18.3.1 → 19.2.8

## 7. Spring Boot Upgrade Considerations

1. Spring Boot 2.7.18 (`pom.xml` line 10) reached end of OSS support in Nov 2023 — version-age fact, not a live-scan finding.
2. `src/main/java/.../config/WebConfig.java` line 13: `public class WebConfig extends WebMvcConfigurerAdapter` — a deliberately-planted deprecated API (see the class's own Javadoc comment at lines 8-10: "Uses the deprecated WebMvcConfigurerAdapter on purpose... (WebMvcConfigurerAdapter -> WebMvcConfigurer in Spring 5+)"). Confirmed by the actual `mvn compile` deprecation warning in §4.1.
3. §4.2 javax→jakarta namespace migration is a hard prerequisite for Spring Boot 3.
4. `WebConfig.java` line 18: `.allowedOrigins("*")` for `/api/**` — a permissive CORS configuration; not a Java/Boot-version blocker, but worth flagging during the upgrade pass since it is easy to tighten alongside `WebMvcConfigurer` migration.

## 8. Artifacts Generated / Modified by This Assessment

**Assessment-convention artifacts (written by this session, per `docs/workshop.md`):**
- `.github/modernize/assessment/assessment.md` — this file (new)
- `.github/modernize/assessment/tasks.json` — structured findings/tasks (new)

**Build-tool-generated artifacts (produced by the executed commands; all already excluded by existing `.gitignore` rules, none committed):**
- `app/Java - Spring Boot/Order Service/target/` (from `mvn compile`/`mvn test`)
- `app/Java - Spring Boot/Order Service/frontend/node_modules/` (from `npm ci`)
- `app/Java - Spring Boot/Order Service/frontend/dist/` (from `npm run build`)

**Confirmed NOT modified:** `pom.xml`, `frontend/package.json`, `frontend/package-lock.json`, and all `src/**` application source files. Final `git status --short` reports only `?? .github/`; `git status --short --ignored` additionally reports the three generated build directories above as ignored.

## 9. Prioritized Next Steps

1. **Security (independent of Java version):** upgrade `log4j-core` off 2.14.1 and `commons-text` off 1.9 to patched lines; re-run `npm audit fix` for the frontend dev-tooling advisories.
2. **Jakarta namespace migration:** `javax.*` → `jakarta.*` in `Order.java` and `OrderController.java` — required before/with the Spring Boot 3 upgrade.
3. **Deprecated API cleanup:** replace `WebMvcConfigurerAdapter` with `WebMvcConfigurer` in `WebConfig.java`.
4. **Spring Boot upgrade:** 2.7.18 → 3.x, validating Hibernate 5→6 and the jakarta-persistence provider swap.
5. **Java level increase:** raise `java.version`/`maven.compiler.source`/`target` from 8 → 17 → 21 → 25 incrementally, re-running the full 20-test backend suite (and 4-test frontend suite) at each step.
6. **Re-validate on a real JDK 25 install** — this assessment substituted JDK 26.0.2 because no JDK 25 was present in the environment.
7. **Obtain OSS Index credentials or an NVD API key** to enable a fully live-confirmed Java CVE scan (current environment has network reachability but no authentication configured).
8. Consider the React 18 → 19 upgrade separately from the Java/Boot modernization track (frontend is independently versioned and already builds/tests cleanly).

## 10. Tooling / Environment Limitations (summary)

- No JDK 25 installed (only JDK 8 default + JDK 26.0.2 alternate) — Java 25 findings are inferred from JDK 26 behavior, not confirmed on JDK 25 itself.
- OSS Index audit plugin: HTTP 401 Unauthorized (no credentials) — Java dependency CVE confirmation is version-correlation-based, not live-scan-confirmed.
- No OWASP dependency-check configured in the repo and not invoked ad hoc (NVD API key requirement / rate-limit risk).
- No `.github/workflows` CI exists in the repository to cross-check against; commands used were taken from `README.md`'s documented developer workflow.
- Frontend tooling (`npm ci`/`test`/`build`/`audit`/`outdated`) had no limitations — all completed successfully.

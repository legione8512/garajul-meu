# PROJECT_STATE

Portable state of the guided build. Updated at every meaningful milestone so the
project can be continued in a new conversation, or by a different AI assistant,
without relying on model memory.

Last updated: 2026-08-12

---

## Current position

| Item | Value |
|---|---|
| Phase | 3 — Common Backend Infrastructure — **in progress** |
| Last milestone | 3.1 test infrastructure: Testcontainers PostgreSQL wired, Mockito attached as an explicit JVM agent, both backend tests green |
| Next verified step | 3.2 — canonical error-code catalog and global exception handler, per specification section 17 |

## Project paths

| What | Path |
|---|---|
| Repository root | `D:\Learning\Claude_Space\Garajul Meu` |
| GitHub remote | https://github.com/legione8512/garajul-meu — **public** |
| Backend (Eclipse) | `backend/` — Maven project, imported via m2e |
| Frontend (VS Code) | `frontend/` |
| Eclipse workspace | outside the repository |
| Specification source | `C:\Drive\UNI\Brunel University London\Personal Projects\Garajul Meu\` |

## Installed tool versions (verified 2026-08-12)

| Tool | Version |
|---|---|
| OS | Windows 11 Pro 10.0.26100 |
| JDK | Eclipse Adoptium 21.0.10 LTS (`JAVA_HOME` set) |
| Maven | 3.9.16 global; wrapper pins the same version, `distributionType=only-script` |
| Node.js | 24.19.0 (Active LTS — upgraded from 22.19.0) |
| npm | 11.12.1 |
| Git | 2.50.0.windows.1, `init.defaultBranch=main` |
| Eclipse | Platform 4.40.0 (2026-06 R), m2e 2.7.800, Spring Tools boot-ls 2.1.1 |
| VS Code | `D:\Microsoft VS Code` |
| Docker Desktop | 29.3.1 installed; daemon not started yet |

A JDK 17.0.18.8 is also present but unused. Eclipse's default project JRE is
explicitly Adoptium 21.

## Environments

### Neon PostgreSQL — provisioned 2026-08-12

| Item | Value |
|---|---|
| Project | `garajul-meu` |
| Region | AWS Europe Central 1 (Frankfurt), per specification section 23 |
| Server version | PostgreSQL 18.4 |
| Database | `neondb` |
| Branch `production` | default branch, created by Neon, **unused so far** — reserved for Phase 15–16 |
| Branch `development` | child of `production`, used by local development |
| Plan | Free: 0.5 GB storage, scales to zero when idle, 10 branches |

Credentials live only in `backend/application-local.yml`, which is gitignored and
never committed. Nothing sensitive is stored in the repository.

Railway and Cloudflare Pages are not provisioned yet. Automated tests will use
PostgreSQL Testcontainers, never the Neon development branch.

## Flyway migrations

Flyway is wired and runs at startup over the direct (non-pooled) endpoint. It has
created `public.flyway_schema_history` on the `development` branch.

**No migration files exist yet** — `spring.flyway.locations` resolves to the
default `classpath:db/migration`, which does not exist, so startup logs
`No migrations found`. This is expected. The first migration is the `users`
table in Phase 4.

## Implemented modules and endpoints

### backend — Spring Boot 4.1.0, Java 21, `ro.garajulmeu`

| Item | State |
|---|---|
| `BackendApplication` | boots on Tomcat 11.0.22, port 8080 |
| `GET /actuator/health` | `UP`, including `db: UP` (PostgreSQL); liveness and readiness groups exposed |
| Runtime dependencies | `spring-boot-starter-webmvc`, `-actuator`, `-data-jpa`, `-flyway`, `org.flywaydb:flyway-database-postgresql`, `org.postgresql:postgresql` (runtime), `spring-boot-devtools` (runtime, optional) |
| Test dependencies | `spring-boot-starter-webmvc-test`, `spring-boot-starter-actuator-test` |
| ORM | Hibernate ORM 7.4.1.Final (Spring Boot 4 baseline) |
| Configuration | `src/main/resources/application.yml`, committed, placeholders only |
| Local secrets | `backend/application-local.yml`, gitignored, loaded via `spring.config.import: optional:file:./application-local.yml` |
| Artifact | `target/backend-0.0.1-SNAPSHOT.jar`, repackaged as an executable jar |

No `/api/v1` endpoints and no JPA entities or repositories exist yet.

### frontend — React 19.2.8, TypeScript ~6.0.2, Vite 8.2.0

| Item | State |
|---|---|
| Template | `react-ts` from create-vite 9.1.2 |
| Linter | ESLint 10.8.0 with `typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh` |
| `npm run dev` | serves on port 5173 |
| `npm run build` | `tsc -b && vite build`, output in `frontend/dist/` |

Default Vite starter page only. No routing, i18n or design system yet — those
belong to Phase 5.

## Tests currently passing

| Test | Scope |
|---|---|
| `BackendApplicationTests.contextLoads` | Spring context boots against a throwaway PostgreSQL container |
| `BackendApplicationTests.usesThrowawayContainerAndNeverTheHostedDatabase` | Asserts the live JDBC URL contains `localhost` and not `neon.tech` |

The second test guards specification section 20. Configuration changes are
frequent and a Neon URL leaking into the test context would be invisible — tests
would pass while writing to the real database. This makes that mistake loud.

Test infrastructure: `TestcontainersConfiguration` supplies a
`PostgreSQLContainer` annotated `@ServiceConnection`, pinned to
`postgres:18.4-alpine` to match the Neon server version. `src/test/resources/application.yml`
deliberately shadows the main configuration file so `application-local.yml` can
never reach a test.

No frontend tests yet. Vitest and Playwright arrive with Phases 5 and 14.

## External integrations configured

None yet. Just-in-time schedule: Neon at Phase 2, Resend at Phase 4,
Google Document AI at Phase 9, Firebase at Phase 11, Cloudflare R2 at Phase 12,
Sentry at Phase 15.

## Decisions taken during the build

| Date | Decision |
|---|---|
| 2026-08-12 | IDE split: backend in Eclipse, frontend in VS Code. `pom.xml` and `package.json` remain the source of truth for project structure, so IDE metadata is gitignored and regenerable. |
| 2026-08-12 | Line endings handled by a committed root `.gitattributes` (`* text=auto eol=lf`, `*.cmd`/`*.bat`/`*.ps1` CRLF) rather than per-machine `core.autocrlf`, so Linux CI and the macOS iOS build behave identically. |
| 2026-08-12 | Default branch `main`. GitHub repository is **public**; secret hygiene is therefore release-critical — an exposed secret must be rotated, not merely deleted from a later commit. |
| 2026-08-12 | Single root `.gitignore` for the whole monorepo. The `.gitignore` and `.gitattributes` files generated by Spring Initializr and by Vite were deleted; their genuinely new rules (`*.local`, `logs/`) were merged into the root file. |
| 2026-08-12 | Base package `ro.garajulmeu`, artifact `backend`. Package name field on Initializr was overridden so packages are `ro.garajulmeu.<feature>`, matching specification section 4. |
| 2026-08-12 | Spring Boot 4.1.0 — the only stable line Initializr still offers. Note that Boot 4 renamed starters: `spring-boot-starter-webmvc` (not `-web`) and per-module test starters (no `spring-boot-starter-test`). Spring Boot 3.x tutorials will give dependency names that no longer exist. |
| 2026-08-12 | Frontend uses ESLint rather than the create-vite 9 default of Oxlint, because `eslint-plugin-jsx-a11y` is needed for the accessibility requirement in specification section 36. |
| 2026-08-12 | Template `react-ts` rather than `react-compiler-ts`. React Compiler is a reversible one-line addition later if performance requires it. |
| 2026-08-12 | Node upgraded 22.19.0 → 24.19.0 because Node 22 is Maintenance LTS and 24 is Active LTS; the version has to be declared for CI and Cloudflare Pages anyway. |
| 2026-08-12 | Neon Auth left disabled at project creation. It would store its own users and sessions and displace the authentication design in specification section 14 and the `users` / `refresh_tokens` / `verification_tokens` tables in section 10. It can still be enabled later from project settings if that ever changes. |
| 2026-08-12 | Two datasources: application runtime uses the Neon **pooled** endpoint, Flyway uses the **direct** endpoint via `spring.flyway.url`. The pooler runs in transaction mode and cannot hold the session-level lock Flyway relies on. Verified in the startup log — the two hostnames differ by the `-pooler` suffix. |
| 2026-08-12 | Configuration moved from `application.properties` to `application.yml`, since the datasource, Hikari, JPA and Flyway trees are deeply nested. |
| 2026-08-12 | `spring.jpa.hibernate.ddl-auto: validate` and `spring.jpa.open-in-view: false`. Flyway is the only thing allowed to change the schema; Hibernate may only verify mappings. |
| 2026-08-12 | Local secrets pattern: committed `application.yml` contains only `${PLACEHOLDER}` references; real values come from gitignored `backend/application-local.yml` locally, and from real environment variables in production. An undefined placeholder fails startup loudly rather than failing at connection time. |
| 2026-08-12 | Spring Boot 4 uses a `spring-boot-starter-flyway` starter rather than depending on `org.flywaydb:flyway-core` directly, as Boot 3.x did. |
| 2026-08-12 | JDBC URLs use `channelBinding` (camelCase). Neon's native connection string uses the libpq spelling `channel_binding`, which the PostgreSQL JDBC driver rejects. |
| 2026-08-13 | Testcontainers 2.0.5: container classes are **no longer generic**. `new PostgreSQLContainer<>(...)` from every 1.x example fails to compile; the canonical class is now `org.testcontainers.postgresql.PostgreSQLContainer`, with a legacy copy left in `org.testcontainers.containers`. Artifact ids also changed to `testcontainers-junit-jupiter` and `testcontainers-postgresql`. |
| 2026-08-13 | The Mockito agent path resolves only because `maven-dependency-plugin`'s `properties` goal runs first; it publishes each dependency's jar path as a property named `groupId:artifactId:type`. Without that plugin, `${org.mockito:mockito-core:jar}` reaches the JVM as literal text and the forked test VM fails to start. |
| 2026-08-13 | Test isolation is enforced by a real assertion, not only by configuration: `usesThrowawayContainerAndNeverTheHostedDatabase` fails if the live JDBC URL ever points at Neon. |

## Known issues and open decisions

### Deferred work with a scheduled phase

| Item | Phase |
|---|---|
| Surefire now sets `argLine` for the Mockito agent. JaCoCo also writes `argLine`, so when coverage is added the value must become `@{argLine} -javaagent:...` or one plugin will silently overwrite the other. | 14 |
| `eslint-plugin-jsx-a11y` not installed; required for the accessibility rules in specification section 36. | 5 |
| Node version not yet pinned in the repository. Add `.nvmrc` and `engines` so GitHub Actions and Cloudflare Pages resolve the same version. | 14 |

### Open decisions

- Mac availability for Phase 18 (iOS) not yet confirmed. Blocks nothing before Phase 17.
- Google Document AI processor version must be verified as currently supported at Phase 9; deliberately not frozen in advance.
- Spring Security Argon2 encoder implementation to be verified against current documentation at Phase 4. Argon2 itself is the frozen algorithm. Spring Boot 4 implies Spring Security 7, whose API differs from the 6.x examples found in most tutorials.

### Carried over from specification section 35

- Final product name, logo, colours and production domain.
- Normalised X/Y/width/height coordinates for certificate overlay fields — calibrated in Phase 8, never guessed.
- Final OCR confidence thresholds — calibrated against representative samples.
- Capacitor secure-storage plugin — selected during the mobile phase against current documentation.
- Final legal wording, retention periods and privacy notices before release.

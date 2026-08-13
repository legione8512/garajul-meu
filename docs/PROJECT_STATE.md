# PROJECT_STATE

Portable state of the guided build. Updated at every meaningful milestone so the
project can be continued in a new conversation, or by a different AI assistant,
without relying on model memory.

Last updated: 2026-08-12

---

## Current position

| Item | Value |
|---|---|
| Phase | 4 — Authentication & Users — **in progress** |
| Last milestone | 4.5a refresh-token domain: issue, rotate, detect replay, revoke a family |
| Next verified step | 4.5b — wire it to HTTP: login also returns a refresh token, `POST /auth/refresh` with cookie **and** explicit transport, `POST /auth/logout`, and CSRF re-enabled for the cookie paths |

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

Flyway runs at startup over the direct (non-pooled) endpoint. Migrations live in
`backend/src/main/resources/db/migration`.

| Version | File | Applied to |
|---|---|---|
| 1 | `V1__create_users_table.sql` | Neon `development`, and every Testcontainers run |
| 2 | `V2__create_verification_tokens_table.sql` | Neon `development`, and every Testcontainers run |
| 3 | `V3__create_refresh_tokens_table.sql` | Neon `development`, and every Testcontainers run |

`V1` creates `users` per specification section 10.1, with `pk_users`, a
`ck_users_preferred_language` CHECK limiting the column to `ro`/`en`, and the
unique index `ux_users_email`.

`V2` creates `verification_tokens` per section 10.10, with a
`ON DELETE CASCADE` foreign key to `users` — which is how section 24 account
deletion reaches this table — a CHECK on the three token types, and the index
`ix_verification_tokens_user_type`.

**Migrations live only in `src/main/resources/db/migration`.** A copy under
`src/test/resources` makes tests pass while the application fails to start:
tests see both `target/test-classes` and `target/classes` on the classpath, but
a running application sees only the latter. This happened once, on 2026-08-13,
and was caught by `ddl-auto: validate` refusing to boot against Neon with a
missing table.

**An applied migration is never edited.** Flyway stores its checksum; changing
the file afterwards fails the next startup with a checksum mismatch. Every
schema change from here is a new `V2__`, `V3__` file.

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

#### Package `ro.garajulmeu.exception`

| Class | Role |
|---|---|
| `ErrorCode` | The canonical catalogue from specification section 17, each code carrying its HTTP status so the same failure cannot answer differently in two endpoints |
| `ApiException` | Thrown for expected business failures; carries an `ErrorCode`, never user-facing text |
| `ApiErrorResponse` | The single JSON shape for every failure: `code`, `status`, `path`, `timestamp`, `fieldErrors`. Deliberately has **no message field**, so the frontend cannot display untranslated server English |
| `GlobalExceptionHandler` | `@RestControllerAdvice` mapping every exception to that shape |

Log levels follow specification section 27: expected 4xx outcomes log at INFO,
only unhandled exceptions log at ERROR — so Sentry, from Phase 15, will not
raise alerts for ordinary business failures.

#### Package `ro.garajulmeu.user`

| Class | Role |
|---|---|
| `User` | `@Entity` on `users`. Id is `@GeneratedValue` UUID; timestamps come from Hibernate `@CreationTimestamp` / `@UpdateTimestamp`. `equals` uses the id alone and `hashCode` is constant, so an entity stays findable in a `HashSet` after Hibernate assigns its id |
| `Language` | `RO("ro")`, `EN("en")` — the code is the same IETF tag i18next and the email templates use |
| `LanguageConverter` | `@Converter(autoApply = true)`. `@Enumerated(STRING)` would write `RO`/`EN` and break the CHECK constraint |
| `UserRepository` | `JpaRepository<User, UUID>` with `findByEmail` and `existsByEmail`; both expect an already-normalised address |
| `UserService` | `profileOf(accountId)` |
| `UserController` | `GET /api/v1/users/me` — the first protected route. The identity comes from the verified token's `sub`, never from a path or query parameter, which is what makes another account's profile unreachable by editing a URL |
| `dto.UserProfileResponse` | Response DTO rather than the entity, so `passwordHash` is never one Jackson change away from the wire |

Email normalisation — trim and lower-case — is the service layer's
responsibility. The entity stores what it is given, and the unique index
enforces one account per address.

#### Package `ro.garajulmeu.auth`

| Class | Role |
|---|---|
| `VerificationTokenType` | `EMAIL_VERIFICATION`, `PASSWORD_RESET`, `EMAIL_CHANGE`. Codes are never interchangeable between purposes, so the type is part of every lookup |
| `VerificationToken` | `@Entity` on `verification_tokens`. Holds the owner as a plain `userId`, not a `@ManyToOne` — the auth module never needs the `User` object from a token, and the foreign key still enforces integrity. `isUsable(now)` requires unused **and** not superseded **and** unexpired |
| `VerificationTokenRepository` | `findFirstByUserIdAndTypeOrderByCreatedAtDesc` for the newest code, and `invalidateOutstandingCodes` — a `@Modifying` bulk update so a double "resend" cannot leave two codes both valid |
| `VerificationCodeGenerator` | `SecureRandom` plus `%06d` padding. Padding matters: returning "42" instead of "000042" would shrink the effective code space |
| `AuthProperties` | `@ConfigurationProperties("garajul-meu.auth")`: 15 minutes code validity, 5 attempts, 30 days refresh-token validity |
| `RefreshToken` | `@Entity` on `refresh_tokens`. Rows are **never deleted on rotation** — a spent token must stay findable, because that is exactly how a replay is detected |
| `RefreshTokenRepository` | `findByTokenHash` on the unique index, and `revokeFamily` as a `@Modifying(flushAutomatically, clearAutomatically)` bulk update |
| `RefreshTokenService` | `startFamily`, `rotate`, `revokeSessionOf`. 32 random bytes, base64url; only the SHA-256 hex is stored |
| `AuthService` | `register`, `verifyEmail`, `resendVerificationCode`, `login` |
| `AuthController` | `POST /api/v1/auth/register` → 201; `/verify-email` and `/resend-verification` → 204; `/login` → 200 with the access token |
| `dto.VerifyEmailRequest` | `@Pattern("\\d{6}")` on the code, so malformed input is rejected before it costs an Argon2 comparison |
| `dto.RegisterRequest` | Bean Validation constraints; password 12–128 characters; language is a `@Pattern("ro\|en")` **string**, not the enum, because JSON carries the lower-case tag while Jackson would expect the constant name |

#### Package `ro.garajulmeu.email`

`EmailProvider` (specification section 32) with `sendVerificationCode(recipient, code, language)`.
`LoggingEmailProvider` writes the message to the log instead of sending it, gated
by `@ConditionalOnProperty(garajul-meu.email.provider=logging)` with **no**
`matchIfMissing`: if the property is absent there is no `EmailProvider` bean and
the application refuses to start, rather than silently writing codes to a
production log. `EmailProperties` declares the key so the IDE knows it; the
Resend API key and sender address join it later.

Only the Argon2 hash of the six-digit code is stored. Argon2 rather than a fast
hash because a six-digit code has only a million possibilities and would be
trivially reversible from a leaked database. The cost is roughly 50 ms and 16 MB
per verification, which makes rate limiting on the auth endpoints a requirement
rather than a nicety.

#### Package `ro.garajulmeu.security`

`SecurityConfig` provides two beans:

- `PasswordEncoder` — `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`: 16 MB memory, two iterations, parallelism one, sixteen byte salt. Chosen over `Argon2Password4jPasswordEncoder` (new in Security 7) because the documentation recommends neither, and the built-in one avoids a further third-party library. **Requires BouncyCastle**, which Spring Boot's BOM does not manage, so `org.bouncycastle:bcprov-jdk18on` is pinned explicitly in `pom.xml`.
- `SecurityFilterChain` — stateless, no session; form login, HTTP Basic and Spring's logout endpoint disabled; `/actuator/health` public, everything else `authenticated()`; an explicit `HttpStatusEntryPoint(UNAUTHORIZED)` so a credential-less request answers 401 rather than the default 403.

JWT is handled entirely by Spring Security — no third-party library, no custom
filter:

| Bean / class | Role |
|---|---|
| `JwtProperties` | `garajul-meu.jwt`. `secret` has **no default** and is `@Validated @NotBlank @Size(min = 32)`, so a missing or too-short key stops startup rather than silently weakening the signature |
| `JwtEncoder` | `NimbusJwtEncoder` over an `ImmutableSecret`. One symmetric HS256 key signs and verifies, because this application both issues and consumes its own tokens |
| `JwtDecoder` | `NimbusJwtDecoder.withSecretKey(...)`. **Declaring it also removed Spring Boot's development user** — `UserDetailsServiceAutoConfiguration` lists `JwtDecoder` among the beans it backs off for, so `Using generated security password` no longer appears |
| `AccessTokenService` | Issues the token. Claims are exactly `iss`, `iat`, `exp`, `sub` — the account id and nothing personal, since a JWT is only base64 and lives on the client |
| `oauth2ResourceServer(jwt())` | Spring's own `BearerTokenAuthenticationFilter` reads `Authorization: Bearer`, verifies signature and expiry, and populates the security context |

There is still no login endpoint, so no token can be obtained yet; every path
except health and `/api/v1/auth/**` answers 401.

#### Package `ro.garajulmeu.common`

`RequestIdFilter` (`@Order(HIGHEST_PRECEDENCE)`) gives each request a
correlation identifier, places it in the SLF4J MDC under `requestId`, returns it
in the `X-Request-Id` response header, and clears the MDC in a `finally` block
because servlet threads are pooled.

A caller-supplied `X-Request-Id` is honoured only when it is at most 64
characters of `[A-Za-z0-9._-]`; anything else is replaced with a fresh UUID.
An unvalidated header would be a log-injection vector — newlines let a caller
forge log entries and an unbounded value lets them flood log storage.

`logging.pattern.correlation` in `application.yml` fills the slot Spring Boot
reserves inside its default console pattern, so the identifier appears on every
line without replacing the standard format or losing colours. Lines produced
outside a request show `[system]`.

**Codes added beyond the section 17 examples**, for HTTP-level failures that
belong to no domain: `MALFORMED_REQUEST`, `RESOURCE_NOT_FOUND`,
`METHOD_NOT_ALLOWED`. Without them an unparseable request body would be
reported as `INTERNAL_ERROR`, blaming us for a client mistake. Section 17
explicitly allows extending the canonical catalogue.

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
| `GlobalExceptionHandlerTest.businessFailureAnswersWithItsOwnCodeAndStatus` | `ApiException(VEHICLE_NOT_FOUND)` produces 404 with that code |
| `GlobalExceptionHandlerTest.unexpectedFailureNeverLeaksInternalDetail` | An exception whose message contains an internal host and a database user yields only `INTERNAL_ERROR`; neither string nor the exception class name appears in the body |
| `RequestIdFilterTest.generatesAnIdentifierWhenTheClientSendsNone` | MDC is populated during the chain and the value matches the response header |
| `RequestIdFilterTest.reusesAnIdentifierTheClientSuppliedSoOneRequestCanBeTracedEndToEnd` | A safe caller-supplied identifier is preserved |
| `RequestIdFilterTest.replacesAnUnsafeClientIdentifier...` | A header containing a newline is discarded rather than written to the log |
| `RequestIdFilterTest.clearsTheMdcOnceTheRequestIsFinished` | No identifier leaks to the next request served by the same pooled thread |
| `UserRepositoryTest` (5 tests) | Migration defaults are applied, a new account is unverified, lookup by email works, a duplicate email is rejected by `ux_users_email`, and the language round-trips as its lower-case code |
| `PasswordEncoderTest` (3 tests) | The configured encoder produces `$argon2id$` output, never the plain password; the same password hashes differently each time thanks to a per-password salt; only the exact original password matches. Tests the bean `SecurityConfig` actually provides, so hashing cannot be weakened unnoticed |
| `RefreshTokenServiceTest` (7 tests) | Only the digest is stored; rotation stays in the family and links the chain; replaying a spent token revokes the whole family and takes the honest holder's current token with it; unknown and expired tokens are refused; logout ends every token in the family; two logins get independent families, so signing out on the phone leaves the laptop alone |
| `AuthFlowTest` (7 tests) | Full HTTP surface: a token from login opens `/users/me`; no token and a forged token both answer 401; the profile body contains neither `argon2` nor `passwordHash`; a wrong password and an unknown address answer the identical `INVALID_CREDENTIALS`; an unverified account answers `EMAIL_NOT_VERIFIED` |
| `AccessTokenServiceTest` (4 tests) | Our own decoder accepts the issued token and reads the account id back; expiry lands inside the configured window; the token carries **only** `iss`, `iat`, `exp`, `sub`; a token signed with a different key is rejected. Builds the beans directly rather than starting Spring, so it runs in about a tenth of a second while still exercising the real configuration |
| `VerificationTokenRepositoryTest` (5 tests) | A fresh code is usable; an expired one is not; a spent one cannot be reused; a resend supersedes every outstanding code; codes of another purpose are untouched |
| `VerificationCodeGeneratorTest` (2 tests) | Always exactly six digits over a thousand draws, which also proves the zero padding; two hundred draws are almost all distinct |
| `AuthServiceTest` (10 tests) | Registration: address normalised, password `$argon2id$`, emailed code matches the stored hash, duplicate address rejected in any case, language defaults to Romanian. Verification: a correct code verifies the account, a wrong one is counted, a spent code cannot be reused, an expired one answers `VERIFICATION_CODE_EXPIRED`, a resend marks the earlier token invalidated and the old code stops working, and a resend for an unknown address sends nothing |

`AuthServiceTest` replaces `EmailProvider` with `@MockitoBean` to capture the
emitted code. That changes the context configuration, so this class gets its own
Spring context and therefore its own PostgreSQL container — the build starts two.

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
| 2026-08-13 | Error responses use a small project-specific JSON shape rather than RFC 9457 Problem Details. Problem Details carries a human-readable `detail` field, which conflicts with the section 6 requirement that all user-facing wording is chosen and translated by the frontend. |
| 2026-08-13 | `ErrorCode` owns its HTTP status. Keeping the mapping in the catalogue rather than at each throw site prevents the same failure answering 404 in one endpoint and 400 in another. |
| 2026-08-13 | Boot 4 moved the test slices: `@WebMvcTest` and `@AutoConfigureMockMvc` are now in `org.springframework.boot.webmvc.test.autoconfigure`, not `org.springframework.boot.test.autoconfigure.web.servlet`. |
| 2026-08-13 | Handler tests use the `@WebMvcTest` slice with a throwaway controller, so they need no database and run in about one second rather than twenty. Slice tests are the default; full `@SpringBootTest` is reserved for cases that genuinely need persistence. |
| 2026-08-13 | Correlation uses a plain MDC entry and `logging.pattern.correlation`, not Micrometer Tracing. Distributed tracing solves a problem a single-instance modular monolith does not have (specification section 19). If tracing is introduced later, the same pattern slot is where its trace and span ids belong. |
| 2026-08-13 | `ApiErrorResponse` carries `requestId`, so a user can quote the identifier from an error screen and the exact request can be found in the logs without knowing their account or the time. |
| 2026-08-13 | Verification codes are hashed with the same Argon2 encoder as passwords. A six-digit code has only a million possibilities and a fast hash would be trivially reversible from a leaked database. |
| 2026-08-13 | Registration answers `EMAIL_ALREADY_EXISTS` rather than hiding whether an address is taken. Specification section 17 defines that code and section 14 requires non-disclosure only for forgot-password. |
| 2026-08-13 | `existsByEmail` is backed up by catching `DataIntegrityViolationException` and re-throwing the same code. Two simultaneous registrations can both pass the check and collide only at the unique index; without the catch the caller would get `INTERNAL_ERROR` for an ordinary conflict. |
| 2026-08-13 | Password policy: 12–128 characters, no composition rules. Current guidance favours length over forced symbols, which mostly produce predictable substitutions. The maximum bounds the Argon2 work per request. |
| 2026-08-13 | `ExceptionHandlerExceptionResolver` is pinned to ERROR level. At its default WARN it logs every resolved exception including rejected field values, which put an attempted password into the log during a manual registration test — forbidden by specification section 30. Our own handler logs field names only. Found by reading the log during manual verification; no automated test asserts on framework logging, so this class of leak needs a human eye. |
| 2026-08-13 | Refresh tokens are hashed with **SHA-256, not Argon2** — the opposite of passwords and verification codes, for two reasons. The token holds 256 bits of entropy, so brute force from a leaked database is irrelevant. And Argon2's per-hash salt makes lookup by value impossible: every refresh would have to load every token and compare each at ~50 ms. **The hash choice follows the entropy of the secret, not habit.** |
| 2026-08-13 | A `familyId` is one login on one device. Rotation keeps the family; reuse revokes it entirely; logout revokes it. Signing out on one device therefore leaves other devices signed in. |
| 2026-08-13 | Detected reuse revokes the family, which also invalidates the honest holder's current token. That is deliberate: the alternative is letting a thief refresh indefinitely. A stolen token buys at most one refresh before it becomes an alarm. |
| 2026-08-13 | A token revoked by logout is indistinguishable from one revoked by rotation, so logging out and then replaying answers `REFRESH_TOKEN_REUSED` rather than something more precise. Distinguishing them would need another column; noted as an observation, not a defect. |
| 2026-08-13 | Login hashes a throwaway value when the address is unknown, so a missing account costs the same as a wrong password. The database lookup is microseconds and an Argon2 comparison is tens of milliseconds; without the dummy hash, response time alone reveals which addresses hold accounts. |
| 2026-08-13 | Login checks the password **before** `isEmailVerified`. The other order would tell anyone who guesses an address that it exists and is unverified, without knowing the password. As written, `EMAIL_NOT_VERIFIED` only reaches someone who has already proved they know it. |
| 2026-08-13 | `LoginResponse` returns `expiresInSeconds`, not an absolute timestamp. A phone with a wrong clock would misjudge an absolute expiry; a duration is immune to clock skew. This is also the OAuth2 convention. |
| 2026-08-13 | `LoginRequest` does **not** carry the 12-character minimum that `RegisterRequest` does. A length policy applies to new passwords; enforcing it at login would lock existing users out of their own accounts the day the policy tightens. |
| 2026-08-13 | JWT uses Spring Security's own Nimbus-backed `JwtEncoder`/`JwtDecoder` rather than `jjwt`. It adds no third-party library, and `oauth2ResourceServer(jwt())` supplies the bearer-token filter, so no authentication filter is written by hand. |
| 2026-08-13 | Symmetric HS256 with a shared secret, not an RSA key pair. An asymmetric key only earns its complexity when a separate service must verify without being able to sign; here one application does both. |
| 2026-08-13 | The issuer claim is the plain string `garajul-meu`, not a URL. RFC 7519 defines `iss` as StringOrURI, so this is valid — but `Jwt.getIssuer()` insists on a URL because it comes from OAuth2, so read `getClaimAsString("iss")` instead. A URL issuer would also bake the still-undecided production domain into every token. |
| 2026-08-13 | `verifyEmail` is annotated `@Transactional(noRollbackFor = ApiException.class)`. A business exception normally rolls the transaction back, which would discard the failed-attempt counter recorded immediately before it — the attempt limit that makes a six-digit code safe would silently never fire. |
| 2026-08-13 | Verification answers `VERIFICATION_CODE_INVALID` for an unknown address, so the endpoint cannot be used to discover which addresses hold accounts. An expired code answers `VERIFICATION_CODE_EXPIRED` distinctly: the caller already held a valid code for that address, so nothing is disclosed, and the client needs the distinction to offer "resend" rather than "retry". |
| 2026-08-13 | `resend-verification` answers 204 identically for an unknown address, an already verified one and a real reissue. |
| 2026-08-13 | Because verification always reads the **newest** token for the account, `invalidateOutstandingCodes` is defence in depth rather than the active mechanism — an older token is never consulted. Its test asserts the `invalidatedAt` column directly, so it cannot pass merely because a newer code shadows the old one. |
| 2026-08-13 | `invalidateOutstandingCodes` is annotated `@Modifying(flushAutomatically = true, clearAutomatically = true)`. A bulk JPQL update bypasses the persistence context entirely: without `flushAutomatically` a freshly persisted but unflushed token escapes the UPDATE unnoticed, and without `clearAutomatically` every already-loaded entity keeps its stale values. **After any bulk update, previously loaded entities must not be trusted.** The defect was invisible until a test asserted the column instead of the outcome. |
| 2026-08-13 | Slice tests must name what they load. `GlobalExceptionHandlerTest` uses `@WebMvcTest(controllers = …)`: the slice instantiates every `@RestController` but excludes every `@Service`, so the first real controller broke an unrelated test. Scoping the slice is the fix, not mocking each new controller's dependencies. |

## Known issues and open decisions

### Deferred work with a scheduled phase

| Item | Phase |
|---|---|
| CSRF is disabled outright. It must be switched back on for the cookie-authenticated `/auth/refresh` and `/auth/logout` paths, per specification section 14. | 4.5 |
| `/api/v1/auth/**` is permitted wholesale. Endpoints added under that prefix that should require authentication must be matched individually. | 4.5 |
| Registration sends the email inside the transaction, so a provider outage rolls the account back. Simple and safe today; revisit if Resend proves flaky. | 4.7 |
| Auth endpoints are not rate limited. This matters more than usual here because verifying a code costs an Argon2 hash — roughly 50 ms and 16 MB — so an unthrottled endpoint is a cheap denial-of-service. | 4.6 |
| `spring-boot-configuration-processor` only runs because `maven.compiler.proc=full` is set in `pom.xml`. **JDK 21 requires the option to be set explicitly**; without it the processor is silently skipped and no metadata is generated. | done |
| `HttpStatusEntryPoint` returns a bare 401 with no body, so authentication failures do not use the `ApiErrorResponse` shape every other error uses. Replace it once a suitable error code exists. | 4.4 |
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

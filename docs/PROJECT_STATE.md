# PROJECT_STATE

Portable state of the guided build. Updated at every meaningful milestone so the
project can be continued in a new conversation, or by a different AI assistant,
without relying on model memory.

Last updated: 2026-08-14

---

## Current position

| Item | Value |
|---|---|
| Phase | 4 — Authentication & Users — **in progress** |
| Last **committed** milestone | **4.7 password reset — verified green on 2026-08-14.** Every authentication flow specification section 14 requires now exists |
| Applied, verified green, **not yet committed** | **4.8a account management** — `PATCH /api/v1/users/me` and `POST /api/v1/users/me/change-password`. Five files: `dto.UpdateProfileRequest` and `dto.ChangePasswordRequest` (new), `UserService` and `UserController` (modified), `UserProfileFlowTest` (new, 8 tests) |
| Commit history note | `ca3c670` is 4.5b. `6736ef8` is labelled as the Jackson 3 login fix but **also carries the whole authentication-entry-point change** — `ErrorCode`, `ApiErrorAuthenticationEntryPoint`, `SecurityConfig`, `AuthFlowTest`. Two commits were intended and collapsed into one. Left as it is: it is already on the public remote, and rewriting published history for an imprecise message is not worth it. Recorded here so the entry point can still be found |
| Verified build | `.\mvnw.cmd clean verify` → **`Tests run: 90, Failures: 0, Errors: 0, Skipped: 0`**, `BUILD SUCCESS`, still **four** PostgreSQL containers |
| Next verified step | 4.8b — `POST /users/me/change-email` and `/confirm-email-change`, over the `EMAIL_CHANGE` token type and the `target_value` column that already exist, so **no new migration**. Two decisions taken 2026-08-14 and not yet implemented: (1) the confirmation code goes **only to the old address**, which proves control of the account; (2) because that proves nothing about the *new* address, confirming sets `email` to the new value, resets `email_verified_at` to null and issues a fresh `EMAIL_VERIFICATION` code to the new address through the existing 4.2 flow. Sessions are **not** revoked — the password did not change, and a live session is the only way to correct a mistyped address. **Residual risk, accepted knowingly:** login refuses unverified accounts and `forgot-password` writes to the account address, so a user who mistypes the new address and then loses their session has an unrecoverable account. Then 4.9 — `DELETE /users/me`, the largest of the three: specification section 24 makes deletion permanent across every table the account touches |

### What 4.5b delivers

`POST /api/v1/auth/refresh` and `/logout` accept the refresh token **either** from
the `garajul_meu_refresh` cookie **or** from a `{"refreshToken": "..."}` body, and
answer on whichever channel the request used. No client-type header is needed,
as specification section 14 requires: the request itself reveals how the client
works.

Login always sets the `HttpOnly` `Secure` `SameSite=Strict` cookie scoped to
`/api/v1/auth`, and additionally returns the token in the body only when the
caller sends `"refreshTokenInBody": true` — so a browser's JavaScript never sees
it, while a native client, which has no cookie jar, can ask for it. Only `/login`
needs that flag: at login the client has presented nothing yet, so the backend
cannot infer the channel. On `/refresh` and `/logout` it can, and does.

`AuthService.login` is no longer `@Transactional(readOnly = true)`: issuing a
refresh token writes a row.

Logout is idempotent — logging out twice, or with no token at all, answers 204
and clears the cookie regardless.

## How this project is built — working agreement

This is a **guided build**, not code generation. The cycle is: explain → give the
exact command or file → the developer applies it → run → verify → fix → continue.
Stop at each meaningful milestone and wait for the developer's actual output
before going further.

| Rule | Detail |
|---|---|
| Who writes files | **The developer.** Provide the exact path, whether it is new or modified, the full content, and the reasoning. Do not write source files unless explicitly asked. |
| This document | Maintained by the assistant, not the developer. Update it at every milestone. |
| Language | Explanations in Romanian; technical terms, code, comments and identifiers in English. |
| Verification | Read the log, not just `BUILD SUCCESS`. **Always check the total test count** — a test lost to a bad edit leaves the build green and the number smaller. This has happened. |
| Versions | Verify current documentation before giving version-specific instructions. Spring Boot 4 renamed many artifacts and packages, and most material online targets 3.x. |
| Paste fragility | Long files and nested XML have repeatedly lost lines in transit. After pasting, check the file size or line count. If a paste fails twice on the same content, ask before switching approach. |
| Scope | Implement only V1 features from the specification. Surface any deviation and wait for approval rather than deciding alone. |

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
| Docker Desktop | 29.3.1, daemon verified working — Testcontainers 2.0.5 connects over the local npipe, API 1.54 |

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

The `/api/v1` surface implemented so far is the authentication block plus the
account-management endpoints `GET`, `PATCH /api/v1/users/me` and
`POST /api/v1/users/me/change-password`; see the package tables below.

#### Package `ro.garajulmeu.exception`

| Class | Role |
|---|---|
| `ErrorCode` | The canonical catalogue from specification section 17, each code carrying its HTTP status so the same failure cannot answer differently in two endpoints. `AUTHENTICATION_REQUIRED` covers both a missing bearer token and one that fails verification — one code, because the client's next move is the same either way |
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
| `UserService` | `profileOf`, `updateProfile`, `changePassword`. Injects `PasswordEncoder` and `RefreshTokenService` — the deliberate `user` → `auth` edge recorded in the decisions table. `validZone` checks the timezone against `ZoneId.getAvailableZoneIds()`, because membership of a set resolved at runtime cannot be expressed as a Bean Validation annotation without a custom constraint |
| `UserController` | `GET /api/v1/users/me` — the first protected route; `PATCH /me` → 200 with the updated profile; `POST /me/change-password` → **204**. For all three the identity comes from the verified token's `sub`, never from a path or query parameter, which is what makes another account unreachable by editing a URL |
| `dto.UserProfileResponse` | Response DTO rather than the entity, so `passwordHash` is never one Jackson change away from the wire |
| `dto.UpdateProfileRequest` | Partial update: every component optional, absent means unchanged. Deliberately carries **no** `@NotBlank` — that would forbid absence, which is the whole point of PATCH |
| `dto.ChangePasswordRequest` | `currentPassword` `@NotBlank` with no length bound; `newPassword` 12–128, the same bound as registration and reset |

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
| `RefreshTokenRepository` | `findByTokenHash` on the unique index, plus two `@Modifying(flushAutomatically, clearAutomatically)` bulk updates: `revokeFamily` for one session and `revokeAllForUser` for every session an account has anywhere, which section 14 requires after a password reset |
| `RefreshTokenService` | `startFamily`, `rotate`, `revokeSessionOf`, `revokeAllSessionsOf`. 32 random bytes, base64url; only the SHA-256 hex is stored |
| `RefreshCookies` | Builds the `garajul_meu_refresh` cookie: `HttpOnly`, `Secure`, `SameSite=Strict`, path `/api/v1/auth`, lifetime from `AuthProperties`. `clear()` returns the same cookie empty with a zero max-age, which is how a cookie is deleted |
| `AuthService` | `register`, `verifyEmail`, `resendVerificationCode`, `forgotPassword`, `resetPassword`, `login`, `refresh`, `logout`. Returns `LoginResult` — the controller decides how the token travels. Two private helpers carry the shared machinery: `issueCode(user, type)` supersedes any outstanding code and returns a new one, and **`consumeCode(userId, type, code, now)` is the single place a six-digit code is checked** — spent, expired, too many attempts, wrong. Verification and reset need the identical sequence, and writing it twice is how two flows end up with quietly different rules. The type is always part of the lookup, so a code issued for one purpose cannot open another |
| `AuthController` | `POST /api/v1/auth/register` → 201; `/verify-email` and `/resend-verification` → 204; `/forgot-password` and `/reset-password` → 204; `/login` → 200 with the access token and the refresh cookie; `/refresh` → 200 on the channel the caller used; `/logout` → 204, idempotent |
| `dto.ForgotPasswordRequest` / `dto.ResetPasswordRequest` | The reset pair. The code is `@Pattern("\\d{6}")`, so malformed input is rejected before it costs an Argon2 comparison, and the new password carries the same 12–128 bound as registration — a length policy applies to new passwords, and this is one |
| `dto.VerifyEmailRequest` | `@Pattern("\\d{6}")` on the code, so malformed input is rejected before it costs an Argon2 comparison |
| `dto.RegisterRequest` | Bean Validation constraints; password 12–128 characters; language is a `@Pattern("ro\|en")` **string**, not the enum, because JSON carries the lower-case tag while Jackson would expect the constant name |
| `dto.LoginRequest` | `refreshTokenInBody` is a boxed `Boolean`, not a primitive — see the Jackson 3 decision below. `wantsRefreshTokenInBody()` is the single place that decides absent means false |
| `dto.RefreshRequest` / `dto.RefreshResponse` | The explicit-token channel used by native clients. `RefreshResponse.refreshToken` is null when the caller arrived by cookie |
| `AuthRateLimit` | Applies the policies in the controller, where the network address lives, and turns an exhausted budget into `RATE_LIMITED`. **Two keys per request**, address and email address, because either alone is avoidable: address-only punishes a whole NAT for one person, email-only lets an attacker rotate addresses and never spend a budget. The network address is checked **first**, and an exhausted one returns before the email key is ever created — that ordering is what bounds the limiter's memory. Reuses `AuthService.normalise`, now package-private, so that `A@B.com` and `a@b.com` cannot hold separate budgets |

#### Package `ro.garajulmeu.email`

`EmailProvider` (specification section 32) with `sendVerificationCode` and
`sendPasswordResetCode`, both `(recipient, code, language)`. Two methods rather
than one with a purpose argument: the templates say different things, and a
reader who receives the wrong wording learns something false about their account.
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
- `SecurityFilterChain` — stateless, no session; form login, HTTP Basic and Spring's logout endpoint disabled; `/actuator/health` public, everything else `authenticated()`; `ApiErrorAuthenticationEntryPoint` wired in **two** places, so a credential-less request answers 401 rather than the default 403, and every 401 carries a body.

`ApiErrorAuthenticationEntryPoint` answers an unauthenticated request with the
same `ApiErrorResponse` shape as everything else. It has to exist because
authentication fails inside the filter chain, before any controller, where
`@RestControllerAdvice` cannot reach. It serialises with the injected
`tools.jackson.databind.ObjectMapper` — the bean Spring MVC itself uses, not a
fresh instance — so the JSON matches every other error byte for byte, and it
reads the correlation id from the MDC exactly as `GlobalExceptionHandler` does.

JWT is handled entirely by Spring Security — no third-party library, no custom
filter:

| Bean / class | Role |
|---|---|
| `JwtProperties` | `garajul-meu.jwt`. `secret` has **no default** and is `@Validated @NotBlank @Size(min = 32)`, so a missing or too-short key stops startup rather than silently weakening the signature |
| `JwtEncoder` | `NimbusJwtEncoder` over an `ImmutableSecret`. One symmetric HS256 key signs and verifies, because this application both issues and consumes its own tokens |
| `JwtDecoder` | `NimbusJwtDecoder.withSecretKey(...)`. **Declaring it also removed Spring Boot's development user** — `UserDetailsServiceAutoConfiguration` lists `JwtDecoder` among the beans it backs off for, so `Using generated security password` no longer appears |
| `AccessTokenService` | Issues the token. Claims are exactly `iss`, `iat`, `exp`, `sub` — the account id and nothing personal, since a JWT is only base64 and lives on the client |
| `oauth2ResourceServer(jwt())` | Spring's own `BearerTokenAuthenticationFilter` reads `Authorization: Bearer`, verifies signature and expiry, and populates the security context |

Every path except `/actuator/health**` and `/api/v1/auth/**` answers 401 without a
valid bearer token. All six endpoints under `/api/v1/auth/**` are legitimately
pre-authentication, so the wholesale `permitAll` is correct **today** — see the
standing rule in the deferred-work table before adding anything else there.

#### Package `ro.garajulmeu.common`

| Class | Role |
|---|---|
| `RateLimiter` | The seam. One method, `tryConsume(key, policy)`, returning a boolean rather than throwing — which error code an exhausted budget produces belongs to the layer that knows what the caller was attempting |
| `InMemoryRateLimiter` | A fixed-window counter per key in a `ConcurrentHashMap`, using `compute` so two threads on one key cannot both write `count + 1`. The counter is capped one past the limit, and hammering never pushes the window forward — otherwise a refused caller could keep their own lockout alive by retrying. Expired entries are swept at most once a minute, because a map that keeps every key ever seen is itself a way to exhaust memory |
| `RateLimitProperties` | Policies grouped by what a request costs: `credentialCheck` 10/15m (login, verify-email — each pays an Argon2 comparison), `emailDispatch` 5/1h (register, resend — each sends mail), `tokenRefresh` 60/1h. A policy with a limit and no window is refused at startup rather than binding a null and failing mid-attack |
| `TimeConfig` | A `Clock` bean. `Instant.now()` cannot be moved forward in a test, so elapsed-time logic becomes either untestable or slow. Phase 11 will need this more than the limiter does |

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
| `PasswordResetServiceTest` (9 tests) | A request for an unknown address sends nothing; the emailed code replaces the password; a wrong code is counted and leaves the password alone; an expired code answers its own error; a spent code cannot be used twice; asking again invalidates the earlier code; **resetting ends every session on every device**; resetting verifies an address that was never verified, and leaves an already verified one verified |
| `AuthFlowTest` (20 tests) | Full HTTP surface. Session: a token from login opens `/users/me`; no token and a forged token both answer 401 with `AUTHENTICATION_REQUIRED`, and the unauthenticated error's `requestId` matches the `X-Request-Id` header; the profile body contains neither `argon2` nor `passwordHash`; a wrong password and an unknown address answer the identical `INVALID_CREDENTIALS`; an unverified account answers `EMAIL_NOT_VERIFIED`. Transport: login always sets the `HttpOnly` `Secure` cookie and omits the token from the body; a caller asking for it in the body receives it; refresh on the cookie channel answers on the cookie channel and on the body channel answers in the body; a replayed token answers `REFRESH_TOKEN_REUSED`; refresh with no token at all answers `REFRESH_TOKEN_INVALID`; logout clears the cookie and kills the session; logout with no token is not an error. Password reset: `forgot-password` answers identically for a known and an unknown address; the whole flow ends with the new password working and the old one refused; a malformed code is rejected as `VALIDATION_ERROR` on the `code` field before it costs a hash; and resetting ends a session that was already open |
| `InMemoryRateLimiterTest` (5 tests) | No Spring and no sleeping — the clock is moved by hand, so a fifteen-minute window is verified in microseconds. Exactly the configured number of attempts is allowed; the window reopens once it has elapsed; hammering does not extend the lockout; two keys do not share a budget; expired entries are swept so the map does not grow forever |
| `AuthRateLimitTest` (4 tests) | The wiring, at two attempts per policy so the boundary arrives in three requests. One address cannot keep guessing by changing the email; another address is unaffected by a noisy one; one account cannot be attacked from many addresses — **the test that justifies the second key**; and spending the login budget leaves the resend budget intact |
| `AccessTokenServiceTest` (4 tests) | Our own decoder accepts the issued token and reads the account id back; expiry lands inside the configured window; the token carries **only** `iss`, `iat`, `exp`, `sub`; a token signed with a different key is rejected. Builds the beans directly rather than starting Spring, so it runs in about a tenth of a second while still exercising the real configuration |
| `VerificationTokenRepositoryTest` (5 tests) | A fresh code is usable; an expired one is not; a spent one cannot be reused; a resend supersedes every outstanding code; codes of another purpose are untouched |
| `VerificationCodeGeneratorTest` (2 tests) | Always exactly six digits over a thousand draws, which also proves the zero padding; two hundred draws are almost all distinct |
| `AuthServiceTest` (10 tests) | Registration: address normalised, password `$argon2id$`, emailed code matches the stored hash, duplicate address rejected in any case, language defaults to Romanian. Verification: a correct code verifies the account, a wrong one is counted, a spent code cannot be reused, an expired one answers `VERIFICATION_CODE_EXPIRED`, a resend marks the earlier token invalidated and the old code stops working, and a resend for an unknown address sends nothing |
| `UserProfileFlowTest` (8 tests) | The two account-management endpoints over HTTP. PATCH leaves every field the body does not mention and trims the name it is given; it accepts a language and a real timezone; it rejects a whitespace-only name as `VALIDATION_ERROR` on `fullName`, and `Europe/Bucuresti` — plausible, but not a zone — as `VALIDATION_ERROR`. **It changes only the account the token belongs to**: a second account exists, the first account's token renames itself, and the second row is read back untouched. That is the ownership property in the only shape this endpoint can have, since there is no path parameter to tamper with. Change-password refuses a wrong `currentPassword` with `INVALID_CURRENT_PASSWORD` and the old password still logs in afterwards; a correct one answers 204, **kills a refresh token issued before the change**, and the new password logs in. Both endpoints, and `GET /me`, answer 401 `AUTHENTICATION_REQUIRED` with no token |

`AuthServiceTest`, `PasswordResetServiceTest` and `AuthFlowTest` all replace
`EmailProvider` with `@MockitoBean` — it is the only way to read a code that is
stored as an Argon2 hash and therefore irreversible by design. Each *distinct*
context configuration buys a separate Spring context and PostgreSQL container,
so the build starts **four**: `AuthFlowTest`, `AuthRateLimitTest` (which
overrides the rate limit properties), `AuthServiceTest`, and
`RefreshTokenServiceTest`. Two classes reuse an existing context instead of
adding a fifth container: `PasswordResetServiceTest` matches `AuthServiceTest`,
and `UserProfileFlowTest` matches `AuthFlowTest` — **including an
`EmailProvider` mock it never uses**, because `@MockitoBean` joins the cache key
through `BeanOverrideContextCustomizer`. Both run in about a second with no
container of their own, and the build log proves it: neither prints a Spring
banner. **Matching an existing class's annotations exactly is what keeps the
container count from growing with every new test class**, and an unused
`@MockitoBean` is a legitimate way to match — documented in the test as
intentional, or the next reader deletes it as debris.

**The rate limiter is a singleton and its counters are not transactional**, so
they survive between test methods and between test classes sharing a context.
`src/test/resources/application.yml` therefore sets every limit to 10000.
Realistic limits there would make unrelated tests fail depending on the order
they happened to run in — `AuthFlowTest` alone logs in eleven times, which is
exactly how this was found.

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
| 2026-08-14 | **No primitive component in any request DTO.** Spring Boot 4 ships **Jackson 3** (3.1.4), which enables `DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES` by default — the opposite of Jackson 2. An absent JSON property reaches a record's canonical constructor as null, and `null → boolean` now throws before the constructor runs, so the *whole request* becomes unreadable rather than defaulting to false. `LoginRequest.refreshTokenInBody` was a primitive `boolean`, so any login that omitted the flag — every browser login — answered `400 MALFORMED_REQUEST`. Six `AuthFlowTest` failures caught it. Fix: box the component and let `wantsRefreshTokenInBody()` own the meaning of absent. A compact constructor cannot rescue this; the failure is earlier. |
| 2026-08-14 | The global escape hatch `spring.jackson.deserialization.fail-on-null-for-primitives: false` was **rejected**. It would weaken deserialization for every DTO in the project permanently. For a missing boolean, false is plausible; for a missing `int`, 0 is a meaningful and usually wrong value — an absent `offset_days` or `engine_capacity_cc` would become a silent zero instead of a rejected request. Jackson 3 turned this on deliberately. Corrections are paid at the defect, not by disabling a protection project-wide. |
| 2026-08-14 | Only `/login` carries a channel flag. At login the client has presented nothing, so the backend cannot infer whether it has a cookie jar; `refreshTokenInBody` is an explicit opt-in whose default is the safe one. On `/refresh` and `/logout` no flag exists or is needed — the presence of a cookie or a body token *is* the answer. Section 14's ban on an `X-Client-Type` header is therefore satisfied in substance, not just in spelling. |
| 2026-08-14 | **A successful password reset also marks the address verified.** Entering a code that arrived by email proves exactly what email verification asks for, so treating the two differently would be an inconsistency rather than a precaution. It also removes a real dead end: register, never verify, forget the password, reset successfully — and still be refused at login with `EMAIL_NOT_VERIFIED`, with nothing on screen explaining why. |
| 2026-08-14 | The six-digit code check was extracted into one private `consumeCode`. The same security-sensitive sequence written in two flows is how they quietly drift apart. The refactor touched working code, and the ten existing `AuthServiceTest` tests were the safety net that made it safe — they stayed 10/10. |
| 2026-08-14 | Rate limiting is **written by hand**, not Bucket4j. One algorithm — a fixed-window counter — for a single-instance backend is roughly fifty testable lines, and specification section 19 says any move to several instances needs shared storage designed first, so a distributed token-bucket library would be carrying machinery we would replace anyway. Section 4 of the Master Prompt does not list Bucket4j, so adding it would have needed an approved architecture change. |
| 2026-08-14 | Rate limit policies are grouped by **what a request costs**, not by endpoint, so the grouping explains itself. `/login` and `/verify-email` share `credentialCheck` because each pays an Argon2 comparison; `/register` and `/resend-verification` share `emailDispatch` because each sends mail to a real inbox. The policy name is part of the key, so the groups never draw on one another's counter. |
| 2026-08-14 | Two keys per request, address **and** email, checked in that order. Either key alone is avoidable: counting only by address punishes everyone behind one NAT for one person's behaviour, and counting only by email lets a distributed attacker rotate emails and never spend a budget. The order is not cosmetic — an exhausted address returns before an email key is created, so flooding with distinct emails cannot grow the map without first spending the address budget. A distributed attack still grows it; that residual is bounded by the number of addresses and is what shared storage fixes at scaling time. |
| 2026-08-14 | `/logout` is deliberately **not** rate limited. Refusing a logout is worse than allowing one: it leaves a user who asked to end their session with a live session. The endpoint costs a SHA-256 lookup, and revoking anything requires already holding a valid 256-bit token. |
| 2026-08-14 | Configuration metadata for `@ConfigurationProperties` records comes from the annotation processor, which emits correctly prefixed and correctly typed entries into `spring-configuration-metadata.json`. **Do not accept Eclipse's "create metadata" quick fix for these keys**: on 2026-08-14 it wrote six entries into `additional-spring-configuration-metadata.json` missing the `garajul-meu.` prefix and typed `java.lang.String` instead of `Integer` and `Duration`. The two files merge, so the result was six phantom keys that bind to nothing and would be ignored in silence if anyone used them. Reverted. |
| 2026-08-14 | **There are two paths to a 401, and `exceptionHandling()` governs only one.** A request with *no* token is refused later by `AuthorizationFilter` and handled by `ExceptionTranslationFilter`, which uses the global entry point. A request whose bearer token *fails to decode* is refused by `BearerTokenAuthenticationFilter`, which calls **its own** entry point on the spot and never reaches `ExceptionTranslationFilter`. The custom entry point must therefore be set on `oauth2ResourceServer(...)` as well as on `exceptionHandling(...)`; neither replaces the other. Wiring only the first left forged tokens on Spring's default, and `AuthFlowTest` caught it. |
| 2026-08-14 | Replacing the resource server's default entry point also removes its `WWW-Authenticate` header, which is a gain rather than a loss. That header carried `error_description="An error occurred while attempting to decode the Jwt: ..."` — English prose the frontend must own per section 6 — and `resource_metadata` naming the server host, which section 30 keeps out of responses. RFC 6750 recommends the header; our only client reads the JSON code, so the recommendation buys nothing and costs a leak. |
| 2026-08-14 | **CSRF stays disabled through Phase 15; the token mechanism arrives at Phase 16.** The refresh cookie is `SameSite=Strict`, `HttpOnly`, `Secure` and path-scoped to `/api/v1/auth`, so a browser attaches it to no cross-site request at all — that is the primary defence against exactly the attack CSRF tokens prevent, not a secondary one. A CSRF token on top is genuine defence in depth, but it needs a frontend that reads and replays it, and that frontend does not exist yet; nor can it be tested convincingly on localhost, where there are no two real origins. It lands with `app.<domain>` and `api.<domain>`. Residual risk accepted until then: an XSS or subdomain takeover on any same-site host could issue same-site requests, which a token would still block. |
| 2026-08-14 | **PATCH means absent-is-unchanged, and that decides the validation.** `UpdateProfileRequest` carries no `@NotBlank`: it rejects an absent value, which would turn PATCH into PUT. `@Pattern(".*\\S.*")` does the work `@NotBlank` would have done without forbidding absence — Bean Validation skips a null, so only a value that is actually present has to contain a non-whitespace character. |
| 2026-08-14 | The timezone is checked in the service rather than declaratively. Membership of `ZoneId.getAvailableZoneIds()` cannot be expressed as an annotation without a custom constraint, and the price of not writing one is that the response carries no field name — acceptable while this endpoint has exactly one field that can produce a `VALIDATION_ERROR` without one. **Revisit the moment a second appears.** The check itself is not pedantry: Phase 11 computes reminder day boundaries from this column, and `Europe/Bucuresti` looks entirely plausible until something tries to resolve it. |
| 2026-08-14 | `ChangePasswordRequest.currentPassword` carries no length bound, for the same reason `LoginRequest` does not: a length policy applies to **new** passwords. Enforcing today's twelve-character minimum on the existing one would refuse exactly the people whose password is too short and who are trying to fix it. |
| 2026-08-14 | **Changing the password ends every session, including the caller's own, and the endpoint answers 204 rather than a fresh token pair.** Section 14 requires this after a reset and the reasoning applies at least as strongly here: if the password is being changed because somebody else learned it, their sessions must not outlive it. Sparing the caller's own session would require the access token to carry its refresh-token family, which it deliberately does not — the token holds `iss`, `iat`, `exp`, `sub` and nothing more. 204 is the honest answer: log in again with the password you just chose. |
| 2026-08-14 | **`revokeAllSessionsOf` is the last statement in `changePassword`, and the ordering is load-bearing.** It is a `@Modifying(flushAutomatically = true, clearAutomatically = true)` bulk update: it flushes the new hash and then detaches every loaded entity, so anything touching `user` after that line would read or write a stale copy. This is the same trap as `invalidateOutstandingCodes` on 2026-08-13, met a second time in a different flow — which is why the constraint is written as a comment at the call site and not only here. |
| 2026-08-14 | **The `user` → `auth` package edge is accepted deliberately.** `UserService` injects `RefreshTokenService` while `auth` already depends on `user`, so in the modular monolith of section 4 this is a cycle between packages. The alternatives — an application event, or a third "session" package — buy indirection without buying independence, since the two modules are one deployable and one transaction. Recorded so that it is a decision rather than an accident. **Trigger to extract the seam: a third module needing to end sessions.** |
| 2026-08-14 | **An unused `@MockitoBean` can be a legitimate context-cache tool.** `UserProfileFlowTest` declares an `EmailProvider` mock it never touches, purely so its annotation stack matches `AuthFlowTest` exactly; `@MockitoBean` joins the Spring context cache key, so omitting it would have started a fifth context and a fifth PostgreSQL container for eight tests that need neither. Verified in the build log — the class prints no banner and creates no container. The field carries a comment saying why, because an unused mock otherwise reads as leftover debris and gets deleted by the next reader, silently costing several seconds on every build from then on. |

## Known issues and open decisions

### Deferred work with a scheduled phase

| Item | Phase |
|---|---|
| CSRF token protection for the cookie-authenticated `/auth/refresh` and `/auth/logout` paths, per specification section 14. Deferred deliberately on 2026-08-14, with reasoning in the decisions table: `SameSite=Strict` is the working defence today, and a token needs a real frontend and two real origins to be implemented or tested honestly. | 16 |
| **Standing rule, not a dated task:** `/api/v1/auth/**` is permitted wholesale, which is correct only while every endpoint under it is pre-authentication. All six are, today. Any endpoint added under that prefix which should require a token must be matched individually — check this at the moment of adding, not later. | standing |
| Registration sends the email inside the transaction, so a provider outage rolls the account back. Simple and safe today; revisit if Resend proves flaky. | 4.7 |
| ~~Auth endpoints are not rate limited~~ — **closed 2026-08-14** by `AuthRateLimit` over `InMemoryRateLimiter`. | done |
| **Release-blocking, not a refinement:** `AuthRateLimit` reads the caller's address from `request.getRemoteAddr()`. Correct locally; behind Railway's proxy it returns the *proxy's* address for everyone, which turns every per-address limit into a global one — `/refresh` at 60/hour would stop the application for all users within minutes. The fix is `server.forward-headers-strategy` in configuration, **never** hand-parsing `X-Forwarded-For`, which a client can forge and which would make per-address limiting bypassable in one line. Must be configured and verified against Railway's actual header behaviour before production. If that behaviour cannot be established with confidence, the safe fallback is to drop the per-address limit on `/refresh` alone: it costs one SHA-256 lookup, and reuse detection already punishes abuse by revoking the family. | 15 |
| `POST /users/me/change-password` is **not** rate limited, and it is the first endpoint that costs two Argon2 operations — one comparison and one encode. Deferred on the reasoning that a valid bearer token is itself a guard: the caller must already hold an unexpired access token for that exact account, and a wrong `currentPassword` changes nothing. That is weaker than it sounds if a token is ever stolen, which is precisely why it is written down rather than assumed. The policy to reuse already exists — `credentialCheck`. **Trigger: the first authenticated endpoint that costs a hash and is exposed without another guard**, or any evidence of grinding. | standing |
| `spring-boot-configuration-processor` only runs because `maven.compiler.proc=full` is set in `pom.xml`. **JDK 21 requires the option to be set explicitly**; without it the processor is silently skipped and no metadata is generated. | done |
| ~~`HttpStatusEntryPoint` returns a bare 401 with no body~~ — **closed 2026-08-14** by `ApiErrorAuthenticationEntryPoint`. Was scheduled for 4.4, slipped, and was caught by re-reading `SecurityConfig` rather than by any test. | done |
| No `AccessDeniedHandler`, so a 403 from Spring Security itself would still answer with a bare body. Not built yet because nothing can trigger it: every rule is `anyRequest().authenticated()` with no roles, and `VEHICLE_ACCESS_DENIED` and its relatives are `ApiException`s from services that already take the `GlobalExceptionHandler` path. Building it now would mean inventing an authorization rule to test it against. **Trigger: the first real authorization rule on the chain**, realistically Phase 7 with vehicle ownership. Both places that take an entry point take a handler too. | 7 |
| Surefire now sets `argLine` for the Mockito agent. JaCoCo also writes `argLine`, so when coverage is added the value must become `@{argLine} -javaagent:...` or one plugin will silently overwrite the other. | 14 |
| `eslint-plugin-jsx-a11y` not installed; required for the accessibility rules in specification section 36. | 5 |
| Node version not yet pinned in the repository. Add `.nvmrc` and `engines` so GitHub Actions and Cloudflare Pages resolve the same version. | 14 |

### Open decisions

- Mac availability for Phase 18 (iOS) not yet confirmed. Blocks nothing before Phase 17.
- Google Document AI processor version must be verified as currently supported at Phase 9; deliberately not frozen in advance.
- ~~Spring Security Argon2 encoder implementation~~ — **resolved 2026-08-13.** `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`, with the reasoning in the `ro.garajulmeu.security` section. Argon2 itself was always the frozen algorithm.

### Carried over from specification section 35

- Final product name, logo, colours and production domain.
- Normalised X/Y/width/height coordinates for certificate overlay fields — calibrated in Phase 8, never guessed.
- Final OCR confidence thresholds — calibrated against representative samples.
- Capacitor secure-storage plugin — selected during the mobile phase against current documentation.
- Final legal wording, retention periods and privacy notices before release.

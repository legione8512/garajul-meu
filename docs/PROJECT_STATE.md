# PROJECT_STATE

Portable state of the guided build. Updated at every meaningful milestone so the
project can be continued in a new conversation, or by a different AI assistant,
without relying on model memory.

Last updated: 2026-08-15

---

## Current position

| Item | Value |
|---|---|
| Phase | **4 — Authentication & Users: complete.** 5 — Frontend Foundation & i18n: **in progress**, 5.1 to 5.3 done |
| Last **committed** milestone | **Phase 5.1–5.2 — `7bdb35b`**, which also carried the specification-citation corrections. Those were meant to be a separate commit and were collapsed into one; noted so the history reads honestly |
| Applied, verified green, **not yet committed** | **Phase 5.3 — the API client**, which touches both halves. Backend: CORS (`CorsProperties`, `SecurityConfig`, both `application.yml` files, `CorsTest`), the `rate-limit` indentation fix, and the deletion of `additional-spring-configuration-metadata.json`. Frontend: `src/api/` and `src/auth/` |
| Commit history note | `ca3c670` is 4.5b. `6736ef8` is labelled as the Jackson 3 login fix but **also carries the whole authentication-entry-point change** — `ErrorCode`, `ApiErrorAuthenticationEntryPoint`, `SecurityConfig`, `AuthFlowTest`. Two commits were intended and collapsed into one. Left as it is: it is already on the public remote, and rewriting published history for an imprecise message is not worth it. Recorded here so the entry point can still be found |
| Verified build — backend | `.\mvnw.cmd clean verify` → **`Tests run: 111, Failures: 0, Errors: 0, Skipped: 0`**, `BUILD SUCCESS`, **four** PostgreSQL containers |
| Verified build — frontend | from `frontend/`: `npm run test:run` → **38 passed in 7 test files**; `npm run build`; `npm run lint`. **Count the files as well as the tests** — on 2026-08-14 a test file never reached disk, the suite stayed green, and only the total gave it away |
| Next verified step | **5.4 — the five pre-authentication screens**, where the pages stop being a single `<h1>`: Welcome, Create Account, Email Verification, Login, Forgot/Reset Password. This is where client-side form validation lands, and with it the deliberate duplication of the backend's bounds recorded in the decisions table. Section 6 sets the i18n contract — `ro` and `en`, no user-facing string hardcoded in a component, the pre-authentication language kept in a local non-sensitive preference and the authenticated one in `users.preferred_language`, and **the frontend translating the backend's stable error codes**. Section 5 lists twenty-two screens; **screens 1–5 — Welcome, Create Account, Email Verification, Login, Forgot/Reset Password — belong to Phase 5, agreed 2026-08-14.** The specification does not say so outright, but Phase 6 is the dashboard and garage skeleton, which presupposes an authenticated user. Agreed breakdown: **5.1 done** — routing, app shell, test harness; **5.2 done** — i18next; **5.3 next** — the API client, token handling and the 401 → refresh → retry loop; **5.4** — the five screens, which is where the pages stop being a single `<h1>` |

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
| Verification | Read the log, not just `BUILD SUCCESS`. **Always check the total test count, and on the frontend the test-file count too** — a test lost to a bad edit leaves the build green and the number smaller, and a whole file that never reached disk removes its entire block at once. Both have now happened, on 2026-08-13 and 2026-08-14. A milestone is not done until the predicted number is the observed number. |
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

**Every "section N" in this document refers to
`Garajul_Meu_Master_Technical_Specification_V1.1.docx`**, whose numbering was
read directly from the file and verified on 2026-08-14. The ones that matter
most here: 4 architecture, 5 screens, 6 internationalisation, 10 data
dictionary, 14 authentication, 17 error codes, 19 scheduler, 20 Neon, 21
hosting and cookies, 23 data residency, 24 privacy and deletion, 27 logging, 30
phase order, 32 provider abstractions, 35 deferred decisions, 36 definition of
V1 success. Several citations written before that check were wrong; do not
copy a section number from memory or from an older note.

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
account-management endpoints: `GET`, `PATCH` and `DELETE /api/v1/users/me`, and
`POST /me/change-password`, `/me/change-email` and `/me/confirm-email-change`.
That is the whole of Phase 4; see the package tables below.

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
| `UserService` | `profileOf`, `updateProfile`, `changePassword`, `deleteAccount`. Injects `PasswordEncoder` and `RefreshTokenService` — the deliberate `user` → `auth` edge recorded in the decisions table. `validZone` checks the timezone against `ZoneId.getAvailableZoneIds()`, because membership of a set resolved at runtime cannot be expressed as a Bean Validation annotation without a custom constraint. `deleteAccount` is a hard delete followed by an explicit `flush()`, so the foreign-key cascade runs inside the method rather than at commit |
| `UserController` | `GET /api/v1/users/me` — the first protected route; `PATCH /me` → 200 with the updated profile; `POST /me/change-password` → **204**; `POST /me/change-email` → **204** (nothing has changed yet, a code has been sent); `POST /me/confirm-email-change` → **200 with the profile**, because the account has just changed in two ways at once and the client must react to both; `DELETE /me` → **204**, carrying a body and clearing the refresh cookie on the way out. For all six the identity comes from the verified token's `sub`, never from a path or query parameter, which is what makes another account unreachable by editing a URL. Injects `UserService`, `AuthService` — the email change is a verification-code flow and belongs to `auth`, while the URL belongs to the account — and `RefreshCookies`, solely to clear the cookie on deletion |
| `dto.UserProfileResponse` | Response DTO rather than the entity, so `passwordHash` is never one Jackson change away from the wire |
| `dto.UpdateProfileRequest` | Partial update: every component optional, absent means unchanged. Deliberately carries **no** `@NotBlank` — that would forbid absence, which is the whole point of PATCH |
| `dto.ChangePasswordRequest` | `currentPassword` `@NotBlank` with no length bound; `newPassword` 12–128, the same bound as registration and reset |
| `dto.ChangeEmailRequest` | `newEmail` `@Email @Size(max = 320)`, matching the column; `currentPassword` with no length bound, for the same reason as above |
| `dto.ConfirmEmailChangeRequest` | The code alone, `@Pattern("\\d{6}")`. The requested address is **not** repeated here — it rides on the token, so a confirmation cannot name a different address from the one the owner was shown |
| `dto.DeleteAccountRequest` | The current password alone. Of the three endpoints that ask for it this is the one that most needs it: no support process can restore what this removes |

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
| `AuthService` | `register`, `verifyEmail`, `resendVerificationCode`, `forgotPassword`, `resetPassword`, `login`, `refresh`, `logout`, and — although the URLs live under `/users/me` — **`requestEmailChange(accountId, newEmail, currentPassword)`** and **`confirmEmailChange(accountId, code)`**, because the single implementation of the code check is private here. Login returns `LoginResult`; the controller decides how the token travels. Two private helpers carry the shared machinery: `issueCode(user, type)` supersedes any outstanding code and returns a new one, with a three-argument overload that attaches a `targetValue` for the one type that needs it; and **`consumeCode(userId, type, code, now)` is the single place a six-digit code is checked** — spent, expired, too many attempts, wrong. Three flows need the identical sequence, and writing it three times is how three flows end up with quietly different rules. It **returns the spent token**, so email change can read the address off it. The type is always part of the lookup, so a code issued for one purpose cannot open another |
| `AuthController` | `POST /api/v1/auth/register` → 201; `/verify-email` and `/resend-verification` → 204; `/forgot-password` and `/reset-password` → 204; `/login` → 200 with the access token and the refresh cookie; `/refresh` → 200 on the channel the caller used; `/logout` → 204, idempotent |
| `dto.ForgotPasswordRequest` / `dto.ResetPasswordRequest` | The reset pair. The code is `@Pattern("\\d{6}")`, so malformed input is rejected before it costs an Argon2 comparison, and the new password carries the same 12–128 bound as registration — a length policy applies to new passwords, and this is one |
| `dto.VerifyEmailRequest` | `@Pattern("\\d{6}")` on the code, so malformed input is rejected before it costs an Argon2 comparison |
| `dto.RegisterRequest` | Bean Validation constraints; password 12–128 characters; language is a `@Pattern("ro\|en")` **string**, not the enum, because JSON carries the lower-case tag while Jackson would expect the constant name |
| `dto.LoginRequest` | `refreshTokenInBody` is a boxed `Boolean`, not a primitive — see the Jackson 3 decision below. `wantsRefreshTokenInBody()` is the single place that decides absent means false |
| `dto.RefreshRequest` / `dto.RefreshResponse` | The explicit-token channel used by native clients. `RefreshResponse.refreshToken` is null when the caller arrived by cookie |
| `AuthRateLimit` | Applies the policies in the controller, where the network address lives, and turns an exhausted budget into `RATE_LIMITED`. **Two keys per request**, address and email address, because either alone is avoidable: address-only punishes a whole NAT for one person, email-only lets an attacker rotate addresses and never spend a budget. The network address is checked **first**, and an exhausted one returns before the email key is ever created — that ordering is what bounds the limiter's memory. Reuses `AuthService.normalise`, now package-private, so that `A@B.com` and `a@b.com` cannot hold separate budgets |

#### Package `ro.garajulmeu.email`

`EmailProvider` (specification section 32) with `sendVerificationCode` and
`sendPasswordResetCode`, both `(recipient, code, language)`, and
`sendEmailChangeCode(recipient, newEmail, code, language)`. Separate methods
rather than one with a purpose argument: the templates say different things, and
a reader who receives the wrong wording learns something false about their
account. The email-change method takes **four** arguments because its recipient
and its subject are two different addresses — the code goes to the address on
file, and naming the requested one in the body is what lets somebody who did not
ask for the change recognise it and refuse.
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

`CorsProperties` (`garajul-meu.cors.allowed-origins`) holds the browser origins
allowed to call the API, per section 21. Exact origins only — the CORS
specification forbids pairing a wildcard with credentials, and every request
this API answers is credentialed. An empty list is valid and safe: no browser
origin is permitted and native clients, which CORS does not bind, still work.

`SecurityConfig` registers the matching `CorsConfigurationSource` on `/api/**`
and enables `.cors(...)` **first** in the chain, so a preflight — which by
definition carries no `Authorization` header — is answered rather than refused
with a 401 the browser would report as a CORS failure. It exposes
`X-Request-Id`, which is not decoration: a browser hides every cross-origin
response header from JavaScript bar a short standard set, so without that line
the correlation id never reaches the frontend and the request id on an error
screen would always be blank, wasting the whole mechanism.

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
| `npm run test:run` | Vitest 4.1.10 over jsdom 30.0.1, with Testing Library 16.3.2 |
| Routing | react-router **8.3.0**, declarative API |
| i18n | i18next **26.3.6** with react-i18next **17.0.11** |

The Vite starter page is gone. Structure:

| Path | Role |
|---|---|
| `src/routes/paths.ts` | Every URL named once, so a route and the links pointing at it cannot drift apart by a typo |
| `src/routes/AppRoutes.tsx` | The route table, **deliberately separate from the Router**. `App.tsx` wraps it in a `BrowserRouter` that reads the real address bar; tests wrap the same table in a `MemoryRouter` and hand it a path, so every route is reachable without navigating through the UI |
| `src/layouts/RootLayout.tsx` | One `<header>`, one `<main>`, and it is a **layout route with no path**, so it wraps the not-found page too — which is exactly where somebody lost needs a way home |
| `src/pages/` | Seven pages, each currently a single translated `<h1>`. Screens 1–5 of section 5 plus not-found; the content arrives in 5.4 |
| `src/i18n/language.ts` | Pure functions: what was remembered, what the browser asks for, what wins. No detector plugin — see the decisions table |
| `src/i18n/locales/ro.ts` | The reference locale. `en.ts` is typed as `typeof ro`, so a forgotten key is a compile error rather than a string that renders in the wrong language |
| `src/i18n/i18next.d.ts` | Augments `CustomTypeOptions`, so a mistyped translation key stops the build instead of rendering itself to the reader |
| `src/i18n/errorKey.ts` | Backend error code → translation key, with an explicit unknown branch. The frontend half of section 17 |
| `src/components/LanguageSwitcher.tsx` | Label translated, option names **not** — each language named in itself |
| `src/api/tokenStore.ts` | The access token in a module variable, never in storage, plus a subscription so the session ending in the background is noticed rather than discovered on the next click |
| `src/api/refresh.ts` | **One refresh in flight, shared by every caller.** The module exists for that single variable |
| `src/api/client.ts` | `apiFetch`: bearer header, `credentials: 'include'`, one refresh and one retry on 401, and every failure turned into `ApiError` |
| `src/api/ApiError.ts` | `code`, `status`, `requestId`, `fieldErrors`. No message from the server, because the server sends none |
| `src/api/endpoints/` | `auth.ts` — `login`, `logout`; `users.ts` — `getProfile`. The rest arrive in 5.4 with the screens that call them, so nothing sits untested |
| `src/auth/` | `AuthContext.ts`, `AuthProvider.tsx`, `useAuth.ts`. Three-state status, silent restore on load, and the account's language adopted on sign-in |

No design system, no CSS beyond the untouched `index.css`. Nothing in section 30
places one in Phase 5, and inventing tokens before the first real screen exists
would be guessing at what they have to support.

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
| `EmailChangeFlowTest` (9 tests) | The email change end to end. **The code is sent to the old address and names the new one**, and never to the new address — the property the whole design rests on, asserted positively and negatively. Nothing changes until the code is confirmed. Confirming moves the address and leaves it **unverified**, which is the honest consequence of a code that proved control of the *old* inbox only. The full round trip then works: confirm, receive a fresh verification code at the new address, verify, log in with it. **The session survives the change**, verified by refreshing a token issued beforehand — that is the only route back from a mistyped address, so it is asserted rather than assumed. A wrong `currentPassword` answers `INVALID_CURRENT_PASSWORD` and sends no email at all; an address somebody else already holds answers `EMAIL_ALREADY_EXISTS`; a code of another type cannot confirm a change; both endpoints answer 401 without a token |
| `AccountDeletionTest` (9 tests) | Deletion, and what it must not leave behind. The account row goes; **every verification code and refresh token goes with it**, counted in the database rather than assumed, because neither child is mapped as an association and the foreign key is the only thing that removes them. **The address is free for a new registration afterwards** — the test that would catch the day somebody turns this into a soft delete or an anonymised tombstone, since either would keep `ux_users_email` occupied. A wrong password deletes nothing; the refresh token stops working at once; **the access token outlives the account and opens nothing**, answering `USER_NOT_FOUND`, which is the honest consequence of a stateless JWT that cannot be recalled; the response clears the refresh cookie; deleting one account leaves another intact; and a caller with no token is refused |
| `CorsTest` (3 tests) | A preflight from the application origin is allowed to carry credentials; one from anywhere else is refused; and **an unauthenticated API call still carries the CORS headers and exposes `X-Request-Id`** — asserted on a response that fails, deliberately, because without those headers on error responses a browser hides the body and the frontend could not read the code or the request id from precisely the answers that need explaining |

Four test classes replace `EmailProvider` with `@MockitoBean` — `AuthServiceTest`,
`PasswordResetServiceTest`, `AuthFlowTest` and `EmailChangeFlowTest` — because
it is the only way to read a code that is stored as an Argon2 hash and therefore
irreversible by design. Each *distinct* context configuration buys a separate
Spring context and PostgreSQL container, so the build starts **four**:
`AuthFlowTest`, `AuthRateLimitTest` (which overrides the rate limit properties),
`AuthServiceTest`, and `RefreshTokenServiceTest`. Four classes reuse an existing
context rather than adding a fifth container: `PasswordResetServiceTest` matches
`AuthServiceTest`, while `UserProfileFlowTest`, `EmailChangeFlowTest` and
`AccountDeletionTest` match `AuthFlowTest`. Two of those — `UserProfileFlowTest`
and `AccountDeletionTest` — do so **with an `EmailProvider` mock they never
use**, because `@MockitoBean` joins the cache key through
`BeanOverrideContextCustomizer` and dropping it would have split the context.
All four run in about a second with no container of their own, and the build
log proves it: none prints a Spring banner. **Matching an existing class's
annotations exactly is what keeps the container count from growing with every
new test class**, and an unused `@MockitoBean` is a legitimate way to match —
documented in the test as intentional, or the next reader deletes it as debris.

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

### Frontend tests — 38 in 7 files

| File | Scope |
|---|---|
| `App.test.tsx` (8) | Each of the six routes renders its own page, an unregistered address falls back to not-found, and the shell with its way home survives even there. Asserted against the Romanian resource rather than literals, so rewording a title stays a one-file change |
| `i18n/language.test.ts` (5) | A remembered language is read back; a stored value that is not a supported language is ignored; the remembered choice outranks the browser; the browser's first supported language wins otherwise; Romanian is the fallback. All pure — the candidate list is a parameter, so nothing rewrites a global the whole environment shares |
| `i18n/errorKey.test.ts` (3) | A known code maps to its key, an unknown one to the generic message, and **every code the authentication surface can send has wording** — if one of those ever resolved to UNKNOWN, a real failure would be shown as "something went wrong" while the backend had said exactly what happened |
| `components/LanguageSwitcher.test.tsx` (3) | Both languages named in themselves; switching re-renders the page in the chosen language; the choice is written to storage |
| `api/refresh.test.ts` (5) | The new token is stored; **one request no matter how many callers ask at once** — the test that justifies the module; a later expiry may refresh again rather than reusing the first result; a refused refresh clears the token; so does an outright network failure |
| `api/client.test.ts` (8) | The bearer header appears only when there is a token; a 401 refreshes once and retries once; **two simultaneous 401s produce exactly one refresh**; a 401 from `/auth/login` refreshes nothing; a failure becomes `ApiError` with the code and request id intact; a response with no JSON at all still becomes one; a 204 parses to nothing |
| `auth/AuthProvider.test.tsx` (6) | A session the cookie still supports is restored; with none, the status settles on anonymous; **the status is `unknown` until the attempt settles**; the account's language is adopted; signing in loads the profile; signing out returns to anonymous |

`src/test/setup.ts` pins the language to Romanian before every test and clears
storage afterwards. jsdom reports `en-US`, so without that pin every component
test would run in English and a Romanian assertion would fail for a reason that
has nothing to do with the component under test.

Playwright and end-to-end coverage arrive with Phase 14.

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
| 2026-08-12 | Frontend uses ESLint rather than the create-vite 9 default of Oxlint, so that `eslint-plugin-jsx-a11y` is available. **Corrected 2026-08-14:** this was recorded as satisfying "the accessibility requirement in specification section 36", and no such requirement exists — section 36 is the definition of V1 success. The specification says almost nothing about accessibility; its only line is in section 7, that colour is never the sole accessibility signal for certificate field states. The tooling choice still stands on its own merits, but it is ours rather than mandated. |
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
| 2026-08-13 | `ExceptionHandlerExceptionResolver` is pinned to ERROR level. At its default WARN it logs every resolved exception including rejected field values, which put an attempted password into the log during a manual registration test — forbidden by specification section 27, which names passwords, tokens and verification codes explicitly. Our own handler logs field names only. Found by reading the log during manual verification; no automated test asserts on framework logging, so this class of leak needs a human eye. |
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
| 2026-08-14 | Configuration metadata for `@ConfigurationProperties` records comes from the annotation processor, which emits correctly prefixed and correctly typed entries into `spring-configuration-metadata.json`. **Never accept Eclipse's "create metadata" quick fix.** On 2026-08-14 it wrote six entries into `additional-spring-configuration-metadata.json` missing the `garajul-meu.` prefix and typed `java.lang.String` instead of `Integer` and `Duration`; the two files merge, so the result was six phantom keys binding to nothing. **It happened again on 2026-08-15**, and that time the fabricated metadata produced a false error: `garajul-meu.cors.allowed-origins` was declared `java.lang.String`, so Eclipse reported "Expecting a 'String' but got a 'Sequence'" against a list that was entirely correct. The trap has a shape worth recognising — **writing the YAML before the properties class exists** is what makes Eclipse offer the quick fix at all. Write the record first. The same file also held five entries for `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_MIGRATION_URL` and `JWT_SECRET`, which are `${}` placeholder names resolved from the environment and not configuration properties of this application at all. The whole file was deleted; nothing in this project needs it. |
| 2026-08-14 | **There are two paths to a 401, and `exceptionHandling()` governs only one.** A request with *no* token is refused later by `AuthorizationFilter` and handled by `ExceptionTranslationFilter`, which uses the global entry point. A request whose bearer token *fails to decode* is refused by `BearerTokenAuthenticationFilter`, which calls **its own** entry point on the spot and never reaches `ExceptionTranslationFilter`. The custom entry point must therefore be set on `oauth2ResourceServer(...)` as well as on `exceptionHandling(...)`; neither replaces the other. Wiring only the first left forged tokens on Spring's default, and `AuthFlowTest` caught it. |
| 2026-08-14 | Replacing the resource server's default entry point also removes its `WWW-Authenticate` header, which is a gain rather than a loss. That header carried `error_description="An error occurred while attempting to decode the Jwt: ..."` — English prose the frontend must own per section 6 — and `resource_metadata` naming the server host, which is internal detail a response has no place to carry: section 17 gives a failure one stable code and nothing else. RFC 6750 recommends the header; our only client reads the JSON code, so the recommendation buys nothing and costs a leak. |
| 2026-08-14 | **CSRF stays disabled through Phase 15; the token mechanism arrives at Phase 16.** The refresh cookie is `SameSite=Strict`, `HttpOnly`, `Secure` and path-scoped to `/api/v1/auth`, so a browser attaches it to no cross-site request at all — that is the primary defence against exactly the attack CSRF tokens prevent, not a secondary one. A CSRF token on top is genuine defence in depth, but it needs a frontend that reads and replays it, and that frontend does not exist yet; nor can it be tested convincingly on localhost, where there are no two real origins. It lands with `app.<domain>` and `api.<domain>`. Residual risk accepted until then: an XSS or subdomain takeover on any same-site host could issue same-site requests, which a token would still block. |
| 2026-08-14 | **PATCH means absent-is-unchanged, and that decides the validation.** `UpdateProfileRequest` carries no `@NotBlank`: it rejects an absent value, which would turn PATCH into PUT. `@Pattern(".*\\S.*")` does the work `@NotBlank` would have done without forbidding absence — Bean Validation skips a null, so only a value that is actually present has to contain a non-whitespace character. |
| 2026-08-14 | The timezone is checked in the service rather than declaratively. Membership of `ZoneId.getAvailableZoneIds()` cannot be expressed as an annotation without a custom constraint, and the price of not writing one is that the response carries no field name — acceptable while this endpoint has exactly one field that can produce a `VALIDATION_ERROR` without one. **Revisit the moment a second appears.** The check itself is not pedantry: Phase 11 computes reminder day boundaries from this column, and `Europe/Bucuresti` looks entirely plausible until something tries to resolve it. |
| 2026-08-14 | `ChangePasswordRequest.currentPassword` carries no length bound, for the same reason `LoginRequest` does not: a length policy applies to **new** passwords. Enforcing today's twelve-character minimum on the existing one would refuse exactly the people whose password is too short and who are trying to fix it. |
| 2026-08-14 | **Changing the password ends every session, including the caller's own, and the endpoint answers 204 rather than a fresh token pair.** Section 14 requires this after a reset and the reasoning applies at least as strongly here: if the password is being changed because somebody else learned it, their sessions must not outlive it. Sparing the caller's own session would require the access token to carry its refresh-token family, which it deliberately does not — the token holds `iss`, `iat`, `exp`, `sub` and nothing more. 204 is the honest answer: log in again with the password you just chose. |
| 2026-08-14 | **`revokeAllSessionsOf` is the last statement in `changePassword`, and the ordering is load-bearing.** It is a `@Modifying(flushAutomatically = true, clearAutomatically = true)` bulk update: it flushes the new hash and then detaches every loaded entity, so anything touching `user` after that line would read or write a stale copy. This is the same trap as `invalidateOutstandingCodes` on 2026-08-13, met a second time in a different flow — which is why the constraint is written as a comment at the call site and not only here. |
| 2026-08-14 | **The `user` → `auth` package edge is accepted deliberately.** `UserService` injects `RefreshTokenService` while `auth` already depends on `user`, so in the modular monolith of section 4 this is a cycle between packages. The alternatives — an application event, or a third "session" package — buy indirection without buying independence, since the two modules are one deployable and one transaction. Recorded so that it is a decision rather than an accident. **Trigger to extract the seam: a third module needing to end sessions.** |
| 2026-08-14 | **An unused `@MockitoBean` can be a legitimate context-cache tool.** `UserProfileFlowTest` declares an `EmailProvider` mock it never touches, purely so its annotation stack matches `AuthFlowTest` exactly; `@MockitoBean` joins the Spring context cache key, so omitting it would have started a fifth context and a fifth PostgreSQL container for eight tests that need neither. Verified in the build log — the class prints no banner and creates no container. The field carries a comment saying why, because an unused mock otherwise reads as leftover debris and gets deleted by the next reader, silently costing several seconds on every build from then on. |
| 2026-08-14 | **The email change code goes only to the address already on the account, never to the requested one.** That single choice is what makes a stolen access token insufficient to hijack an account: the thief can provoke the request, but the answer lands in an inbox they do not hold. It also makes the message useful to the person who did *not* ask — the requested address is named in the body, so an unexpected change is recognisable and refusable rather than merely alarming. |
| 2026-08-14 | **Confirming the change resets `email_verified_at` to null and issues a fresh verification code to the new address.** The code proved control of the old inbox, which is what authorises the move, and proved nothing whatever about the new one. The alternative — marking the new address verified — would record a claim nobody checked, and a mistyped address would then sit permanently "verified" while silently receiving nothing, including the Phase 11 reminders this application exists to send. A visible unverified state is a recoverable error; a silent one is not. |
| 2026-08-14 | **An email change deliberately does not revoke sessions**, unlike a password change or reset. No credential changed, so there is nothing to outrun. More importantly the live session is the *only* route back from a mistyped address: login refuses unverified accounts and `forgot-password` writes to the account address, so revoking here would convert a typo into an unrecoverable account. **Residual risk accepted knowingly:** a user who mistypes and then loses the session is still locked out. Closing that gap needs either a confirmation step at the new address — rejected in favour of a single code at the old one — or an operator-side recovery path, which does not exist yet. |
| 2026-08-14 | The requested address rides on the token in `target_value` rather than being sent again at confirmation time. Asking the client to repeat it would let the confirmation name a different address from the one the owner was shown and approved in the email — precisely the substitution this flow exists to prevent. |
| 2026-08-14 | `consumeCode` now **returns the spent token** rather than `void`, and `issueCode` gained a three-argument overload carrying `targetValue`. Both were preferred to giving email change its own copy of the code check, per the 2026-08-13 decision that one security-sensitive sequence written twice is how two flows quietly drift apart — now three flows. The change touched working code, and the ninety existing tests were the net that made it safe. |
| 2026-08-14 | **Account deletion is a hard delete.** No soft delete, no anonymised tombstone row: specification section 24 says permanent, and the address becomes free again immediately. That is both what "delete my account" means to the person asking and what a data protection request means legally. `theAddressIsFreeForANewAccountAfterwards` is the test that would catch the day somebody decides deletion should merely hide the account — a tombstone would keep `ux_users_email` occupied and the re-registration would answer `EMAIL_ALREADY_EXISTS`. |
| 2026-08-14 | **The foreign key is the only thing that removes a deleted account's child rows.** Neither `VerificationToken` nor `RefreshToken` is mapped as an association — both hold a plain `userId` — so JPA cascades nothing, and `ON DELETE CASCADE` in `V2` and `V3` does all the work. A future migration that dropped it would leave orphans in complete silence, which is why the test counts the rows in the database afterwards rather than trusting the mechanism. `deleteAccount` also calls `flush()` explicitly, so the cascade runs inside the method and a broken constraint fails on that line instead of inside Spring's commit machinery. |
| 2026-08-14 | **A deleted account's access token stays cryptographically valid until it expires**, because a stateless JWT cannot be recalled. It opens nothing: every route resolves the account first and finds none, so the answer is `USER_NOT_FOUND`. Recorded and tested rather than left to be discovered, since the alternative — a revocation list checked on every request — would trade the entire benefit of stateless verification for at most fifteen minutes of exposure on an account that no longer holds any data. |
| 2026-08-14 | `DELETE /users/me` carries a request body, which HTTP permits but does not require. The password has to travel somewhere, and a query parameter would put it in access logs and browser history — the two places a password must never be. See the deferred-work table for the proxy risk this creates. |
| 2026-08-14 | The two email-change endpoints live on `UserController` but call `AuthService`, so the controller injects two services. The URL belongs to the account; the flow is a verification-code flow and belongs to `auth`, where the single implementation of the check is private. A controller coordinating two services is a smaller price than either a second copy of that check or a pass-through method on `UserService` that would add a layer without adding meaning. |
| 2026-08-15 | **The route table is separate from the Router that drives it.** `BrowserRouter` reads the real address bar and cannot be told to start anywhere else, so `App.tsx` holds only the Router and `AppRoutes.tsx` holds everything testable. Tests wrap the same table in a `MemoryRouter` with an initial path, which makes every route reachable directly instead of by navigating the UI to get there. `RootLayout` is a pathless layout route so it wraps the not-found page too. |
| 2026-08-15 | **`i18next-browser-languagedetector` was rejected.** The policy needed is three rules — the remembered choice, then the browser, then Romanian — and hand-written they are about twenty **pure** functions' worth of code, with the candidate list passed as a parameter that defaults to `navigator`. The plugin would have made detection a one-time event at initialisation, testable only by rewriting a global the whole environment shares. Same reasoning as the hand-written rate limiter: one narrow policy, fully testable, no dependency. |
| 2026-08-15 | **Two compile-time guards replace a class of silent translation bugs.** `en.ts` is typed `typeof ro`, so a key added to Romanian and forgotten in English fails the build rather than rendering in the wrong language; and `i18next.d.ts` augments `CustomTypeOptions`, so a mistyped key in `t()` fails the build rather than rendering itself to the reader. Romanian is the reference locale because it is the fallback and the backend's default. |
| 2026-08-15 | **Language names are never translated.** `languageNames` lives in `language.ts`, not in the resource files, because somebody who has landed in a language they cannot read must still recognise their own in the list — which they cannot do if "Romanian" is rendered as a Romanian word. The switcher's label is translated; its options are not. |
| 2026-08-15 | **`errorMessageKey` returns a key, not a sentence.** The mapping from a backend code to a translation key is the logic worth testing; translating is i18next's job. Keeping it a plain function rather than a hook keeps it free of React and trivially testable. The unknown branch is the point of the whole module: the backend's catalogue grows every phase, and a code this frontend has never seen must never reach a reader as the literal text `errors.SOMETHING_NEW`. Wording exists only for codes the authentication and account surface can actually raise; inventing Romanian copy for OCR failures that no screen can yet produce would be wording nobody has checked. |
| 2026-08-15 | **A delivered test file silently failed to reach disk.** `i18n/language.test.ts` was never created; the suite reported 11 passed and green, and five assertions simply did not exist. Caught only by comparing the total against the predicted 16. This is the second time paste loss has cost this project real coverage, and it is why the verification rule now says **count the test files as well as the tests** — a missing file removes its whole block at once, which no per-test number reveals. |
| 2026-08-15 | **`rate-limit` was at the wrong nesting level in `application.yml` and bound nothing.** It sat at column zero, a sibling of `garajul-meu` rather than a child, so `RateLimitProperties` fell back to the defaults in its compact constructor — which happen to be **exactly the values written in the file**, 10/15m, 5/1h, 60/1h. Nothing behaved wrongly, and that is what makes it dangerous: anyone tuning those limits in production would have seen no effect at all, silently, and found out mid-attack. Fixed by indenting. **The test suite cannot catch this class of fault**: `src/test/resources/application.yml` deliberately shadows the main file, so no test ever reads its values. A real blind spot, recorded rather than papered over. |
| 2026-08-15 | **CORS was configured now rather than deferred behind a Vite dev proxy.** The proxy would have made the frontend same-origin in development and cost nothing on the backend, but it hides preflight, `credentials: 'include'` and the allowlist until deployment day. This project already carries one "verify against the real proxy at Phase 15" debt — `X-Forwarded-For` — and that one is there because it *cannot* be tested locally. This one can, so it was. |
| 2026-08-15 | **The access token lives in a module variable and never in `localStorage` or `sessionStorage`.** Any XSS can read storage, which is the entire reason the refresh token is an HttpOnly cookie; storing the token that opens every endpoint beside it in plain text would give back exactly what that design bought. The cost is that a reload loses it, which is why the session is restored by a silent refresh at startup rather than read back. |
| 2026-08-15 | **All refreshes share one in-flight promise.** The backend rotates refresh tokens and treats a spent one as theft, revoking the whole family. Two panels expiring together would each call `/auth/refresh`, the second still carrying the token the first had just spent, and the user would be signed out of every device for doing nothing but loading a page with two panels on it. One shared promise means exactly one request reaches the server however many were waiting. It also makes React StrictMode's deliberate double-invocation harmless for free. |
| 2026-08-15 | Refresh is attempted **once** and the request retried **once**, and never at all for paths under `/api/v1/auth/`. A 401 from `/auth/login` means the password was wrong, not that a token expired; refreshing there would be nonsense and would spend the 60-per-hour budget. A second refresh would be arguing with a server that has already refused the first. |
| 2026-08-15 | **Authentication status has three values, and the provider does not withhold its children.** `unknown` covers the gap between load and the silent refresh answering; collapsing it into `anonymous` would show a signed-in person the signed-out view and correct itself a moment later. Blanking the tree during that moment was rejected as the opposite mistake — the public pages are perfectly renderable, so it would trade a brief wrong view for a brief empty one. Gating belongs to the routes that need protecting, which is Phase 6. |
| 2026-08-15 | **Server-side field errors get generic wording; precision belongs to client-side validation.** The backend sends `{field, constraint}` with no parameters, so `Size` arrives without its bounds. Rather than invent a per-field override table, the registration form in 5.4 will enforce and explain the rule before the request is sent — which it must know anyway to validate — and the server's `fieldErrors` become the backstop for a stale client or a direct API call. **Accepted duplication:** the numeric bounds will exist in both halves with nothing linking them. Changing a bound on the backend means changing a form rule by hand. Adding the constraint's parameters to `ApiErrorResponse` would remove the duplication and was rejected as a contract change for messages users should rarely see; **trigger to revisit: evidence that server-side field errors are commonly reaching users.** |

## Known issues and open decisions

### Deferred work with a scheduled phase

| Item | Phase |
|---|---|
| CSRF token protection for the cookie-authenticated `/auth/refresh` and `/auth/logout` paths, per specification section 14. Deferred deliberately on 2026-08-14, with reasoning in the decisions table: `SameSite=Strict` is the working defence today, and a token needs a real frontend and two real origins to be implemented or tested honestly. | 16 |
| **Standing rule, not a dated task:** `/api/v1/auth/**` is permitted wholesale, which is correct only while every endpoint under it is pre-authentication. All six are, today. Any endpoint added under that prefix which should require a token must be matched individually — check this at the moment of adding, not later. | standing |
| Registration sends the email inside the transaction, so a provider outage rolls the account back. Simple and safe today; revisit if Resend proves flaky. | 4.7 |
| ~~Auth endpoints are not rate limited~~ — **closed 2026-08-14** by `AuthRateLimit` over `InMemoryRateLimiter`. | done |
| **Release-blocking, not a refinement:** `AuthRateLimit` reads the caller's address from `request.getRemoteAddr()`. Correct locally; behind Railway's proxy it returns the *proxy's* address for everyone, which turns every per-address limit into a global one — `/refresh` at 60/hour would stop the application for all users within minutes. The fix is `server.forward-headers-strategy` in configuration, **never** hand-parsing `X-Forwarded-For`, which a client can forge and which would make per-address limiting bypassable in one line. Must be configured and verified against Railway's actual header behaviour before production. If that behaviour cannot be established with confidence, the safe fallback is to drop the per-address limit on `/refresh` alone: it costs one SHA-256 lookup, and reuse detection already punishes abuse by revoking the family. | 15 |
| `DELETE /users/me` sends the current password in a **request body on a DELETE**. Legal per RFC 9110, which defines no semantics for it but does not forbid it, and it works locally through Tomcat. Some proxies and CDNs strip DELETE bodies, and if Railway or Cloudflare does, the endpoint fails in production with a validation error on a field the client did send — a confusing failure far from its cause. **Verify against the real deployment before release.** If the body does not survive, the fallbacks are a dedicated header or `POST /me/delete`; a query parameter is not one, because it would put the password in access logs. | 15 |
| **CORS has never been exercised by a real browser.** `CorsTest` drives MockMvc, which speaks HTTP but enforces no same-origin policy, and every frontend test replaces `fetch` outright. So preflight, `credentials: 'include'` and the refresh cookie crossing an origin boundary are all still unproven. The check costs minutes: start the backend, `npm run dev`, open `http://localhost:5173`, and watch the startup `POST /api/v1/auth/refresh` in the browser console — a clean 401 with a JSON body means the chain works; a CORS error means it does not, and 111 green tests would not have said so. **Do this before 5.4 builds forms on top of it.** | 5.4 |
| **No endpoint under `/users/me` is rate limited**, and three of them now cost real work: `change-password` pays two Argon2 operations (a comparison and an encode), `change-email` pays a comparison **and sends an email to a real inbox**, and `confirm-email-change` pays a comparison. Deferred on the reasoning that a valid bearer token is itself a guard — the caller must already hold an unexpired access token for that exact account — and, for the email change specifically, that the code goes to an address the caller may not control, so grinding buys nothing. That is weaker than it sounds if a token is ever stolen: a thief could still spend the account's mail reputation by requesting changes in a loop. The policies to reuse already exist — `credentialCheck` and `emailDispatch` — and the key would be the account id rather than the network address. **Trigger: any authenticated endpoint that sends mail or costs a hash going live without another guard**, or the first evidence of grinding. | standing |
| `spring-boot-configuration-processor` only runs because `maven.compiler.proc=full` is set in `pom.xml`. **JDK 21 requires the option to be set explicitly**; without it the processor is silently skipped and no metadata is generated. | done |
| ~~`HttpStatusEntryPoint` returns a bare 401 with no body~~ — **closed 2026-08-14** by `ApiErrorAuthenticationEntryPoint`. Was scheduled for 4.4, slipped, and was caught by re-reading `SecurityConfig` rather than by any test. | done |
| No `AccessDeniedHandler`, so a 403 from Spring Security itself would still answer with a bare body. Not built yet because nothing can trigger it: every rule is `anyRequest().authenticated()` with no roles, and `VEHICLE_ACCESS_DENIED` and its relatives are `ApiException`s from services that already take the `GlobalExceptionHandler` path. Building it now would mean inventing an authorization rule to test it against. **Trigger: the first real authorization rule on the chain**, realistically Phase 7 with vehicle ownership. Both places that take an entry point take a handler too. | 7 |
| Surefire now sets `argLine` for the Mockito agent. JaCoCo also writes `argLine`, so when coverage is added the value must become `@{argLine} -javaagent:...` or one plugin will silently overwrite the other. | 14 |
| `eslint-plugin-jsx-a11y` **cannot be installed yet.** Verified against the registry on 2026-08-14: the latest release is 6.10.2 and its peer range is `eslint ^3 \|\| … \|\| ^9`, while this project runs **ESLint 10.8.0**. There is no newer version and no prerelease tag — only a `v5-backport` line. Forcing it past the peer check was rejected: a lint plugin running outside its supported range is the kind of thing that appears to work and silently stops reporting. **Trigger: a jsx-a11y release whose peer range includes ESLint 10.** If that does not arrive by the time the frontend has real forms, revisit the 2026-08-12 choice of ESLint over Oxlint, because the plugin was its entire justification. | 5 |
| ~~Five specification citations in the Java source cite the wrong sections~~ — **closed 2026-08-15.** `EmailProvider` said 22 for the RO/EN email templates (it is 6), `LoggingEmailProvider` said 30 for the ban on logging codes (it is 27), and `GlobalExceptionHandler`, `SecurityConfig` and `GlobalExceptionHandlerTest` all cited 30 for "internal detail must not reach the client" — a rule section 30 does not contain, being the development phase order. Corrected in comments only; no behaviour changed. Found by reading the specification directly rather than trusting the notes, which is the only way this class of error surfaces. | done |
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

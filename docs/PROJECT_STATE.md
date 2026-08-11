# PROJECT_STATE

Portable state of the guided build. Updated at every meaningful milestone so the
project can be continued in a new conversation, or by a different AI assistant,
without relying on model memory.

Last updated: 2026-08-12

---

## Current position

| Item | Value |
|---|---|
| Phase | 1 — Repository & Project Skeleton |
| Milestone | 1.1 repository foundation created; skeletons not yet generated |
| Next verified step | 1.2 — generate the Spring Boot backend skeleton under `backend/` |

## Project paths

| What | Path |
|---|---|
| Repository root | `D:\Learning\Claude_Space\Garajul Meu` |
| Backend (Eclipse) | `backend/` — not yet created |
| Frontend (VS Code) | `frontend/` — not yet created |
| Eclipse workspace | outside the repository (must never be inside it) |
| Specification source | `C:\Drive\UNI\Brunel University London\Personal Projects\Garajul Meu\` |

## Installed tool versions (verified 2026-08-12)

| Tool | Version |
|---|---|
| OS | Windows 11 Pro 10.0.26100 |
| JDK | Eclipse Adoptium 21.0.10 LTS (`JAVA_HOME` set) |
| Maven | 3.9.16 — runs on Java 21, platform encoding UTF-8 |
| Node.js | 22.19.0 |
| npm | 11.12.1 |
| Git | 2.50.0.windows.1 |
| Eclipse | Platform 4.40.0 (2026-06 R), m2e 2.7.800, Spring Tools boot-ls 2.1.1 |
| VS Code | installed at `D:\Microsoft VS Code` |
| Docker Desktop | 29.3.1 installed; daemon not started yet |

Also present but unused: JDK 17.0.18.8. Eclipse's default project JRE is
explicitly set to Adoptium 21.

## Environments

None provisioned yet. Planned: Neon development branch and Neon production
branch (EU region), Railway EU, Cloudflare Pages. Automated tests will use
PostgreSQL Testcontainers, never the Neon development database.

## Flyway migrations

None yet.

## Implemented modules and endpoints

None yet.

## Tests currently passing

None yet.

## External integrations configured

None yet. Just-in-time schedule: Neon at Phase 2, Resend at Phase 4,
Google Document AI at Phase 9, Firebase at Phase 11, Cloudflare R2 at Phase 12,
Sentry at Phase 15.

## Decisions taken during the build

| Date | Decision |
|---|---|
| 2026-08-12 | IDE split: backend in Eclipse, frontend in VS Code. Not a spec concern; `pom.xml` and `package.json` stay the source of truth for project structure. |
| 2026-08-12 | Line endings handled by a committed `.gitattributes` (`* text=auto eol=lf`) rather than per-machine `core.autocrlf`, so Linux CI and the macOS iOS build behave identically. |
| 2026-08-12 | Default branch is `main`. |

## Known issues and open decisions

Carried over from specification section 35 (intentionally deferred):

- Final product name, logo, colours and production domain.
- Normalised X/Y/width/height coordinates for certificate overlay fields —
  calibrated in Phase 8, never guessed.
- Final OCR confidence thresholds — calibrated against representative samples.
- Capacitor secure-storage plugin — selected during the mobile phase against
  current documentation.
- Final legal wording, retention periods and privacy notices before release.

Open for this build:

- Mac availability for Phase 18 (iOS) is not yet confirmed. Does not block any
  phase before 17.
- Google Document AI processor version must be verified as currently supported
  at Phase 9; not frozen in advance.
- Spring Security Argon2 encoder implementation to be verified against current
  documentation at Phase 4. Argon2 itself is the frozen algorithm.

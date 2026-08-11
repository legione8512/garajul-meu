# Garajul Meu

Vehicle management application for private vehicle owners in Romania.
Web-first, packaged for Android and iOS with Capacitor.

V1 covers vehicle identity, the digital Romanian registration certificate,
ITP / RCA / CASCO / rovinieta expiry tracking, renewal history and reminders
delivered as native push notifications.

## Repository layout

```
.
├── backend/    Spring Boot modular monolith (Java 21, Maven)  -- opened in Eclipse
├── frontend/   React + TypeScript + Vite + Capacitor          -- opened in VS Code
└── docs/       PROJECT_STATE.md and project documentation
```

## Stack

| Area | Technology |
|---|---|
| Frontend | React, TypeScript, Vite, React Router, i18next |
| Mobile | Capacitor (Android, iOS) |
| Backend | Java 21, Spring Boot, Maven |
| API | REST + JSON under `/api/v1` |
| Persistence | Spring Data JPA + Hibernate |
| Database | Neon PostgreSQL, migrations via Flyway |
| Object storage | Cloudflare R2 |
| OCR | Google Document AI Enterprise Document OCR |
| Push | Firebase Cloud Messaging (native only in V1) |
| Email | Resend |
| Hosting | Cloudflare Pages (web), Railway (API) |
| CI | GitHub Actions |

## Getting started

Current status, tool versions and the next verified step are tracked in
[docs/PROJECT_STATE.md](docs/PROJECT_STATE.md).

## Specification

This repository implements *Garajul Meu — Master Technical Specification V1.1*,
which is authoritative for scope, data model, API contract, security and
privacy rules. Approved decisions are not changed without an explicit
architecture-change proposal.

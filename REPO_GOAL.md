# Android Core Kit — Repository Goal

## Mission
Build a reusable Android starter template that can be cloned across many products, with capability parity to `mobile-core-kit`, adapted to Android/Kotlin architecture and tooling.

## North Star References
- Capability baseline: `/mnt/c/Development/_CORE/mobile-core-kit`
- Default backend contract: `/mnt/c/Development/_CORE/backend-core-kit`
- Current architecture proposal for this repo: `_WIP/*` and `docs/android-architecture-guide.md`

## Locked Product/Engineering Decisions
- Flavors: `dev` and `prod` only.
- Environment differences (for now): `applicationIdSuffix` (dev only) + `BuildConfig.BASE_URL`.
- Backend API is treated as stable and sourced from backend-core-kit OpenAPI contract (`docs/openapi/openapi.yaml` in backend repo).
- Android structure adapts to platform conventions; we mirror capabilities, not folder structure 1:1.
- First milestone target: **foundation + auth end-to-end**.

## P0 Scope (Must Exist Before Expansion)
1. Stable core modules/contracts (`core/common`, `core/navigation`, `core/session`, `core/designsystem`, `core/testing`).
2. Auth vertical slice wired end-to-end against backend-core-kit:
   - register, login, forgot/reset flow, email verification, logout.
3. Session/current-user baseline with mobile-core-kit equivalent behavior:
   - secure token persistence + restore on cold start,
   - auth-pending hydration via `GET /v1/me`,
   - refresh-and-retry policy (write retry requires idempotency key),
   - race guards for account/session switches,
   - logout clears tokens and cached user.

## Quality Bar (Parity Target with mobile-core-kit)
- One canonical gate: `./gradlew verify`.
- Gate must block merge on: build variants, formatting, static analysis, boundary checks, and tests.
- Required test layers for changed behavior:
  - unit tests (domain/presentation),
  - integration tests (data mapping/session boundaries),
  - E2E smoke for critical auth journey.
- CI must run the same canonical gate.

## Open Decision (Needs Resolution Early)
- API integration strategy: generated client (OpenAPI-first) vs manual datasource/repository implementation.
- Decision point: before completing auth backend integration phase.

## Success Criteria
- A new product team can clone this repo and reach a production-safe auth-enabled baseline with minimal structural rework and consistent quality gates.

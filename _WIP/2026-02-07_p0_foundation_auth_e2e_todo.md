# TODO — P0 Foundation + Auth (Login/Register First)

**Project:** `android-core-kit`  
**Date:** 2026-02-07  
**Status:** Planned  
**Primary goal source:** `REPO_GOAL.md`

This TODO is the execution plan for the first product-ready milestone: **foundation first**, then a narrow auth slice (**login + register**) against `backend-core-kit`.

Goal: prove architecture and code quality before scaling to additional auth endpoints.

## Locked decisions

- Capability parity target: `/mnt/c/Development/_CORE/mobile-core-kit`
- Backend contract source: `/mnt/c/Development/_CORE/backend-core-kit/docs/openapi/openapi.yaml`
- Flavors: `dev` and `prod` only.
- Env differences only: `applicationIdSuffix` (dev), `BuildConfig.BASE_URL`.
- First milestone scope: foundation + login/register only.

## Milestone definition of done

- [ ] Core runtime foundations compile and are verified by `./gradlew verify`.
- [ ] Login/register flow works end-to-end with backend-core-kit:
  - [ ] register (`/v1/auth/password/register`)
  - [ ] login (`/v1/auth/password/login`)
- [ ] Foundation quality proves feature simplicity:
  - [ ] datasource code remains thin (API helper style, parser-based mapping)
  - [ ] feature code does not duplicate networking/session/error plumbing
- [ ] Session baseline is sufficient for login/register iteration:
  - [ ] token persistence + cold-start restore
  - [ ] refresh path exists for authenticated requests (minimal, safe baseline)
  - [ ] logout clears tokens and cached user (implemented but logout endpoint wiring can be deferred)
- [ ] Minimum tests exist at unit + integration + login/register smoke level.

## Scope boundaries (P0)

In scope:
- `core/network` foundation modeled after mobile-core-kit quality.
- minimal `core/session` runtime needed to support login/register safely.
- `feature/auth` login/register flows.
- root navigation gating for unauthenticated/authenticated states.

Out of scope:
- email verify/resend, forgot/reset, password change, OIDC, push token sync.
- profile image upload, account deletion, admin endpoints.
- New environments (`staging`), analytics expansion, non-essential UX polish.

## Phase 0 — Contract alignment and gap map

- [ ] Build capability matrix: mobile-core-kit vs current Android status.
- [ ] Build endpoint matrix from backend-core-kit OpenAPI for immediate scope:
  - `/v1/auth/password/register`
  - `/v1/auth/password/login`
- [ ] Note deferred endpoints for later phase (verify/reset/logout/me hydration).
- [ ] Freeze DTO/error mapping expectations (`{data, meta?}` + RFC7807 problem details).

Exit criteria:
- [ ] No unresolved contract ambiguity for P0 endpoints.

## Phase 1 — `core/network` golden foundation

- [ ] Create `:core:network` module and wire it into settings/build-logic usage.
- [ ] Add HTTP client stack (Retrofit/OkHttp + converter strategy).
- [ ] Add API helper abstractions so datasource code stays thin:
  - [ ] `ApiHost` + host routing
  - [ ] `ApiResponse<T>` wrapper (`isError`, `statusCode`, `message`, parsed body/meta)
  - [ ] parser-based request methods (`get/post/...`) with `throwOnError` option
- [ ] Implement response envelope parsing and error mapping:
  - [ ] success envelope `{ data, meta? }`
  - [ ] RFC7807 error model
  - [ ] request/trace id propagation
- [ ] Add minimal auth header/refresh support required by login/register follow-up calls.
- [ ] Add tests (MockWebServer) for envelope/error/parser behavior.

Exit criteria:
- [ ] `core/network` test suite is green.
- [ ] Auth datasource methods are concise and mostly declarative.

## Phase 2 — minimal session runtime for auth bootstrap

- [ ] Move from in-memory session to persisted session implementation.
- [ ] Add token persistence and restore on startup.
- [ ] Add cached user store contract hook (implementation can stay minimal in this phase).
- [ ] Add minimal race-safety guard around token refresh and logout.
- [ ] Ensure logout clears all persisted session/user state.
- [ ] Add deterministic tests for restore/logout/switch safety.

Exit criteria:
- [ ] Cold start with persisted tokens is stable.
- [ ] Session transitions do not leak stale state.

## Phase 3 — `feature/auth` login/register slice

- [ ] Build auth feature vertical slice (presentation/domain/data/di).
- [ ] Implement flows:
  - [ ] register
  - [ ] login
- [ ] Wire session manager integration on login/register success.
- [ ] Root navigation gate:
  - [ ] `UnauthenticatedRoot` for auth graph
  - [ ] `AuthenticatedRoot` for app graph
- [ ] Add login/register smoke test for critical happy path.

Exit criteria:
- [ ] Dev build can complete register -> login -> authenticated root path.

## Phase 4 — quality gate parity

- [ ] Keep `./gradlew verify` as single canonical gate.
- [ ] Ensure verify includes:
  - [ ] assemble dev/prod
  - [ ] unit tests
  - [ ] lint
  - [ ] detekt
  - [ ] spotless
  - [ ] feature dependency boundary check
- [ ] Ensure CI runs the same gate and fails on regressions.
- [ ] Add/confirm report visibility in CI artifacts where useful.

Exit criteria:
- [ ] PR merge requires green canonical gate.

## Open decisions (resolve early)

- [ ] API integration approach:
  - Option A: generated OpenAPI client/models + manual repositories/use cases.
  - Option B: fully manual datasource/repository implementation.
- [ ] Record decision in docs and enforce it consistently in code review.
- [ ] Logging abstraction choice for network/session (`Log`-style façade vs direct logger usage).

## Execution rhythm

- [ ] Deliver in small PRs:
  - PR1: `core/network` foundation
  - PR2: minimal session runtime
  - PR3: auth login/register
  - PR4: quality/CI hardening + docs
- [ ] Each PR must include: scope note, tests added/updated, and exact verify commands run.

## Risks and mitigations

- Risk: overbuilding auth endpoints before foundation quality is proven.
  - Mitigation: keep first auth slice strictly login/register.
- Risk: weak network abstractions causing noisy/fragile datasources.
  - Mitigation: force parser-based API helper pattern and test it first.
- Risk: session bugs hidden until later features.
  - Mitigation: add lifecycle/race safety tests in phase 2 before expanding endpoints.

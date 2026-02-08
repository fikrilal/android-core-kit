# Network Architecture Refactor Proposal (Enterprise Android)

## Context

- Current `core/network` was optimized for fast P0 progress and mirrors `mobile-core-kit` capability semantics.
- `docs/android-architecture-guide.md` is a draft architecture guide, not a strict 1:1 implementation template.
- For Android-native long-term scale, we should adopt a Retrofit-first enterprise pattern while preserving current behavior guarantees.

## Problem Statement

Current helper-centric networking (`ApiHelper`) is strong for bootstrap but suboptimal as the permanent architecture:

- Request contracts are parser-driven instead of Retrofit interface-driven.
- Endpoint definitions are not co-located with feature data modules (`feature/*/data/remote`).
- Enterprise concerns (auth refresh orchestration, observability, redaction, retry policy, security hardening) are harder to standardize as infrastructure.

## Goals

1. Move to enterprise-native Android networking architecture.
2. Keep existing reliability behavior (envelope parsing, RFC7807 mapping, transport error taxonomy, idempotency-aware retry).
3. Implement login/register on top of the new architecture without regressions.
4. Keep migration incremental and reversible.

## Non-Goals

- Full auth endpoint expansion beyond login/register in this refactor.
- Introducing extra environments (`staging`) or unrelated infrastructure.
- Large cross-module refactors outside `core/network`, `core/session`, and `feature/auth`.

## Target Architecture

### `:core:network` responsibilities (infrastructure only)

- `RetrofitFactory` and `OkHttpClient` provisioning.
- Interceptor chain:
  - auth header injection,
  - request id / trace propagation,
  - redacted logging (dev-only detail).
- Auth refresh orchestration via `Authenticator` + single-flight refresh coordination (integrated with `core/session` contracts).
- Shared serialization config (`kotlinx.serialization`).
- Central network error parser:
  - success envelope `{ data, meta? }`,
  - RFC7807 problem details,
  - transport/local errors (`NETWORK_UNAVAILABLE`, `REQUEST_TIMEOUT`, etc.).
- `safeApiCall` utility returning standardized `ApiResponse` or mapped domain-facing error primitive.

### `:feature:*` responsibilities

- Own Retrofit interfaces (`AuthApi`) in `feature/auth/data/remote`.
- Own DTOs and mappers in feature data layer.
- Repositories map infra errors to `AppResult/AppError` (from `core/common`).

### `:core:session` responsibilities

- Token source of truth (persist/restore/clear).
- Refresh token use path used by authenticator.
- Concurrency/race safety around refresh and logout.

## Proposed Migration Plan

### Phase 1 — Infra Foundation (core/network refactor)

- Add Retrofit + converter setup.
- Add interceptors and authenticator wiring with DI.
- Add `NetworkErrorParser` and `safeApiCall`.
- Keep `ApiHelper` temporarily as compatibility layer (deprecated for new code).

### Phase 2 — Auth Vertical Slice (login/register only)

- Implement `AuthApi` endpoints:
  - `POST /v1/auth/password/register`
  - `POST /v1/auth/password/login`
- Build `AuthRemoteDataSource` and repository using new stack.
- Integrate session persistence on success.

### Phase 3 — Stabilization and Decommission

- Add parity tests between old/new behavior for critical cases.
- Migrate remaining consumers away from `ApiHelper`.
- Remove or sharply reduce helper surface once no longer needed.

## Testing & Verification Requirements

- Unit tests (`core/network`):
  - envelope parsing, RFC7807 mapping, transport error mapping,
  - authenticator refresh behavior (401 flows, single-flight guard).
- Integration tests (`feature/auth` + MockWebServer):
  - login/register happy path and representative error path.
- Mandatory gate:
  - `./gradlew verify` (via `tool/agent/winrun --no-stdin -- ./gradlew.bat verify` in WSL environments).

## Risks and Mitigations

- Risk: refactor slows auth delivery.
  - Mitigation: keep scope to login/register and preserve compatibility layer in phase 1.
- Risk: refresh loop or token race bugs.
  - Mitigation: explicit single-flight guard + deterministic tests before rollout.
- Risk: overengineering too early.
  - Mitigation: keep `core/network` infra minimal; move endpoint contracts to features.

## Acceptance Criteria

- `feature/auth` login/register fully uses Retrofit-first path (no new `ApiHelper` usage).
- Network failures map deterministically and consistently with current taxonomy.
- Session token write/restore works for auth happy path.
- `verify` is green.
- Architecture docs and AGENTS references updated to reflect final networking direction.

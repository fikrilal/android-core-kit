# PR2 Network Hardening Plan

## Context Snapshot
- `core/network` is now split into enterprise-aligned packages (`auth`, `client`, `error`, `execution`, `interceptor`, `model`, `serialization`).
- Retrofit-first infrastructure is in place.
- Current DI still uses `NoOpAccessTokenProvider` and `NoOpAccessTokenRefresher`, so auth/session behavior is not production-ready yet.

## Objective
Make the network stack production-safe for the P0 auth slice (login/register first) without widening scope to unrelated features.

## In Scope
1. Wire real token provider/refresher from `core/session` into `app` DI.
2. Add explicit OkHttp hardening config:
- connect/read/write timeouts
- call timeout
- connection pool policy
3. Introduce retry strategy for transport + `429/5xx`:
- safe methods (`GET/HEAD/OPTIONS`) can retry
- write methods require `Idempotency-Key`
4. Add observability hook surface for request outcomes (success/error/latency/correlation id).
5. Expand tests:
- refresh single-flight and retry boundaries
- idempotency enforcement
- login/register integration with MockWebServer

## Out of Scope
- Non-auth endpoint rollout.
- New environment flavors.
- Broad refactor outside `core/network`, `core/session`, and auth data layer wiring.

## Work Plan
1. Define final contracts for `AccessTokenProvider` and `AccessTokenRefresher` integration with session state.
2. Implement hardened `OkHttpClient` builder defaults in `NetworkClientFactory`.
3. Add retry policy component and integrate in execution path.
4. Add telemetry hook API and default no-op implementation.
5. Add/adjust tests and run canonical verification.

## Acceptance Criteria
- No no-op auth provider/refresher in production DI path.
- Deterministic retry behavior with idempotency safety.
- Request tracing and error taxonomy preserved.
- `tool/agent/winrun --no-stdin -- ./gradlew.bat verify` is green.

## Risks
- Retry loops or duplicate writes.
- Token refresh race on concurrent unauthorized requests.
- Silent regressions in auth error mapping.

## Mitigation
- Strict retry guard rules + unit tests.
- Single-flight refresh tests under concurrency.
- MockWebServer integration coverage for auth success/failure flows.

# PR3–PR7 Hardening Plan

## Context Snapshot
- PR1 established baseline foundation and PR2 hardened network wiring for auth/session integration.
- Current state is implementation-ready for login/register, but several production-hardening concerns are still open.
- This plan defines the next hardening sequence before expanding endpoint scope.

## Objective
Raise the template from "working foundation" to "production-safe auth/session baseline" in small, reviewable PRs.

## Scope Boundaries
In scope:
- `core/session`, `core/network`, and app-level DI/runtime wiring.
- Reliability, security, observability, and error-contract hardening.

Out of scope:
- Non-auth feature rollout.
- New flavors beyond `dev` and `prod`.
- Broad architecture rewrites unrelated to auth/session/network.

## PR3 — Secure Session Persistence
Goal: replace volatile session state with secure persisted state.

- Implement encrypted token persistence (Keystore-backed encryption + DataStore).
- Hydrate session on cold start with explicit state transitions.
- Keep token writes atomic (`accessToken` + `refreshToken` update together).
- Ensure logout clears persisted tokens and cached user data.
- Add tests for restore, clear, and corruption/fallback behavior.

Acceptance criteria:
- Restart retains valid session state.
- Corrupt persisted payload fails closed (unauthenticated).
- `verify` gate remains green.

## PR4 — Refresh Robustness
Goal: make refresh behavior deterministic under concurrency and failure.

- Add refresher-level single-flight guard to prevent parallel refresh storms.
- Define refresh timeout/cancellation policy.
- Enforce strict unauthorized handling (`401/403` => session logout).
- Add tests for concurrent unauthorized requests, timeout, and terminal refresh failure.

Acceptance criteria:
- At most one refresh call is active for a token epoch.
- No infinite retry/refresh loops.
- Session state converges deterministically after failure.

## PR5 — Transport Security Hardening
Goal: tighten network transport posture for production.

- Add certificate pinning for `prod` hosts.
- Keep non-pinned configuration for `dev` with explicit DI branching.
- Harden TLS configuration policy through OkHttp client setup.
- Add fail-closed tests for pin mismatch and config tests for env-specific behavior.

Acceptance criteria:
- `prod` client uses pin set and rejects mismatch.
- `dev` remains usable for local/backend-core-kit iteration.

## PR6 — Error Contract Hardening
Goal: standardize error surfaces so feature code stays simple and predictable.

- Define typed error taxonomy for auth/session/network failures.
- Normalize backend problem-details + transport/parsing errors into stable domain errors.
- Ensure UI-facing layers consume typed errors instead of ad-hoc string parsing.
- Add mapping tests for key auth failure cases.

Acceptance criteria:
- Same backend/transport failures always map to the same typed error.
- Feature/auth presentation logic has no HTTP-code branching.

## PR7 — Observability Hardening
Goal: make request/session behavior diagnosable without leaking sensitive data.

- Implement real `NetworkTelemetryObserver` sink from current no-op surface.
- Emit structured events for retry, refresh, auth failures, and latency.
- Enforce redaction rules for tokens/PII in logs and telemetry payloads.
- Add correlation between `X-Request-Id`, retry attempts, and refresh outcomes.

Acceptance criteria:
- Critical auth/network flows are traceable end-to-end in logs/telemetry.
- No token or sensitive payload leakage in logs.

## Execution Rules
- One PR = one hardening objective.
- Each PR must include:
  - scope note,
  - tests added/updated,
  - exact verification commands run.
- Canonical verification command:
  - `tool/agent/winrun --no-stdin -- ./gradlew.bat verify`

## Risks and Mitigations
- Risk: security hardening slows feature delivery.
  - Mitigation: keep each PR narrowly scoped and merge incrementally.
- Risk: behavior regressions during persistence/refresh refactor.
  - Mitigation: prioritize deterministic tests before expanding auth endpoints.
- Risk: observability introduces sensitive data leaks.
  - Mitigation: enforce redaction-by-default and review log payloads explicitly.

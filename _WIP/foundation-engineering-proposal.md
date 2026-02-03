# AndroidCoreKit — Foundation Engineering Proposal (WIP)

This document proposes the **Foundation** work (Backlog Phase 0). The goal is to make the starter template compile cleanly, enforce boundaries, and make future features (especially auth) cheap to build.

## Context

- This repo is a **clone-per-app starter** (not a shared SDK).
- We standardize on **Hilt** for DI.
- Environments: **dev** and **prod** only.
- For now, env differences are **only**:
  - `applicationIdSuffix` (dev only)
  - `BuildConfig.BASE_URL`

## Production-readiness bar (Foundation must meet this)

This is not a toy project. “Foundation” is considered done only if:

- Builds are **reproducible** (wrapper + pinned versions + consistent toolchain).
- The repo has a **single verification entry point** (local + CI).
- Module boundaries are enforceable by **Gradle structure**, not just by docs.
- Secrets/signing are handled safely (no accidental commits, clear cloning steps).

## Goals

- Establish a **multi-module baseline** that matches `docs/android-architecture-guide.md`.
- Add **convention plugins** (`build-logic`) so every new module is consistent.
- Add **dev/prod flavors** as a stable config surface (no scattering env logic).
- Ensure git + tooling are reliable in WSL/Windows workflows.

## Non-goals (explicitly out of scope for Foundation)

- Implementing real backend API calls (we’ll read backend contracts later).
- Persisted session/token refresh (we’ll do in later phases).
- Full auth feature implementation (only scaffolding decisions).
- Adding extra envs (no `staging`).

## Proposed module baseline

These modules are the initial stable “platform” for every cloned app:

- `:app`
  - Composition root: hosts `MainActivity`, root `NavHost`, and platform-only implementations.
- `:core:common`
  - Pure-ish Kotlin helpers: `AppResult`, `AppError`, dispatcher provider, small utilities.
- `:core:ui`
  - Shared UI primitives that should not be re-invented per feature (e.g., `UiText`, basic Loading/Error content).
- `:core:designsystem`
  - Compose theme + tokens + shared components.
- `:core:navigation`
  - Contracts only: `Destination`, route builders, **graph-level destinations** (`AuthenticatedRoot`, `UnauthenticatedRoot`).
- `:core:session`
  - Session contract: `SessionState`, `SessionManager`.
  - No backend details; other features depend on this rather than on `:feature:auth`.
- `:core:testing`
  - Shared test helpers: coroutine rules/dispatchers, fakes.
- `:feature:auth` (placeholder module exists, but behavior can be stubbed initially)
  - A vertical slice module that will eventually contain login/register/forgot/confirm/logout.

### Dependency rules (enforced by structure + conventions)

- `:app` depends on all `:feature:*` and `:core:*`.
- `:feature:*` depends on `:core:*` but **never** on other `:feature:*`.
- `:core:*` modules depend only on what they need (prefer no Android deps in `:core:common` when possible).

## Toolchain & reproducibility (explicit decisions)

Foundation must standardize the toolchain so a cloned app builds the same way on every machine and in CI.

- **JDK:** Use **JDK 17** for Gradle/AGP (documented + enforced via Gradle toolchains).
- **Gradle:** Use the repo’s Gradle wrapper (`gradlew`) only; do not rely on system Gradle.
- **Version management:** Keep dependencies and plugin versions in `gradle/libs.versions.toml`.
- **Line endings:** Keep repo text files as LF (with explicit CRLF allowlist for Windows scripts).
- **IDE files:** Do not commit user-local IDE state (`.idea/` ignored).

## Build system: `build-logic` convention plugins

Rationale: this template is meant to be cloned repeatedly. Convention plugins prevent copy/paste divergence between apps and allow new modules to be added quickly.

### `build-logic` layout

- `build-logic/` (included build)
  - convention plugins:
    - `android-application` (standard app config, compose opt-in hook, flavors)
    - `android-library` (standard library config)
    - `android-feature` (library + feature defaults)
    - `compose` (Compose + compiler options)
    - `hilt` (Hilt plugin + deps + KAPT/KSP decision)

### Standardized Android config (centralized)

In convention plugins:

- `compileSdk` pinned (from version catalog / shared constant)
- `minSdk` pinned
- Java/Kotlin toolchain pinned (JDK 17)
- Common test options baseline
- `android { namespace }` remains module-specific

## Annotation processing (KSP/KAPT)

Decision: **KSP-first** (with a safe fallback).

- Recommended: use **KSP** for Hilt (faster builds, better incremental behavior).
- Fallback: if KSP is blocked by version/tooling constraints, use **KAPT** temporarily, but:
  - keep it limited to the minimum set of modules,
  - codify it in `build-logic` so the project stays consistent,
  - record the reason and the exit plan (switch back to KSP) in `_WIP/*`.

## Flavors & config surface (dev/prod only)

### Flavor model

- `flavorDimensions += "env"`
- `productFlavors`:
  - `dev`
    - `applicationIdSuffix = ".dev"`
    - `buildConfigField("String", "BASE_URL", "\"https://dev.example.invalid\"")`
  - `prod`
    - `buildConfigField("String", "BASE_URL", "\"https://prod.example.invalid\"")`

Notes:

- We keep base URLs as placeholders until backend contract work begins.
- We avoid additional differences (no app name/icon changes yet) to prevent template sprawl.

### Consumption pattern

- `BuildConfig.BASE_URL` should not be referenced everywhere directly.
- Instead, introduce a small DI-provided config surface in `:app` (e.g., `ApiConfig(baseUrl: String)`) that is injected into networking and repositories later.
  - This keeps the rest of the codebase testable and reduces “BuildConfig is everywhere” coupling.

## DI (Hilt) baseline

### Rule

- `:app` owns the composition root and any platform implementations/overrides.
- Each module contributes bindings via `@Module` + `@InstallIn(SingletonComponent::class)` (or appropriate component).

### Foundation deliverables

- Hilt enabled in `:app` and at least one `@Module` in `:app` that binds:
  - `SessionManager` to an in-memory implementation (for now)
  - `ApiConfig` or equivalent config wrapper

## Quality gates (local + CI)

Production-ready templates need guardrails. Foundation should define the baseline, even if later phases expand it.

### Verification entry point

Add a single Gradle task (root-level) that CI and developers run:

- `./gradlew verify`

Minimum contents:

- `:app:assembleDevDebug`
- `:app:assembleProdDebug`
- unit tests for all modules (`test*UnitTest`)
- Android Lint for `devDebug` (and optionally `prodDebug`)

### Static analysis & formatting (baseline decision)

Decision: **Spotless + ktlint** (formatting) and **Detekt** (static analysis).

- Formatting: `spotless` with `ktlint` backend for Kotlin.
- Static analysis: `detekt` with a minimal, template-safe ruleset (expand as needed).

These should be runnable via `./gradlew verify` and configured once in `build-logic`.

### CI baseline (GitHub Actions)

Since this repo is hosted on GitHub, the default CI target is GitHub Actions:

- A `verify` workflow triggered on `push` + `pull_request`.
- Cache Gradle dependencies/build cache.
- Run `./gradlew verify`.
- Upload lint + test reports as artifacts (minimal but practical).

## Git + tooling baseline (WSL-safe)

- Use `tool/agent/gitw --no-stdin` for git commands when WSL needs Windows credential manager.
- Repo includes `.gitattributes` to prevent line-ending churn across Windows/WSL.

## Clone checklist (what every new app must do)

Foundation should include a short checklist (README or docs) so cloning is safe and fast:

- Rename `applicationId` / package and update namespaces.
- Update `BuildConfig.BASE_URL` for `dev` and `prod`.
- Configure signing (keystore not committed; values supplied via env/secure gradle props).
- Replace app name/icon if needed (optional; not part of env differences by default).
- Confirm `./gradlew verify` is green on the new clone before adding features.

## Acceptance criteria (Definition of Done)

- Module skeleton exists with the baseline modules above (even if some are stubs).
- Convention plugins exist and are used by modules (no repeated Gradle boilerplate).
- `dev` and `prod` variants build:
  - `./gradlew :app:assembleDevDebug`
  - `./gradlew :app:assembleProdDebug`
- Hilt wiring compiles with a minimal composition root.
- `BuildConfig.BASE_URL` is set per flavor and can be injected via a single config object.
- `./gradlew verify` exists and runs the baseline checks (build + unit tests + lint).

## Risks / Tradeoffs

- Too many modules too early can slow iteration; we mitigate by keeping `:core:*` minimal and only adding `:core:network/:core:datastore` when needed.
- Overusing `BuildConfig` directly creates coupling; mitigate with an injected config interface.
- Convention plugins add upfront complexity; for a starter template, the long-term payoff is high.
- With AGP built-in Kotlin, KSP currently requires `android.disallowKotlinSourceSets=false` to allow registering generated sources (experimental flag; remove once supported).

## Open questions (parked for later phases)

- Backend auth contract details (token types, refresh semantics, error model): will be read from `/mnt/c/Development/_CORE/backend-core-kit`.
- Whether we standardize on Retrofit + Kotlinx Serialization or Moshi (proposal leans Kotlinx Serialization, but we’ll confirm later).
- The exact static analysis stack (ktlint vs spotless vs both; detekt ruleset scope) beyond the baseline “verify” requirement.

## References

- Architecture guide: `docs/android-architecture-guide.md`
- Backend contracts: `/mnt/c/Development/_CORE/backend-core-kit`
- Flutter reference implementation: `/mnt/c/Development/_CORE/mobile-core-kit`

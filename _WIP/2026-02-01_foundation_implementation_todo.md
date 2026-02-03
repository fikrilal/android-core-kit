# TODO — Foundation (build-logic + modules + Hilt + dev/prod flavors + verify + CI)

**Project:** `android-core-kit`  
**Date:** 2026-02-01  
**Status:** Planned  
**Related proposal:** `_WIP/foundation-engineering-proposal.md`  

This is the implementation checklist for **Backlog Phase 0: Foundation**.
The goal is a production-ready starter template baseline that is reproducible, dependency-safe, and cheap to extend.

## Decisions (locked)

- Environments: **dev** and **prod** only.
- Env differences (for now): **only**:
  - `applicationIdSuffix` (dev only)
  - `BuildConfig.BASE_URL`
- DI: **Hilt**.
- Toolchain: **JDK 17** + Gradle wrapper.
- Annotation processing: **KSP-first** (fallback to KAPT only if blocked; document why).
- Formatting: **Spotless + ktlint**.
- Static analysis: **Detekt**.

## Implementation notes

- KSP/KAPT: implement KSP by default; only fall back to KAPT if forced by tooling constraints.
- Spotless/Detekt: wire both into `./gradlew verify` so local + CI share the same gates.

## Target outcomes (definition of done)

- [ ] **Reproducible builds:** Gradle wrapper + pinned versions + JDK 17 toolchain is enforced.
- [ ] **Multi-module baseline exists** (`:core:*`, `:feature:auth`, `:app`) and compiles.
- [ ] **Convention plugins exist** (`build-logic`) and are applied (no boilerplate drift).
- [ ] **Dev/prod flavors exist** with only `applicationIdSuffix` + `BASE_URL` differences.
- [ ] **Single verification entry point exists:** `./gradlew verify` (local + CI).
- [ ] **CI runs verify:** GitHub Actions workflow runs `./gradlew verify` on push/PR.
- [ ] **Clone checklist exists:** safe steps for renaming package/appId, baseUrl, and signing.

## Working agreements

- Keep this phase **foundational and mechanical** (structure + wiring). Avoid feature behavior work.
- Prefer **phase-based PRs** or at least phase-based commits to keep review sane.
- Don’t introduce secret handling that risks accidental commits. Default to safe placeholders.

---

## Phase 0 — Baseline & safety rails (no architecture changes yet)

- [x] Ensure Windows-git wrapper exists and works:
  - [x] `tool/agent/gitw --no-stdin --version`
- [x] Ensure repo hygiene is in place:
  - [x] `.idea/` ignored in `.gitignore`
  - [x] `.gitattributes` enforces LF (CRLF allowlist for Windows scripts)
- [x] Capture “before” build baseline (record results here):
  - [x] `./gradlew :app:assembleDebug`
  - [x] `./gradlew test` (or `:app:testDebugUnitTest`)

Results (2026-02-01):
- `:app:assembleDebug` ✅ (ran via `tool/agent/winrun --no-stdin -- ./gradlew.bat :app:assembleDebug`, ~1m)
- `:app:testDebugUnitTest` ✅ (ran via `tool/agent/winrun --no-stdin -- ./gradlew.bat :app:testDebugUnitTest`, ~4s)
- Note: current WSL environment has no Linux JDK; Windows Gradle wrapper is the baseline execution path.

Checkpoint:
- [x] Baseline results recorded in this doc (success/fail + notes).

---

## Phase 1 — Introduce `build-logic` (convention plugins)

Goal: make module creation consistent and remove Gradle copy/paste across app clones.

- [x] Add included build: `build-logic/` and wire via `settings.gradle.kts` (`includeBuild("build-logic")`).
- [x] Create convention plugins (minimal, production-grade):
  - [x] `android-application` (`androidcorekit.android.application`)
  - [x] `android-library` (`androidcorekit.android.library`)
  - [x] `android-feature` (`androidcorekit.android.feature`)
  - [x] `compose` (`androidcorekit.compose`)
  - [x] `hilt` (`androidcorekit.hilt`)
- [x] Centralize common Android config in plugins:
  - [x] JDK 17 toolchain
  - [x] `compileSdk`, `minSdk`, `targetSdk` rules
  - [x] consistent Kotlin/Compose configuration
- [x] Codify annotation processing choice (KSP/KAPT) in `hilt` plugin.

Checkpoint:
- [x] `./gradlew help` works with included build enabled.
- [x] A sample module applies convention plugins successfully (`:app`).

---

## Phase 2 — Create baseline modules (skeleton + minimal code)

Goal: establish module graph and stable “core contracts” before feature work.

### 2.1 Add modules + Gradle wiring

- [x] Add modules and include them in `settings.gradle.kts`:
  - [x] `:core:common`
  - [x] `:core:ui`
  - [x] `:core:designsystem`
  - [x] `:core:navigation`
  - [x] `:core:session`
  - [x] `:core:testing`
  - [x] `:feature:auth`
- [x] Apply convention plugins to each module (no bespoke build.gradle boilerplate).
- [x] Wire minimal dependency rules:
  - [x] `:feature:*` must not depend on `:feature:*` (Gradle task: `checkFeatureModuleDependencies`).

### 2.2 Minimal code contracts (compile-first, no backend)

- [x] `:core:common`:
  - [x] `AppResult` + `AppError` baseline types
  - [x] `AppDispatchers`/dispatcher provider abstraction for testability
- [x] `:core:navigation`:
  - [x] `Destination` contract
  - [x] `AppDestinations.AuthenticatedRoot` + `UnauthenticatedRoot`
- [x] `:core:session`:
  - [x] `SessionState` (minimal: unknown/unauthenticated/authenticated + optional metadata)
  - [x] `SessionManager` interface (state flow + logout)

Checkpoint:
- [x] `./gradlew :app:assembleDebug` still works after module extraction.
  - ✅ ran via `tool/agent/winrun --no-stdin -- ./gradlew.bat :app:assembleDebug`
  - ✅ ran `tool/agent/winrun --no-stdin -- ./gradlew.bat checkFeatureModuleDependencies`

---

## Phase 3 — Hilt baseline wiring (composition root)

Goal: `:app` composes modules; core remains contracts; app binds implementations.

- [x] Add `@HiltAndroidApp` application class in `:app`.
- [x] Enable Hilt in `:app` via convention plugin.
- [x] Add app DI module(s):
  - [x] Bind `SessionManager` to an **in-memory** implementation (Foundation only).
  - [x] Provide `ApiConfig` (or equivalent) that reads from `BuildConfig.BASE_URL`.
- [x] Ensure `:feature:auth` can depend on `:core:session` without circular dependencies.

Checkpoint:
- [x] `./gradlew :app:assembleDebug` is green with Hilt enabled.
  - ✅ ran via `tool/agent/winrun --no-stdin -- ./gradlew.bat :app:assembleDebug`
  - ✅ ran via `tool/agent/winrun --no-stdin -- ./gradlew.bat check`

Notes:
- KSP with AGP built-in Kotlin currently requires `android.disallowKotlinSourceSets=false` (see `gradle.properties`). Treat as temporary and remove once supported.

---

## Phase 4 — Flavors (dev/prod) + config surface

Goal: env differences are explicit and centralized; no “BuildConfig everywhere”.

- [x] Add flavor dimension `env` and flavors `dev` and `prod` (in convention plugin).
- [x] Configure `dev`:
  - [x] `applicationIdSuffix = ".dev"`
  - [x] `BuildConfig.BASE_URL` placeholder
- [x] Configure `prod`:
  - [x] `BuildConfig.BASE_URL` placeholder
- [x] Enforce consumption pattern:
  - [x] `ApiConfig` is the single injection surface for base URL (no widespread `BuildConfig` usage).

Checkpoint:
- [x] `./gradlew :app:assembleDevDebug`
  - ✅ ran via `tool/agent/winrun --no-stdin -- ./gradlew.bat :app:assembleDevDebug`
- [x] `./gradlew :app:assembleProdDebug`
  - ✅ ran via `tool/agent/winrun --no-stdin -- ./gradlew.bat :app:assembleProdDebug`

---

## Phase 5 — Verification entry point + CI baseline

Goal: production-ready starter means “hard to break”.

- [ ] Add root `verify` task (or plugin) that runs the baseline suite:
  - [ ] builds (devDebug + prodDebug)
  - [ ] unit tests
  - [ ] Android Lint
  - [ ] formatting/static analysis (once tools are chosen)
- [ ] Add GitHub Actions workflow:
  - [ ] cache Gradle
  - [ ] run `./gradlew verify`
  - [ ] upload lint/test reports artifacts (minimal)

Checkpoint:
- [ ] A PR triggers CI and `verify` passes on CI.

---

## Phase 6 — Clone checklist + signing safety

Goal: cloning is fast and safe; release signing doesn’t leak secrets.

- [ ] Add a short “Clone checklist” doc (README or `docs/template/*`):
  - [ ] rename `applicationId` + namespaces
  - [ ] update `BASE_URL` values
  - [ ] confirm `./gradlew verify` is green
- [ ] Add signing guidance:
  - [ ] release signing reads from env/gradle props (no keystore committed)
  - [ ] document required properties and safe local setup

Checkpoint:
- [ ] A fresh clone can follow docs and build dev/prod debug successfully.

---

## Notes / risks

- Flavors can easily sprawl. Keep them limited to baseUrl + applicationIdSuffix until a real need emerges.
- Build-logic adds upfront complexity; the payoff is preventing drift across future app clones.
- Boundary enforcement should be automated (at least at the Gradle dependency level) to avoid “best practice only in docs”.

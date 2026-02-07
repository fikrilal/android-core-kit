# Repository Guidelines

## Project Map (where to look)

```
app/
├─ src/main/java/dev/fikril/androidcorekit/
│  ├─ AndroidCoreKitApp.kt            # @HiltAndroidApp
│  ├─ MainActivity.kt                 # app entry activity
│  ├─ di/                             # app-level composition root bindings
│  ├─ navigation/                     # root nav host + top-level destinations
│  └─ session/                        # app-level session implementation

core/
├─ common/                            # AppResult/AppError/AppDispatchers contracts
├─ designsystem/                      # Compose theme/tokens/components
├─ network/                           # API helper, envelope/error mapping, retry policy
├─ navigation/                        # destination contracts + root destinations
├─ session/                           # SessionState/SessionManager contracts
├─ testing/                           # shared test dependencies/helpers
└─ ui/                                # shared UI primitives

feature/
├─ auth/                              # P0 auth slice (target: end-to-end)
├─ home/                              # placeholder feature
└─ profile/                           # placeholder feature

build-logic/                          # convention plugins (android/compose/hilt)
config/detekt/detekt.yml              # detekt baseline config
docs/android-architecture-guide.md    # architecture source of truth
REPO_GOAL.md                          # mission/scope/quality bar
```

## Simplicity First

- Build the minimum that solves the requested problem; avoid speculative abstractions.
- Prefer small, reversible changes over broad refactors.
- Keep module boundaries strict:
  - no `:feature:*` -> `:feature:*` dependencies,
  - shared contracts belong in `:core:*`.
- If a solution feels “framework-heavy” for one use case, simplify before merging.

## Coding Style & Naming

- Kotlin style: official (`kotlin.code.style=official`), 4-space indentation.
- Formatting: Spotless + ktlint (`./gradlew spotlessCheck`, `./gradlew spotlessApply`).
- Static analysis: Detekt (`maxIssues: 0`), config in `config/detekt/detekt.yml`.
- Naming:
  - classes/objects: `PascalCase`,
  - methods/fields: `camelCase`,
  - Compose screens: `*Screen`,
  - navigation contracts: `*Destination`.
- Keep Compose code readable: small composables, explicit state/effects, no side effects inside render paths.

## Testing Guidelines

- Unit tests: `src/test` (JUnit4 + coroutine test where relevant).
- Instrumented/UI tests: `src/androidTest` (AndroidX/Compose test APIs).
- Mirror source paths in tests where possible.
- Cover changed behavior at the right layer:
  - domain/session rules with unit tests,
  - data mapping/integration seams with integration tests,
  - critical auth journey with smoke instrumentation/E2E when introduced.

## Git, Commits & PRs

- Use `tool/agent/gitw --no-stdin ...` for git commands in WSL when needed.
- Do not commit/push unless explicitly requested.
- Commit style should match repo history: `<area>: <summary>` (e.g., `build: ...`, `foundation: ...`, `app: ...`).
- PRs should include:
  - problem + solution summary,
  - impacted modules,
  - verification commands run and results,
  - screenshots/video for UI changes,
  - linked issue/task when available.

## Agent Verification (required)

Agents must verify changes before claiming completion (when feasible).

Minimum checks (pick what changed):

- `./gradlew :app:assembleDevDebug`
- `./gradlew :app:assembleProdDebug`
- `./gradlew test`
- `./gradlew lint`
- `./gradlew detekt`
- `./gradlew spotlessCheck`
- `./gradlew checkFeatureModuleDependencies`

Full pipeline (preferred for non-trivial changes):

- `./gradlew verify`

WSL Windows-wrapper fallback:

- `tool/agent/winrun --no-stdin -- ./gradlew.bat verify`

## Agent Preferences (Code Authoring)

- Follow `REPO_GOAL.md` first; it defines mission, P0 scope, and quality expectations.
- Keep Android structure platform-native while mirroring capabilities from `mobile-core-kit`.
- For auth/session work, align behavior with mobile-core-kit semantics:
  - token persistence + restore,
  - `GET /v1/me` hydration when auth-pending,
  - refresh-and-retry with idempotency safety for writes,
  - race guards on session/account switches,
  - logout clears tokens and cached user.
- Use app module as DI composition root; keep feature modules isolated.
- Prefer explicit contracts in `core/*` over direct coupling between features.
- Avoid hidden config branching; consume env via DI surfaces (e.g., `ApiConfig`) rather than scattering `BuildConfig`.

## Documentation & Best Practices

- Start with:
  - `REPO_GOAL.md`
  - `docs/android-architecture-guide.md`
  - `_WIP/2026-02-07_p0_foundation_auth_e2e_todo.md`
- Backend contract source of truth:
  - `/mnt/c/Development/_CORE/backend-core-kit/docs/openapi/openapi.yaml`
  - `/mnt/c/Development/_CORE/backend-core-kit/docs/standards/`
- Mobile capability reference:
  - `/mnt/c/Development/_CORE/mobile-core-kit`
- When architecture decisions change materially, update docs in the same PR to keep future agent context accurate.

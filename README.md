# AndroidCoreKit

Starter/base Android project intended to be **cloned per app** and scaled based on the project’s needs.

## Locked decisions (template constraints)

These are **intentional constraints** for production readiness and consistency across future clones:

- Environments: **dev** and **prod** only.
- Flavor differences (for now): **only**:
  - `applicationIdSuffix` (dev only)
  - `BuildConfig.BASE_URL`
- DI: **Hilt** (app module is the composition root).
- Toolchain: **JDK 17** + Gradle wrapper (`./gradlew`).
- Formatting: **Spotless + ktlint** (to be wired into `./gradlew verify`).
- Static analysis: **Detekt** (to be wired into `./gradlew verify`).
- CI baseline: **GitHub Actions** runs `./gradlew verify` on push/PR.

Tracking docs:

- End-to-end backlog: `_WIP/end-to-end-backlog.md`
- Foundation proposal: `_WIP/foundation-engineering-proposal.md`
- Foundation TODO: `_WIP/2026-02-01_foundation_implementation_todo.md`

## Architecture

See `docs/android-architecture-guide.md`.

## Related core kits (local workspace paths)

These repos are used as references while evolving this starter template:

- Backend contracts: `/mnt/c/Development/_CORE/backend-core-kit`
- Flutter mobile core kit (stable reference): `/mnt/c/Development/_CORE/mobile-core-kit`

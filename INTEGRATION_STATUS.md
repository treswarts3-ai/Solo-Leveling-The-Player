# Integration Status

## Current verified checkpoint

- Source: reconstructed and normalized from the latest successful GitHub Actions artifact
- Original recovery repository: `treswarts3-ai/Reddit`
- Recovery branch: `solo-leveling-mod-build-20260710`
- Verified recovery commit: `8fa97122ccf377202a45734d649cb33fbdf5e578`
- Verification: GitHub Actions run 51 completed successfully
- Forge target: 1.20.1 / Forge 47.x / Java 17

## Repository migration state

- Recovery chunks and patch-chain workflow are intentionally excluded from this clean repository.
- The old repository and branch remain the disaster-recovery backup.
- Generated Gradle output is excluded from source control.
- The verified JAR is retained separately as a migration deliverable.

## Planned branch set

- `integration`
- `feature/core-systems`
- `feature/system-ui`
- `feature/quests`
- `feature/abilities`
- `feature/rulers-authority`
- `feature/shadows`
- `feature/shadow-monarch`
- `feature/items-equipment`
- `feature/assets-presentation`

The integration and testing chat owns merges, shared-file changes, CI repairs, and promotion from `integration` to `main`.

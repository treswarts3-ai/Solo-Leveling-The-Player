# Contributing

## Starting work

1. Pull the latest `integration` branch.
2. Create or reset the branch assigned in `FILE_OWNERSHIP.md`.
3. Read `DEVELOPMENT.md` before editing shared systems.
4. Keep the pull request focused on one workstream.

## Before opening a pull request

```bash
./gradlew clean build --stacktrace --no-daemon
```

Also verify that no generated output, logs, archives, or recovery chunks are staged.

## Pull request requirements

Include:

- Features implemented
- Files and shared contracts changed
- Persistent data keys added or changed
- Packets added or changed
- Manual tests performed
- Gradle result
- Known limitations or integration risks

Target `integration`, not `main`. Pull requests should remain drafts until the build is green and the implementation is ready for integration.

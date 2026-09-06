# Project instructions

This repository contains the independent Android/Quest Companion prototype.

- Publish only source, generic documentation and synthetic tests. Never commit real savegames, device identifiers, personal paths, private exports, signing keys or screenshots of real worlds.
- Keep the helper restricted to the fixed RealmCraft save root and explicit numeric world selections. No arbitrary commands or paths over Binder.
- Preserve the game-stop guard, explicit world selection, bounded ZIP validation, atomic snapshot publication and independent library copies.
- This prototype must not modify or restore RealmCraft files. Any future write path needs separately scoped implementation and validation.
- Use English in README, backlog and changelog; provide English/German user-facing guidance where practical.
- Run `./gradlew testDebugUnitTest lintDebug assembleDebug` after implementation changes. Use synthetic-only instrumentation tests for UI changes.
- Before any commit, push or release, inspect exact files, archives and metadata for private content. Use a public account name and GitHub noreply commit address.

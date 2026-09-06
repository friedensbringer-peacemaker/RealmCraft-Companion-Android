# Update log

## 0.1.1 — 2026-09-06

- Prefixed the launcher label with `! ★` to make the Companion easier to find near the top of alphabetically sorted sideloaded apps. Launcher-specific ordering still applies.
- Read the version displayed in the app from build metadata.

## 0.1.0 — 2026-09-06

- Initial independent Android/Quest test project with a resizable native 2D panel.
- Added a synthetic sample, single-world ZIP import, v9 metadata details, file listing and persistent local snapshots.
- Added bounded archive validation, per-file hashes and atomic import completion.
- Added an experimental authorized Shizuku user service for on-device world discovery and read-only snapshot import.
- Added synthetic unit/instrumentation tests and GitHub Actions APK builds.
- Real Quest and phone behavior remains subject to device validation; this is not a full port of the Mac Companion.

## 0.1.2 — 2026-09-06

- Add an explicit GitHub demo download and import action with a pinned SHA-256 checksum, HTTPS redirect restrictions, timeouts and a bounded download.
- Link to the Android GitHub repository and explain snapshot-only inspection in English and German. The offline synthetic sample remains available.

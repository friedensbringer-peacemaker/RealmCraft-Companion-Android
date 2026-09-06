# Update log

## 0.2.0 — 2026-09-06

- Add a read-only surface map with pan, zoom, dimension switching and per-block inspection.
- Add player level, schematic equipped-armor preview and 36-slot inventory with localized names, quantities, durability and enchantment details.
- Verify file checksums before analysis, report unsupported data and bound map work for large snapshots.
- Expand the offline synthetic sample and add parser, integrity and feature-page tests.

## 0.1.3 — 2026-09-06

- Use the larger owner-shared demo snapshot at the existing demo download URL, with 1,025 files and a newly pinned SHA-256 checksum.
- Update the displayed download size to 3.4 MB. Import continues to create an independent copy without replacing existing snapshots.

## 0.1.2 — 2026-09-06

- Add an explicit GitHub demo download and import action with a pinned SHA-256 checksum, HTTPS redirect restrictions, timeouts and a bounded download.
- Link to the Android GitHub repository and explain snapshot-only inspection in English and German. The offline synthetic sample remains available.

## 0.1.1 — 2026-09-06

- Prefixed the launcher label with `! ★` for alphabetically sorted sideloaded apps. Launcher-specific ordering still applies.
- Read the displayed version from build metadata.

## 0.1.0 — 2026-09-06

- Initial independent Android/Quest test project with a resizable native 2D panel.
- Added a synthetic sample, single-world ZIP import, v9 metadata details, file listing and persistent local snapshots.
- Added bounded archive validation, per-file hashes and atomic import completion.
- Added an experimental authorized Shizuku user service for on-device world discovery and read-only snapshot import.
- Added synthetic unit/instrumentation tests and GitHub Actions APK builds.
- Real Quest and phone behavior remains subject to device validation; this is not a full port of the Mac Companion.

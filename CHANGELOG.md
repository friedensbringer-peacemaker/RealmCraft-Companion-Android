# Update log

## 0.6.0 — 2026-09-06

- Add local named markers, favorites and a manual reference position with horizontal distances. Automatic saved player-position decoding remains unavailable.
- Add exact-height cave slices and one-layer navigation; air stays blank.
- Persist viewport, dimension, render settings, filters and large-map controls per world.
- Compare same-world snapshots for inventory quantities, readable chest contents and decoded terrain coverage.
- Add a guided Shizuku import entry and bilingual setup steps, preserving explicit game-stop and world selection.
- Add compact Quest controls, larger touch targets and scrollable dimension tools.
- Add synthetic slice, cache, comparison, notebook and reopen tests; screenshot creation remains paused.

## 0.5.0 — 2026-09-06

- Search loaded map points by block name, ID or readable sign text across both dimensions.
- Find matching items in readable chests with per-stack amounts, total quantities and direct map navigation.
- Report unreadable points and limit displayed search results to 100 with refinement guidance.
- Reuse decoded chunks in a bounded 8 MiB process-local cache after source checksum validation; create and cache map bitmaps only when visible.

## 0.4.0 — 2026-09-06

- Add a large map that fills the app panel, with optional tools and Back to exit.
- Keep zoom and center when resizing, enlarging or switching feature tabs; remember each dimension independently.
- Add direct X/Z navigation and pinch zoom anchored at the gesture position.

## 0.3.0 — 2026-09-06

- Add per-snapshot deletion with confirmation for downloaded, imported and synthetic copies.
- Add block-anchored signs, chest contents, beds and crafting tables with filtered Previous/Next navigation and descriptive details.
- Add persistent render settings for chunk budget, X/Z area, height and optional Kenney Voxel Pack textures.
- Bound record decoding and map work, preserve unsupported-state messages and expand synthetic deletion/point/render tests.

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

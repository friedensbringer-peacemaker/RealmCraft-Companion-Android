# Backlog

Ideas have no promised delivery date.

## Prototype follow-up

- Validate installation, panel resizing, controller/hand focus and coexistence with RealmCraft on Quest 3; repeat on Quest 3S.
- Validate Shizuku startup and permission behavior, actual directory access, reconnect and reboot behavior on real Horizon OS. Test denied permission, disconnected service, cancellation and game restart during import.
- Add progress in bytes, cancellation and preflight storage-space guidance for large worlds.
- Investigate phone-to-Quest ADB over USB and Wi-Fi, keeping explicit device and world selection.
- Add ZIP export of existing snapshots before considering removal or cleanup controls.
- Establish stable release signing and an update path before regular distribution.
- Investigate metadata compatibility with further RealmCraft versions; never infer unsupported offsets.
- Port maps, material guides, inventory/chest readers and remaining Companion functionality incrementally.

## Deferred

Restore/editing, live position/inventory synchronization, automatic background imports and store submission require separate design and validation. Do not assume that a 2D APK grants access to another app's storage.

## Demo download follow-ups

- Support additional explicitly published demo worlds through a reviewed versioned catalog.
- Map rendering, point search and readable chest-item search are available as of 0.5.0.

## Android 0.2 follow-up

- Stream chunk analysis progressively beyond the bounded overview and investigate a versioned disk cache. Viewport retention, visible-tile bitmap creation and bounded process-local decode caching are available as of 0.5.0.
- Add verified player position, exact-slice cave rendering and inventory item icons. Selected-height surfaces and chest inspection are available in 0.3.0.
- Decode additional player layouts and personal skins only when validated.

## Follow-ups after 0.6.0

- Validate an automatic saved player-position decoder against known records; manual reference coordinates are available now and are never labeled as player data.
- Validate the guided Shizuku flow on a real authorized service, including reconnect and permission denial; no real worlds are imported in automated tests.
- Visualize terrain differences directly on the map. Per-chest quantity deltas are available in 0.7.0.
- Add progressive chunk streaming and a versioned disk cache.
- Extend notebook import/export and multi-world management.

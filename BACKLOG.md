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
- Add map rendering for imported worlds; current Android snapshots expose metadata and files only.

## Android 0.2 follow-up

- Stream larger map regions beyond the current bounded overview and retain the map viewport across navigation.
- Add verified player position, selected-height cave/Nether layers, chest inspection and item icons.
- Decode additional player layouts and personal skins only when validated.

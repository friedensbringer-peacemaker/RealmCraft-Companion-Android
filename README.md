# RealmCraft Companion for Android & Quest

A small, independent Android prototype for testing a Companion window on Meta Quest and Android phones. This repository is separate from the [macOS Companion](https://github.com/friedensbringer-peacemaker/RealmCraft-Companion) because its UI, toolchain and device permissions differ.

**Status: 0.3.0 experimental.** This includes read-only maps, player equipment and inventory for imported copies; it is not the complete Mac Companion. It is not affiliated with Tellurion Mobile or Meta.

## Try the APK

Download the APK from [Releases](https://github.com/friedensbringer-peacemaker/RealmCraft-Companion-Android/releases). The first APK is a debug-signed test build for sideloading, not a store release.

- **Android phone:** open the downloaded APK and allow installation from that source when Android asks.
- **Quest:** enable developer mode and authorize USB debugging, then install with SideQuest or `adb install -r RealmCraft-Companion-Android-0.3.0.apk`. Open **! ★ RealmCraft Companion Lab** from the headset's installed/unknown-source apps. The exact launcher label depends on Horizon OS.
- Generate a synthetic ZIP for testing the document picker with `python3 tools/create_test_zip.py synthetic-test-world.zip`. It contains placeholder data and must never be restored into the game. Use the built-in sample for the map/player preview.
- Choose **Try synthetic sample** / **Synthetische Testwelt öffnen** first. No game data or Shizuku is needed for this test.
- Resize the panel, inspect the sample, close the Companion and reopen it. The sample should remain in the local library.

The app follows the device language for English/German. See the [device test guide](docs/TESTING.md).

## What works in the prototype

- Native, resizable 2D Android activity with large controls, usable on a phone or a Quest panel.
- Built-in **synthetic, non-playable** test terrain and player records. The owner-shared real demo downloads only after tapping the demo action; no real savegame is embedded in the APK.
- Import one world's ZIP using the system document picker. Accepts a world at the ZIP root or inside a nested folder; multiple-world archives are rejected with an explanation.
- Store an independent snapshot in the Companion's own private files directory, inspect v9 world metadata and a bounded file listing, and reopen imports after restart.
- Calculate per-file SHA-256 and a deterministic manifest digest. Verify ZIP integrity, reject unsafe paths, enforce entry/size limits and avoid replacing existing snapshots.
- Experimental **on-device Shizuku** integration: explicitly stop RealmCraft, list its worlds, select one and stream a ZIP into the Companion library. Source files are opened for reading only.

## Direct access experiment

Install and start [Shizuku](https://shizuku.rikka.app/guide/setup/) separately on the **Quest itself**, grant the Companion access and select **Connect Shizuku**. Save and close RealmCraft, then use **Find worlds on this device**. The app shows names, IDs, seeds, dates and sizes before selection.

This tests whether that Horizon OS version allows an ADB-backed user service to read the game's external app-specific directory. Compatibility is **not established by compilation or emulator tests**. Permission failures are shown in the app; no root requirement or permission bypass is assumed. A non-root Shizuku service generally needs restarting after reboot.

Running Shizuku on a **phone** grants access on that phone; it does not connect to the Quest. Phone-to-Quest USB/Wi-Fi ADB is future work. A phone can already inspect an exported ZIP.

The helper is restricted to the fixed RealmCraft world root and numeric world selections. It exposes no arbitrary shell/path API. RealmCraft is stopped before the scan and again before copying. The helper checks for source changes and whether the game is running before completing the archive. Keep RealmCraft closed throughout import; after completion the snapshot can be viewed beside the game. It is not live data.

## Limits

No game editing, restore, automatic sync or phone-to-Quest transfer yet. Archive input and expanded content are each limited to 1 GiB, with at most 100,000 entries. Import temporarily needs room for both archive and extracted files. Only the observed version-9 metadata layout is accepted. Unsupported worlds remain unmodified.

The app requests internet permission only for the explicitly tapped GitHub demo download. It requests no broad-storage permission and contains no analytics or upload code. Opening the setup guide delegates to the browser. Android backup is disabled for the app. Uninstalling the app removes its private snapshots, so retain original backups externally. Debug signing identities can differ between local and CI builds; Android may reject an in-place update signed by a different key. Stable release signing is future work.

## Build

Use JDK 17, Android SDK platform 35 and build-tools 35.0.0. Gradle 8.11.1 and Android Gradle Plugin 8.9.2 are pinned.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Open the repository in Android Studio or configure `ANDROID_HOME` locally. Never commit local SDK paths, signing keys, imported saves or private exports. GitHub Actions builds and tests from source and uploads a debug APK plus reports. Instrumentation uses only the synthetic sample.

## Architecture

- `MainActivity`: native panel, document picker, library and Shizuku connection.
- `core/SnapshotStore`: platform-independent ZIP validation, metadata decoding and immutable import storage.
- `WorldAccessService` / `IWorldAccess`: small privileged read-only transport, separately authorized through Shizuku.
- `docs/TESTING.md`: phone/Quest test procedure and observed validation status.

## References

- [Meta: Android app manifest and panels](https://developers.meta.com/horizon/documentation/android-apps/create-app/)
- [Meta: multitasking and focus](https://developers.meta.com/horizon/resources/vrc-quest-input-4/)
- [Android: all-files access limitations](https://developer.android.com/training/data-storage/manage-all-files)
- [Shizuku API and user services](https://github.com/RikkaApps/Shizuku-API)

MIT licensed. See [LICENSE](LICENSE) and [third-party notices](THIRD-PARTY-NOTICES.md).

## Downloadable demo

Tap **Download RealmCraft Companion Demo** to fetch the owner-shared world from [GitHub](https://github.com/friedensbringer-peacemaker/RealmCraft-Companion-Android/releases/tag/demo-world-v1). The app pins the ZIP SHA-256, limits the download to 4 MiB and imports through the existing bounded ZIP validator. Internet is required; Shizuku is not. The imported copy supports map, player, inventory and metadata inspection. It never restores anything into RealmCraft. The separate synthetic sample still works offline.

## Map, player and inventory

After importing, choose **Map · Player · Inventory** on a snapshot or in its details dialog. The offline synthetic sample also includes a small terrain and player fixture (not a playable world).

- **Map:** pan, zoom, fit, switch Overworld/Nether and tap a block for coordinates, top height and block name. Colors are schematic. Only stored chunks are shown; Nether includes its roof.
- **Player:** observed saved level when unambiguous, a schematic equipment avatar and four armor slots. This does not reconstruct a personal skin, username or live player position.
- **Inventory:** 36 slots, hotbar labels, localized names and quantities. Item details expose stored durability and enchantment IDs when present. Unknown item IDs remain visible.
- **Details:** world identity, seed, import time and snapshot manifest checksum.

The readers support the observed v9 block layout and v2 player/v1 item layout. Every inspected file is checked against its snapshot checksum. Unreadable player data and skipped chunks are reported. Map settings select a 256/1,024/4,096/16,384 chunk budget, with 64/256/512 MiB input caps. Point indexing is capped at 5,000 points (4,096 per chunk); sign text is limited to 2,048 UTF-8 bytes. Reaching a limit is reported. Imported copies are never edited. Re-import to see newer game progress.

## Local deletion and detailed maps

Every library entry, including the synthetic sample, has **Delete copy…**. Confirming removes that entry and all of its files from Companion. Other snapshots and the RealmCraft game folder are untouched. A fresh import or demo download creates a new copy. Bundled textures are part of the APK; choose the color mode to stop displaying them.

**Map settings** persist on this device: select Companion colors or Kenney Voxel Pack textures, chunk budget, square map area around X/Z, and highest rendered Y. Textures appear at close zoom; unmatched blocks retain colors. Kenney artwork is independent CC0 artwork also supported by the Mac Companion, not original RealmCraft graphics. See third-party notices.

Filter the point navigator to signs, chests, beds or crafting tables. Previous/Next centers each point; tap a marker or open Point details for its coordinates, inscription or chest slots. Points include underground locations regardless of the selected surface height. Nearby markers may overlap; the navigator still reaches every indexed point. Bed halves are separate saved blocks. Unsupported/missing sign or chest records are shown as unavailable, never as empty.

### Large map

Open a snapshot, select Map, then **Large map**. The map fills the app panel; **Show tools** reveals dimensions, settings, coordinate navigation and point filters. Zoom and Fit remain available with tools hidden. Use **Small map** or Android Back to return. The map keeps its center and zoom when the panel resizes or when returning from another feature tab. **Go to X/Z** centers on a location within the saved map; excluded areas remain blank.

### Search and map performance

Use **Search points** for block names, IDs and readable sign text, or **Find chest items** for item names and IDs. Both searches cover both dimensions within the loaded area. Results open their details and center the map. Item search reports matching stack quantities and totals; unknown chest contents are excluded and explicitly counted. Only the first 100 results are displayed; refine the query for more specific matches. Render and point limits also limit search coverage.

Decoded chunks use an 8 MiB process-local cache keyed by verified file checksum, coordinates and height. Source files are still checked on every load; the cache does not survive process termination. Map bitmaps are created on visibility and retained in a 4,096-tile LRU cache (4 MiB of pixel data).

### Exploration tools (0.6.0)

Map → **More** opens local markers/favorites, a manual reference position, snapshot comparison, layer stepping and compact Quest layout. Markers can be named, edited, favorited and deleted; up to 200 are stored per world and shared across its imported copies. They are Companion annotations, not game blocks. Manual reference distances are horizontal straight-line distances in the same dimension. Automatic saved player-position decoding is unavailable.

In **Settings**, enable **Exact layer / caves** and choose Y. Air on that layer stays blank instead of showing the surface below. **Layer down/up** changes Y by one. Point markers still include all heights and their details show the actual Y. Map viewport, filters, dimensions, render settings and large-map controls are restored per world after reopening. World identity uses the saved world ID and seed.

**Compare snapshots** selects another imported copy of the same world as the baseline and compares it to the open copy with identical render settings. Results summarize inventory quantity changes, readable chest-content changes and changed columns in shared coverage. One-sided chunks and unknown containers are reported separately; missing coverage does not establish removed terrain. Armor and inventory durability changes are not included in the inventory quantity summary.

**Import from this Quest / device** guides connection to an already running Shizuku service and continues to explicit game-stop confirmation and world selection. Shizuku still needs separate installation/startup. ZIP import remains available without it. The app only creates new local snapshots.

Deutsch: Unter **Karte → Mehr** findest du Markierungen/Favoriten, eine manuelle Referenzposition, den Spielstandsvergleich, Ebenenschritte und die kompakte Ansicht. Markierungen und Kartenansichten werden pro Welt gespeichert. Unter **Einstellungen → Exakte Ebene / Höhlen** bleibt Luft leer; Punkte berücksichtigen weiterhin alle Höhen. Vergleiche beziehen sich auf die gewählte Kopie → aktuelle Kopie und nur auf lesbare, geladene Daten. Automatische Spielerkoordinaten sind nicht verfügbar. **Von dieser Quest / diesem Gerät importieren** führt über Shizuku-Verbindung, Beenden-Bestätigung und Weltauswahl zu einer neuen Kopie.

### Planning tools (0.7.0)

**Map → More → Packing list** stores up to 100 item targets per world. Choose an item from readable chest stock or enter its ID, then set a target quantity. Available and missing quantities refer only to readable chests in loaded coverage, across both dimensions; player inventory is excluded. Unknown contents and render limits remain explicit. Select a target to find matching chests, change its quantity or remove it.

**Map bookmarks** stores up to 30 named views per world, including center, zoom, dimension, render settings, height mode, textures and point filter. Open a bookmark to render its view again, or delete it. Existing local markers remain separate.

Snapshot comparisons now list up to 100 changed chests with coordinates and item quantity deltas. A property-only change is labeled when quantities remain equal. Missing or unreadable containers still do not imply removed or empty chests.

Deutsch: **Karte → Mehr → Packliste** verwaltet Zielmengen je Gegenstand, zeigt den lesbaren Truhenbestand und fehlende Mengen und öffnet die Truhensuche. **Karten-Lesezeichen** speichert benannte Ausschnitte samt Ebene, Darstellung und Filter. Der Spielstandsvergleich nennt geänderte Truhen mit Koordinaten und Mengenänderungen. Alles bezieht sich auf geladene, lesbare Kopien und verändert keine Spieldateien.

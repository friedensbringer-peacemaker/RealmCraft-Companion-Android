# RealmCraft Companion for Android & Quest

A small, independent Android prototype for testing a Companion window on Meta Quest and Android phones. This repository is separate from the [macOS Companion](https://github.com/friedensbringer-peacemaker/RealmCraft-Companion) because its UI, toolchain and device permissions differ.

**Status: 0.1.0 experimental.** This is a metadata and file-import test app, not the complete Mac Companion. It is not affiliated with Tellurion Mobile or Meta.

## Try the APK

Download the APK from [Releases](https://github.com/friedensbringer-peacemaker/RealmCraft-Companion-Android/releases). The first APK is a debug-signed test build for sideloading, not a store release.

- **Android phone:** open the downloaded APK and allow installation from that source when Android asks.
- **Quest:** enable developer mode and authorize USB debugging, then install with SideQuest or `adb install -r RealmCraft-Companion-Android-0.1.0.apk`. Open **RealmCraft Companion Lab** from the headset's installed/unknown-source apps. The exact launcher label depends on Horizon OS.
- The release also includes a synthetic ZIP for testing the document picker. It contains placeholder data and must never be restored into the game. Recreate it with `python3 tools/create_test_zip.py synthetic-test-world.zip`.
- Choose **Try synthetic sample** / **Synthetische Testwelt öffnen** first. No game data or Shizuku is needed for this test.
- Resize the panel, inspect the sample, close the Companion and reopen it. The sample should remain in the local library.

The app follows the device language for English/German. See the [device test guide](docs/TESTING.md).

## What works in the prototype

- Native, resizable 2D Android activity with large controls, usable on a phone or a Quest panel.
- Built-in **synthetic, non-playable** test metadata. No real savegames are distributed.
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

No map renderer, inventory/chest decoding, editing, restore, automatic sync or phone-to-Quest transfer yet. Archive input and expanded content are each limited to 1 GiB, with at most 100,000 entries. Import temporarily needs room for both archive and extracted files. Only the observed version-9 metadata layout is accepted. Unsupported worlds remain unmodified.

The app requests no internet or broad-storage permission and contains no analytics or upload code. Opening the setup guide delegates to the browser. Android backup is disabled for the app. Uninstalling the app removes its private snapshots, so retain original backups externally. Debug signing identities can differ between local and CI builds; Android may reject an in-place update signed by a different key. Stable release signing is future work.

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

# Prototype test guide / Prototyp testen

The Android prototype reads copies and never restores files to RealmCraft. The generated sample contains synthetic metadata and a placeholder chunk; it is **not a playable world**. Direct Quest access through Shizuku and use beside RealmCraft require a physical-device test; this guide does not claim those tests have passed.

## Validation status

- Local APK and instrumentation-APK compilation passed.
- 13 synthetic JVM tests passed; Android Lint completed successfully.
- APK v2 signature verified.
- Emulator UI/persistence validation is run by GitHub Actions; consult the workflow result for the exact commit.
- Physical Quest/phone and Shizuku directory access: not yet tested.

## Automated checks

Use a disposable Android emulator for instrumentation. No Quest or real savegame is needed.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

The JVM suite covers metadata parsing, nested ZIP imports, multiple-world rejection, path safety, duplicate names, archive truncation, actual import limits, input failures and independent persisted snapshots. The framework instrumentation test opens the activity, creates the synthetic sample, checks its library entry, dismisses details and relaunches the activity to check persistence. It uses Android's built-in test runner without AndroidX.

A successful build or emulator test does not establish that Horizon OS permits the Shizuku bridge or that RealmCraft behaves correctly during multitasking.

## Deutsch

### APK und lokale Testwelt

1. Debug-APK auf einem Testgerät installieren und den Companion starten.
2. **Synthetische Testwelt öffnen** auswählen. Erwartet: ein neuer Bibliothekseintrag und ein Detaildialog mit dem Hinweis, dass die Welt nicht spielbar ist.
3. Dialog schließen, App verlassen und neu öffnen. Der Eintrag muss erhalten bleiben.
4. Die Testwelt erneut öffnen. Es muss ein zweiter, unabhängiger Eintrag entstehen.
5. Display drehen beziehungsweise das Quest-Fenster verkleinern und vergrößern. Texte und Schaltflächen müssen erreichbar bleiben.

### ZIP und Fehlerfälle

1. Eine ausschließlich synthetische ZIP mit genau einer gültigen Version-9-Datei `world_data` über **Welt-ZIP importieren** auswählen. Unterordner sind erlaubt.
2. Name, Seed, Welt-ID, Dateianzahl und Dateiliste mit der synthetischen Quelle vergleichen.
3. ZIP-Auswahl abbrechen: Es darf kein neuer Eintrag entstehen.
4. Eine beschädigte ZIP, eine ZIP ohne `world_data` und eine ZIP mit zwei Welten ausprobieren. Erwartet: verständlicher Fehler, kein unvollständiger Bibliothekseintrag, danach weiterhin bedienbare App.
5. Import starten und die App schließen. Danach erneut starten. Es darf nur ein vollständig veröffentlichter Snapshot oder kein neuer Snapshot erscheinen.

### Optional: Shizuku direkt auf der Quest

1. Zunächst einen eigenen, rückspielbaren Backup-Stand außerhalb dieser App anlegen. Shizuku gemäß dessen offizieller Anleitung installieren und starten.
2. RealmCraft im Spiel speichern und schließen. **Shizuku verbinden** auswählen und die erforderliche Freigabe erteilen.
3. **Welten auf diesem Gerät suchen** auswählen. Den Hinweis zum Beenden lesen und bestätigen. Alle gefundenen Welten müssen mit Name, ID, Seed, Datum und Größe angezeigt werden.
4. Genau eine Welt auswählen und den Kopierimport bestätigen. RealmCraft bis zum Abschluss geschlossen lassen.
5. Den neuen Snapshot prüfen. RealmCraft wieder starten und den Companion als 2D-Fenster daneben öffnen. Fokuswechsel, Controller-/Handbedienung, Lesbarkeit, mögliche Spielpause und Rückkehr ins Spiel beobachten.
6. Das Spiel erneut speichern: Der bereits importierte Snapshot muss unverändert bleiben. Ein neuer Import erzeugt eine neue Kopie.
7. Shizuku beenden oder dessen Freigabe entziehen und erneut verbinden/importieren. Erwartet: Fehlerhinweis; bestehende Kopien bleiben lesbar. Nach einem Quest-Neustart prüfen, ob Shizuku erneut aktiviert werden muss.

Auf einem Handy prüft der Shizuku-Helfer das Handy selbst. Eine per USB oder WLAN verbundene Quest ist in diesem Prototyp kein Importziel.

## English

### APK and local sample

1. Install the debug APK on a test device and open Companion.
2. Select **Try synthetic sample**. Expect a new library entry and a detail dialog clearly marking the world as non-playable.
3. Dismiss the dialog, leave the app and reopen it. The entry must persist.
4. Create the sample again. Expect a second independent snapshot.
5. Rotate the display or resize the Quest window. Text and controls must remain accessible.

### ZIP imports and failures

1. Select **Import world ZIP** and choose an entirely synthetic archive containing one valid version-9 `world_data`. Nested folders are supported.
2. Compare the displayed name, seed, world ID, file count and paths with the synthetic source.
3. Cancel the document picker. No new snapshot should appear.
4. Try a damaged ZIP, an archive without `world_data`, and an archive containing two worlds. Expect a clear error, no incomplete library entry, and usable controls afterward.
5. Close the app during import and reopen it. The library must contain either a fully published snapshot or no new snapshot.

### Optional: Shizuku on Quest

1. First keep an independently restorable backup outside this app. Install and start Shizuku following its official instructions.
2. Save and close RealmCraft in-game. Select **Connect Shizuku** and grant access.
3. Select **Find worlds on this device**, read the stop-game prompt and continue. Check that all discovered worlds show name, ID, seed, date and size.
4. Select one world and confirm its copy import. Keep RealmCraft closed until completion.
5. Inspect the new snapshot, restart RealmCraft, and open Companion beside it as a 2D window. Check input focus, controller/hand interaction, readability, game pausing and returning to play.
6. Save the game again. The existing snapshot must stay unchanged; a fresh import creates a new copy.
7. Stop Shizuku or revoke authorization and retry connection/import. Expect an error while existing snapshots remain readable. After restarting Quest, check whether Shizuku needs activation again.

On a phone, the Shizuku helper reads that phone. Importing from a Quest connected over USB or Wi-Fi is outside this prototype.

## Recording results

Record app version, OS version, device model, test step, expected result and observed result. Keep real world names, identifiers, seeds, device serials, local paths and screenshots out of public issues. Reproduce failures using the synthetic sample wherever possible.

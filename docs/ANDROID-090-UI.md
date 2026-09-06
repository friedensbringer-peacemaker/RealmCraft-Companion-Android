# Android 0.9.0 Quest UI

Reference: macOS origin/main 5b1dbce, ItemIcons.swift and PixelItemIcons.json. Android remains an independent read-only snapshot prototype. Web was outside this update.

| Area | macOS reference | Android 0.9.0 | Status |
| --- | --- | --- | --- |
| Item artwork | Version-pinned Kenney and Pixel Perfection mappings | 686 Pixel mappings bundled, Kenney fallback, explicit ID tile | Adapted for offline Quest use |
| Map and POI inspection | Map with contextual information | Default large map, pictograms, labels, direct side dialog | Adapted to native Android |
| Inventory | Named items with artwork | Responsive grid, quantities and item details | Adapted |
| Automatic position | Unsupported in checked reference | Manual reference retained | Known limitation |
| Web | Not inspected | No web changes | Unverified |

## English guide

The map opens large automatically. Use Map to switch to player, inventory or snapshot details. Search finds points or chest items; Tools contains markers, routes, notebooks and exports. Tools also selects Pixel Perfection or Kenney item artwork. Settings controls rendered area, layer, texture style and streaming. Drag the terrain to load neighboring saved chunks.

Tap a pictogram to read the sign or chest contents. Previous/Next browses the current point category. Wide windows place the details at the right; close it to use the whole map. Points include underground blocks. A missing record is reported as unknown, never empty. Pixel artwork is illustrative; unmapped IDs have labeled tiles.

## Deutsche Hilfe

Die Karte öffnet automatisch groß. Über Karte zu Spieler, Inventar oder Details wechseln. Suchen findet Punkte oder Truheninhalte; Werkzeuge enthält Markierungen, Routen, Notizen und Exporte. Unter Werkzeuge lässt sich Pixel Perfection oder Kenney für Itemgrafiken wählen. Einstellungen steuert Renderbereich, Ebene, Texturstil und Nachladen. Ziehen lädt benachbarte gespeicherte Chunks nach.

Ein Piktogramm antippen, um Schildertext oder Truheninhalt zu sehen. Zurück/Weiter schaltet innerhalb der Punktkategorie durch. In breiten Fenstern stehen Details rechts; Schließen gibt die ganze Karte frei. Punkte umfassen unterirdische Blöcke. Fehlende Datensätze bleiben unbekannt. Pixelgrafiken sind illustrative Darstellungen; nicht zugeordnete IDs erscheinen als beschriftete Kacheln.

## Validation scope

Synthetic core and Android instrumentation tests cover map geometry, camera persistence, direct chest selection, visible item drawables, searches and streamed panning. No new screenshots are produced. User-worn optical readability, every Horizon panel size and hand/controller comfort require user feedback; successful automated tests do not establish perfection.

Developed with Codex; AI contributor: Codex Astra.

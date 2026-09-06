# Read-only feature implementation

The Java chunk reader ports the four-channel RLE decoder from the MIT-licensed macOS Companion MapEngine (`realmcraft_map/chunks.py`). Top surfaces use the same air IDs and coordinate ordering. Schematic colors and bilingual item names originate from the same public Companion source. No game textures or user-specific artwork are included.

The player reader ports `PlayerModels.swift`: bounded input, unique inventory anchor, structured item markers, duplicate-slot rejection, optional durability/enchantments and conservative experience decoding. The avatar follows the original schematic equipment preview, not a personal skin.

`SnapshotAnalysis` checks relative paths, rejects symlinks, verifies the manifest digest and checks each requested file hash. It reads one bounded chunk at a time and stores only 256 surface samples per chunk. Separate small bitmaps avoid allocating a full-world image for sparse coordinates. Unsupported chunks are counted; unsupported player data is explained.

Validation uses synthetic fixtures for slots, equipment, experience ambiguity, terrain orientation, truncation, malformed runs, checksum changes and symlinks. Android instrumentation navigates all pages using synthetic snapshots. Real demo regression data is kept out of the source and test artifacts.

## Detailed map readers

Sign records follow `realmcraft_map/signs.py`; chest quantity/slot decoding follows `realmcraft_map/chests.py`. Both require matching decoded block locations, bounded records and unambiguous coordinates. Item extras in chest records are not interpreted. Bed and crafting points come directly from decoded blocks. The map may include points above or below the chosen height; details explain this. Duplicate or unsupported records never look empty.

Snapshot deletion walks only the selected UUID directory without following links. Render settings have hard upper bounds, and the activity cancels background work when closed. Optional textures use the Mac Companion mapping and the SHA-256-verified Kenney Voxel Pack 1.0 archive; bundled originals and license notices are kept.

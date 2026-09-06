# Read-only feature implementation

The Java chunk reader ports the four-channel RLE decoder from the MIT-licensed macOS Companion MapEngine (`realmcraft_map/chunks.py`). Top surfaces use the same air IDs and coordinate ordering. Schematic colors and bilingual item names originate from the same public Companion source. No game textures or user-specific artwork are included.

The player reader ports `PlayerModels.swift`: bounded input, unique inventory anchor, structured item markers, duplicate-slot rejection, optional durability/enchantments and conservative experience decoding. The avatar follows the original schematic equipment preview, not a personal skin.

`SnapshotAnalysis` checks relative paths, rejects symlinks, verifies the manifest digest and checks each requested file hash. It reads one bounded chunk at a time and stores only 256 surface samples per chunk. Separate small bitmaps avoid allocating a full-world image for sparse coordinates. Unsupported chunks are counted; unsupported player data is explained.

Validation uses synthetic fixtures for slots, equipment, experience ambiguity, terrain orientation, truncation, malformed runs, checksum changes and symlinks. Android instrumentation navigates all pages using synthetic snapshots. Real demo regression data is kept out of the source and test artifacts.

#!/usr/bin/env python3
"""Generate a deterministic, synthetic ZIP for testing the document-picker import."""
import argparse
import struct
from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED


def create(destination: Path):
    name = b"Synthetic test world"
    metadata = b"\x09" + struct.pack(">I", 42) + b"\0" * 4 + struct.pack(">iI", 12345, len(name)) + name + b"\0" * 105
    entries = {
        "42/world_data": metadata,
        "42/SYNTHETIC-NOT-PLAYABLE.txt": b"Generated test metadata only. This is not a playable savegame. Never restore it to RealmCraft.\n",
        "42/o.0,0": b"SYNTHETIC CHUNK PLACEHOLDER - NOT PLAYABLE\n",
    }
    with ZipFile(destination, "x") as archive:
        for path, contents in sorted(entries.items()):
            info = ZipInfo(path, (1980, 1, 1, 0, 0, 0))
            info.compress_type = ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, contents)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path, help="New output ZIP (existing files are not overwritten)")
    create(parser.parse_args().output)

#!/usr/bin/env python3
"""Import approved, offline CARTE salon recordings at Android build time.

The binary archive is delivered separately from text-only GitHub edits.
Run from the project root. Every entry must be a supported Ogg resource name.
No network downloads, external codecs, unpacking outside res/raw or surprises.
"""
from pathlib import Path
from zipfile import ZipFile, BadZipFile
import sys

ARCHIVE = Path("tools/audio/CARTE-Salon-Audio.zip")
DEST = Path("app/src/main/res/raw")
NAMES = {
    "salon_jazz.ogg",
    "salon_fireplace.ogg",
    "salon_card_deal.ogg",
    "salon_card_play.ogg",
    "salon_card_collect.ogg",
    "salon_conversation.ogg",
    "salon_glassware.ogg",
    "salon_door.ogg",
    "salon_chair.ogg",
    "salon_footsteps.ogg",
    "salon_applause.ogg",
}

def main() -> None:
    if not ARCHIVE.exists():
        print("CARTE AUDIO: archive absent; optional recordings are not in this APK.")
        return
    try:
        with ZipFile(ARCHIVE) as z:
            entries = [info for info in z.infolist() if not info.is_dir()]
            names = [info.filename for info in entries]
            if len(names) != len(set(names)):
                raise ValueError("duplicate audio resource names")
            if not set(names).issubset(NAMES):
                raise ValueError("unknown entry or unsafe archive path: " + str(sorted(set(names) - NAMES)))
            if "salon_jazz.ogg" not in names:
                raise ValueError("approved jazz loop salon_jazz.ogg is required")
            payloads = {}
            for info in entries:
                if not 128 < info.file_size < 30_000_000:
                    raise ValueError(f"invalid size for {info.filename}: {info.file_size}")
                data = z.read(info)
                if not data.startswith(b"OggS"):
                    raise ValueError(f"not an Ogg sound: {info.filename}")
                payloads[info.filename] = data
            DEST.mkdir(parents=True, exist_ok=True)
            for name, data in payloads.items():
                (DEST / name).write_bytes(data)
            print(f"CARTE AUDIO: imported {len(payloads)} files: " + ", ".join(sorted(payloads)))
    except (BadZipFile, ValueError) as error:
        print("CARTE AUDIO: INVALID BUNDLE - " + str(error), file=sys.stderr)
        raise SystemExit(1)

if __name__ == "__main__":
    main()

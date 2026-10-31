#!/usr/bin/env python3
"""Import genuine Kenney CC0 card foley, only where no approved sample exists.

Original source: https://kenney.nl/assets/casino-audio
Archive mirror: https://opengameart.org/content/54-casino-sound-effects-cards-dice-chips
Licence: CC0 1.0. Download is a BUILD-TIME step: Android never uses network.
"""
import hashlib
from io import BytesIO
from pathlib import Path
from urllib.request import Request, urlopen
from zipfile import ZipFile, BadZipFile

DEST = Path("app/src/main/res/raw")
URL = "https://opengameart.org/sites/default/files/kenney_casino-audio.zip"
CHOICES = {
    "salon_card_deal.ogg": "card-shuffle.ogg",
    "salon_card_play.ogg": "card-place-2.ogg",
    "salon_card_collect.ogg": "card-shove-4.ogg",
}
MAX_ARCHIVE = 4_000_000


def main():
    required = {out: source for out, source in CHOICES.items()
                if not (DEST / out).exists()}
    if not required:
        print("KENNEY CARDS: three user-provided recordings already present")
        return

    req = Request(URL, headers={"User-Agent": "CARTE-Android-AudioBuilder/1.0"})
    with urlopen(req, timeout=35) as response:
        declared = response.headers.get("Content-Length")
        if declared is not None and int(declared) > MAX_ARCHIVE:
            raise ValueError("Audio pack too large")
        raw = response.read(MAX_ARCHIVE + 1)
    if len(raw) > MAX_ARCHIVE or len(raw) < 25_000:
        raise ValueError("Unexpected audio pack size")

    digest = hashlib.sha256(raw).hexdigest()
    print("KENNEY CARDS: source sha256=" + digest)
    with ZipFile(BytesIO(raw)) as archive:
        for out_name, source_name in required.items():
            candidates = [info for info in archive.infolist()
                          if not info.is_dir() and info.filename.split("/")[-1] == source_name]
            if len(candidates) != 1:
                raise ValueError(f"Missing or duplicate approved source {source_name}")
            data = archive.read(candidates[0])
            if not data.startswith(b"OggS") or not 500 < len(data) < 2_000_000:
                raise ValueError(f"Invalid Ogg sample {source_name}")
            DEST.mkdir(parents=True, exist_ok=True)
            (DEST / out_name).write_bytes(data)
            print(f"KENNEY CARDS: {out_name} <- {source_name}, {len(data)} bytes")


if __name__ == "__main__":
    main()

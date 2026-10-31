#!/usr/bin/env python3
"""Fetch an already loopable real fireplace recording from an acknowledged CC0 source.

Original: fireplace by inchadney (Freesound, CC0)
Loop rehost/credits: https://github.com/Muges/ambientsounds
No network calls are made by the Android application.
An explicitly bundled salon_fireplace.ogg takes precedence.
"""
from pathlib import Path
from urllib.request import Request, urlopen
import hashlib

OUTPUT = Path("app/src/main/res/raw/salon_fireplace.ogg")
SOURCE = "https://raw.githubusercontent.com/Muges/ambientsounds/master/fireplace.ogg"
MAX_SIZE = 5_000_000


def main():
    if OUTPUT.exists():
        print("RECORDED FIREPLACE: using supplied recording")
        return
    request = Request(SOURCE, headers={"User-Agent": "CARTE-Android-AudioBuilder/1.0"})
    with urlopen(request, timeout=35) as response:
        declared = response.headers.get("Content-Length")
        if declared is not None and int(declared) > MAX_SIZE:
            raise ValueError("Fireplace source too large")
        data = response.read(MAX_SIZE + 1)
    if not 30_000 < len(data) <= MAX_SIZE or data[:4] != b"OggS":
        raise ValueError("Invalid fireplace Ogg source")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_bytes(data)
    print(f"RECORDED FIREPLACE: {len(data)} bytes, sha256={hashlib.sha256(data).hexdigest()}")


if __name__ == "__main__":
    main()

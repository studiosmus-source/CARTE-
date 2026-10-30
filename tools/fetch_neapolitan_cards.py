#!/usr/bin/env python3
"""Download the 40 public-domain Neapolitan card scans from Wikimedia Commons.

Run from repository root: python3 tools/fetch_neapolitan_cards.py
The resulting app/src/main/assets/cards/*.jpg files are bundled in the APK.
Requires only Python standard library and network access at asset-preparation time.
"""
from pathlib import Path
from urllib.parse import quote
from urllib.request import Request, urlopen
import time

OUT = Path(__file__).resolve().parents[1] / "app/src/main/assets/cards"
SUITS = ("denari", "coppe", "spade", "bastoni")
NAMES = ("Asso", "Due", "Tre", "Quattro", "Cinque", "Sei", "Sette", "Otto", "Nove", "Dieci")

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for suit_index, suit in enumerate(SUITS):
        for number, name in enumerate(NAMES, start=1):
            destination = OUT / f"{suit}_{number:02d}.jpg"
            if destination.is_file() and destination.stat().st_size > 10000:
                continue
            index = suit_index * 10 + number
            filename = f"{index:02d}_{name}_di_{suit}.jpg"
            if index == 40:
                filename = "40_Dieci_di_Bastoni.jpg"
            url = "https://commons.wikimedia.org/wiki/Special:Redirect/file/" + quote(filename)
            req = Request(url, headers={"User-Agent": "CARTE-Android-AssetPrep/1.0 (Wikimedia Commons public domain)"})
            for attempt in range(3):
                try:
                    with urlopen(req, timeout=40) as response:
                        data = response.read()
                    if not data.startswith(bytes((0xFF, 0xD8, 0xFF))):
                        raise ValueError(f"Not a JPEG: {filename}")
                    destination.write_bytes(data)
                    print(f"{index:02d}/40 {destination.name} ({len(data)} bytes)")
                    break
                except Exception:
                    if attempt == 2:
                        raise
                    time.sleep(2 * (attempt + 1))
    files = list(OUT.glob("*.jpg"))
    assert len(files) >= 40, f"Expected 40 cards, found {len(files)}"
    print("40 card images ready for offline packaging.")

if __name__ == "__main__":
    main()

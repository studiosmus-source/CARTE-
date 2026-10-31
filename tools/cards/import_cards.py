#!/usr/bin/env python3
"""Install the approved 40-card napoletane deck without altering JPEG images.

CARTE-usura-parte-1.zip / parte-2.zip are two upload-sized fragments of
CARTE-HD-usura-intensa.zip supplied by the game's creator, split losslessly.
The hashes lock the approved asset set. If not uploaded, preserve the legacy
card importer rather than accidentally packaging an incomplete deck.
"""
from hashlib import sha256
from pathlib import Path
from zipfile import ZipFile
import subprocess

CARDS = Path("app/src/main/assets/cards")
OLD = Path("app/src/main/assets/CARTE-HD-assets.zip")
PARTS = {
    Path("tools/cards/CARTE-usura-parte-1.zip"): (
        "7aaae2788ad9740e5c8dbc2c3dddeee2b5a86d047a5bac899d19344531abd5a9",
        {"denari", "bastoni"},
    ),
    Path("tools/cards/CARTE-usura-parte-2.zip"): (
        "4ff0be37fba6948ccd73c14758bfa135df59ed90e5b7c509d3eedd378b59cd12",
        {"coppe", "spade"},
    ),
}
SUITS = {"denari", "bastoni", "coppe", "spade"}
PREFIX = "app/src/main/assets/cards/"
EXPECTED = {f"{PREFIX}{suit}_{i:02d}.jpg" for suit in SUITS for i in range(1, 11)}


def check_image(name: str, data: bytes) -> None:
    if name not in EXPECTED or len(data) < 10_000:
        raise ValueError(f"Unexpected card: {name}")
    if not data.startswith(b"\xff\xd8\xff") or not data.endswith(b"\xff\xd9"):
        raise ValueError(f"Malformed JPEG boundaries: {name}")


def load_zip(path: Path, wanted: set[str], expected_digest: str | None = None) -> dict[str, bytes]:
    raw = path.read_bytes()
    if expected_digest and sha256(raw).hexdigest() != expected_digest:
        raise ValueError(f"Approved-card archive checksum mismatch: {path}")
    with ZipFile(path) as archive:
        infos = [m for m in archive.infolist() if not m.is_dir()]
        actual = [m.filename for m in infos]
        if len(set(actual)) != len(actual) or set(actual) != wanted:
            raise ValueError(f"Card archive content differs: {path}")
        cards = {name: archive.read(name) for name in actual}
    for name, data in cards.items():
        check_image(name, data)
    return cards


def install(cards: dict[str, bytes]) -> None:
    if set(cards) != EXPECTED:
        raise ValueError("Card set must contain exactly forty cards")
    CARDS.mkdir(parents=True, exist_ok=True)
    # No stale cards from a prior implementation or unused alternative deck.
    for file in CARDS.glob("*.jpg"):
        file.unlink()
    for name, data in cards.items():
        (CARDS / name[len(PREFIX):]).write_bytes(data)
    print(f"CARTE DECK: installed {len(cards)} full-resolution JPEGs")


def main() -> None:
    present = {p for p in PARTS if p.is_file()}
    if present:
        if present != set(PARTS):
            raise ValueError("Both worn-card archives must be present; refusing half a deck")
        contents: dict[str, bytes] = {}
        for path, (digest, suits) in PARTS.items():
            names = {f"{PREFIX}{suit}_{i:02d}.jpg" for suit in suits for i in range(1, 11)}
            contents.update(load_zip(path, names, digest))
        install(contents)
        print("CARTE DECK: approved intensely worn art, original 1920 x 3173 pixels, lossless ZIP split")
    elif OLD.is_file():
        install(load_zip(OLD, EXPECTED))
        print("CARTE DECK: legacy supplied HD archive")
    else:
        subprocess.run(["python3", "tools/fetch_neapolitan_cards.py"], check=True)
        expected_names = {name[len(PREFIX):] for name in EXPECTED}
        found = {p.name for p in CARDS.glob("*.jpg")}
        if found != expected_names:
            raise ValueError("Default card downloader did not provide exactly forty cards")
        print("CARTE DECK: original fetched card art (wear archive not uploaded)")


if __name__ == "__main__":
    main()

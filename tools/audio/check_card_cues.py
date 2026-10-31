#!/usr/bin/env python3
"""CI quality gate for nine real card takes, never three reused sounds."""
from array import array
from hashlib import sha256
from pathlib import Path
import math
import subprocess
import sys

RAW = Path("app/src/main/res/raw")
DURATIONS = {"deal": .32, "play": .27, "collect": .40}


def pcm_samples(file: Path) -> array:
    pcm = subprocess.check_output([
        "ffmpeg", "-v", "error", "-nostdin", "-i", str(file),
        "-ac", "1", "-ar", "8000", "-f", "s16le", "pipe:1"
    ], timeout=20)
    if not pcm or len(pcm) % 2:
        raise ValueError(f"Invalid PCM sound: {file}")
    samples = array("h")
    samples.frombytes(pcm)
    if sys.byteorder != "little":
        samples.byteswap()
    return samples


def check() -> None:
    hashes = set()
    for kind, expected_seconds in DURATIONS.items():
        for index in range(1, 4):
            resource = f"salon_card_{kind}_{index:02d}"
            file = RAW / f"{resource}.ogg"
            if not file.is_file():
                raise ValueError(f"Missing real card handling cue: {file}")
            data = file.read_bytes()
            if data[:4] != b"OggS":
                raise ValueError(f"Invalid OGG file: {file}")
            digest = sha256(data).hexdigest()
            if digest in hashes:
                raise ValueError(f"Duplicated acoustic take: {resource}")
            hashes.add(digest)
            samples = pcm_samples(file)
            seconds = len(samples) / 8000
            if abs(seconds - expected_seconds) > .055:
                raise ValueError(f"{resource} duration {seconds:.3f}s, wanted {expected_seconds}s")
            hop = 160  # 20ms
            envelope = [
                math.sqrt(sum(sample * sample for sample in samples[i:i + hop]) /
                          len(samples[i:i + hop]))
                for i in range(0, len(samples), hop)
            ]
            strongest = max(envelope)
            start = max(envelope[:7])
            if strongest < 190 or start < strongest * .16:
                raise ValueError(f"Late or silent card sound {resource}: "
                                 f"early={start:.0f}, peak={strongest:.0f}")
            print(f"RECORDED CARD CUE OK {resource}: {seconds:.3f}s; "
                  f"attack in first 140ms: {start / strongest:.0%}")
    for old in ("salon_card_deal.ogg", "salon_card_play.ogg", "salon_card_collect.ogg"):
        if (RAW / old).exists():
            raise ValueError(f"Obsolete repetitive cue still bundled: {old}")


if __name__ == "__main__":
    check()

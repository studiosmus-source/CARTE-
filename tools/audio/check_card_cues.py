#!/usr/bin/env python3
"""CI: reject late, prolonged or silent card handling sounds.

Sound durations are checked after all audio imports, including any supplied
archive. Only three card cues are constrained; jazz and salon ambience stay
untouched. The early onset test detects the previous PLAY file, whose loudest
impact arrived ~1.1 seconds after the animation began.
"""
from array import array
from pathlib import Path
import math
import subprocess
import sys

RAW = Path("app/src/main/res/raw")
LIMITS = {
    "salon_card_deal": (0.48, 0.55),
    "salon_card_play": (0.28, 0.35),
    "salon_card_collect": (0.39, 0.46),
}


def pcm_samples(file: Path) -> array:
    data = subprocess.check_output([
        "ffmpeg", "-v", "error", "-nostdin",
        "-i", str(file), "-ac", "1", "-ar", "8000",
        "-f", "s16le", "pipe:1",
    ], timeout=20)
    if not data or len(data) % 2:
        raise ValueError(f"Invalid PCM audio for {file}")
    samples = array("h")
    samples.frombytes(data)
    if sys.byteorder != "little":
        samples.byteswap()
    return samples


def check() -> None:
    for resource, (expected, maximum) in LIMITS.items():
        file = RAW / f"{resource}.ogg"
        if not file.exists():
            raise ValueError(f"Missing card cue: {file}")
        samples = pcm_samples(file)
        seconds = len(samples) / 8000
        if not expected - 0.09 <= seconds <= maximum:
            raise ValueError(f"{resource} lasts {seconds:.3f}s; expected about {expected}s")

        # 20 ms RMS windows; look for a nontrivial transient near animation
        # onset instead of merely requiring the Ogg to be nonempty.
        frames = 160
        windows = [
            math.sqrt(sum(value * value for value in samples[i:i + frames]) /
                      len(samples[i:i + frames]))
            for i in range(0, len(samples), frames)
        ]
        loudest = max(windows)
        early = max(windows[:9])   # first 180 ms
        if loudest < 180 or early < loudest * 0.20:
            raise ValueError(
                f"{resource}: late/inaudible onset (early={early:.0f}, peak={loudest:.0f})"
            )
        print(f"CARD CUE OK: {resource}, {seconds:.3f}s, early impact={early / loudest:.0%}")


if __name__ == "__main__":
    check()

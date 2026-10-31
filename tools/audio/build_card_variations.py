#!/usr/bin/env python3
"""Build nine distinct, dry card Foley cues from verified CC0 real handling.

Source: MellowAudio / Playing Cards.wav / Freesound 591486, CC0.
The original recording contains a series of distinct card-handling takes. We
select different transient-rich moments in three time regions, preserve their
natural recorded pitch, add a tiny boundary fade, and reject silent output.

Deterministic selection and pinned-source SHA make CI results reproducible.
Only the nine final short Ogg cues are bundled; the 20-second source is not.
"""
from array import array
from pathlib import Path
from subprocess import check_output, run
import hashlib
import math
import sys
import tempfile

from import_approved_recordings import SOURCES, fetch_source

OUT = Path("app/src/main/res/raw")
RATE = 8000
HOP = 80  # 10 ms
# The existing approved recording was auditioned in three regions for the
# three classes of motion. Pick distinct takes within the same region.
REGIONS = {
    "deal": (0.05, 5.6, 0.32),
    "play": (5.6, 10.8, 0.27),
    "collect": (10.8, 19.65, 0.40),
}


def decode_mono(source: Path) -> array:
    pcm = check_output([
        "ffmpeg", "-v", "error", "-nostdin", "-i", str(source),
        "-ar", str(RATE), "-ac", "1", "-f", "s16le", "pipe:1"
    ], timeout=35)
    if not pcm or len(pcm) % 2:
        raise RuntimeError("Real cards source could not be decoded")
    values = array("h")
    values.frombytes(pcm)
    if sys.byteorder != "little":
        values.byteswap()
    if len(values) < RATE * 19:
        raise RuntimeError("Expected the complete 20-second CC0 recording")
    return values


def choose_real_takes(pcm: array, begin: float, end: float, count: int = 3):
    """Find distinct acoustic attacks, spaced far enough to be separate takes."""
    energies = []
    for i in range(0, len(pcm) - HOP, HOP):
        block = pcm[i:i + HOP]
        energies.append(math.sqrt(sum(v * v for v in block) / len(block)) / 32768)
    i0 = max(6, int(begin * RATE / HOP))
    i1 = min(len(energies) - 15, int(end * RATE / HOP))
    region_peak = max(energies[i0:i1])
    threshold = max(.003, region_peak * .11)
    candidates = []
    for i in range(i0, i1):
        e = energies[i]
        if e < threshold:
            continue
        local = energies[max(i0, i-3):min(i1, i+4)]
        if e < max(local):
            continue
        previous = sum(energies[max(0, i-8):i]) / 8
        novelty = max(0.0, e - previous)
        score = e * .65 + novelty * 1.45
        candidates.append((score, i, e))
    selected = []
    for score, i, energy in sorted(candidates, reverse=True):
        onset = i * HOP / RATE
        if any(abs(onset - chosen[0]) < .52 for chosen in selected):
            continue
        selected.append((onset, energy))
        if len(selected) == count:
            break
    if len(selected) != count:
        raise RuntimeError(f"Not enough distinct recorded card attacks in {begin}-{end}s")
    return sorted(selected)


def render_variation(source: Path, dest: Path, start: float, duration: float,
                     peak: float):
    # Peak matching only, NOT compressor/loudnorm: preserve the natural tap,
    # paper friction and attack-release pattern from the recorded take.
    gain = max(.76, min(1.30, .48 / max(peak, .15)))
    fade_start = duration - .06
    filters = (
        f"highpass=f=110,lowpass=f=7500,volume={gain:.4f},"
        f"afade=t=in:st=0:d=0.008,"
        f"afade=t=out:st={fade_start:.3f}:d=0.06"
    )
    run(["ffmpeg", "-v", "error", "-nostdin", "-y",
         "-ss", f"{start:.3f}", "-t", f"{duration:.3f}",
         "-i", str(source), "-filter:a", filters,
         "-ac", "1", "-ar", "24000", "-c:a", "libvorbis",
         "-q:a", "5", str(dest)],
        check=True, timeout=35)
    data = dest.read_bytes()
    if len(data) < 1800 or data[:4] != b"OggS":
        raise RuntimeError(f"Missing/empty recorded cue: {dest}")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="carte-cards-") as directory:
        source = Path(directory) / "cards.mp3"
        author, sound_id = SOURCES["cards"]
        # fetch_source verifies CC0 on the source page and the pinned SHA-256.
        fetch_source("cards", author, sound_id, source)
        pcm = decode_mono(source)
        digests = set()
        for kind, (begin, end, seconds) in REGIONS.items():
            attacks = choose_real_takes(pcm, begin, end)
            for index, (onset, peak) in enumerate(attacks, start=1):
                # 45 ms lead-in captures actual handling onset, not silence.
                start = max(0.0, onset - .045)
                name = f"salon_card_{kind}_{index:02d}"
                dest = OUT / f"{name}.ogg"
                render_variation(source, dest, start, seconds, peak)
                digest = hashlib.sha256(dest.read_bytes()).hexdigest()
                if digest in digests:
                    raise RuntimeError("Duplicate recorded card sample: " + name)
                digests.add(digest)
                print(f"REAL CARD CUE {name}: source {start:.2f}s, "
                      f"{seconds:.2f}s, peak={peak:.3f}, SHA256={digest}")
    # Only the short, specific variants belong in the installed APK.
    for legacy in ("salon_card_deal", "salon_card_play", "salon_card_collect"):
        (OUT / (legacy + ".ogg")).unlink(missing_ok=True)
    print("Nine genuinely different card-handling takes are ready.")


if __name__ == "__main__":
    main()

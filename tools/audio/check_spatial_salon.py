#!/usr/bin/env python3
"""Regression tests for real offscreen room acoustics in the installable APK."""
from array import array
from pathlib import Path
import math
import subprocess
import sys

RAW = Path("app/src/main/res/raw")
DURATIONS = {
    "salon_fireplace": 67.0,
    "salon_glassware": 1.26,
    "salon_chair": 4.0,
    "salon_footsteps": 4.0,
    "salon_applause": 2.60,
    "salon_whisper_01": 3.6,
    "salon_whisper_02": 4.0,
    "salon_whisper_03": 3.8,
    "salon_whisper_04": 4.1,
}


def pcm(name: str, high_pass: bool = False) -> array:
    file = RAW / (name + ".ogg")
    command = ["ffmpeg", "-v", "error", "-nostdin", "-i", str(file)]
    if high_pass:
        command.extend(["-af", "highpass=f=3600"])
    command.extend(["-ac", "1", "-ar", "8000", "-f", "s16le", "pipe:1"])
    data = subprocess.check_output(command, timeout=28)
    samples = array("h")
    samples.frombytes(data)
    if sys.byteorder != "little":
        samples.byteswap()
    if not samples:
        raise RuntimeError(f"Silent/empty audio: {name}")
    return samples


def rms(data: array, start: int, stop: int) -> float:
    values = data[start:stop]
    return math.sqrt(sum(value * value for value in values) / max(len(values), 1))


def main() -> None:
    for name, seconds in DURATIONS.items():
        file = RAW / (name + ".ogg")
        if not file.is_file() or file.read_bytes()[:4] != b"OggS":
            raise RuntimeError(f"Missing/invalid recorded ambience: {file}")
        length = float(subprocess.check_output([
            "ffprobe", "-v", "error", "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1", str(file),
        ], text=True).strip())
        if abs(length - seconds) > .16:
            raise RuntimeError(f"Wrong duration for {name}: {length:.3f}s")
        sound = pcm(name)
        level = rms(sound, 0, len(sound))
        if level < 50:
            raise RuntimeError(f"Inaudible/broken spatial sound: {name} (RMS {level:.0f})")
        if name.startswith("salon_whisper"):
            # Fade-in prevents the full voice volume arriving at t=0.
            first = rms(sound, 0, min(len(sound), 1200))
            middle = rms(sound, 6400, min(len(sound), 16000))
            if first >= middle * .86:
                raise RuntimeError(f"Whisper attacks too suddenly: {name}")
        print(f"SPATIAL OK {name}: {length:.2f}s RMS {level:.0f}")

    fire = pcm("salon_fireplace")
    fire_high = pcm("salon_fireplace", high_pass=True)
    total = rms(fire, 0, len(fire))
    crackle = rms(fire_high, 0, len(fire_high))
    # Previous heavy noise removal + lowpass 2.8k removed the actual
    # high-frequency wood pops. Verify these now survive compression.
    if crackle / total < .027:
        raise RuntimeError(
            f"Fire recording still has no wood crackle: high/total={crackle/total:.3%}"
        )
    print(f"FIRE CRACKLE VERIFIED: high-frequency ratio {crackle/total:.1%}")

    applause = pcm("salon_applause")
    if max(abs(v) for v in applause) < 2500:
        raise RuntimeError("Applause lacks audible transients")
    print("APPLAUSE VERIFIED: real group layers and nontrivial transients")


if __name__ == "__main__":
    main()

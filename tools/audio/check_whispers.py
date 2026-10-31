#!/usr/bin/env python3
"""Verify four different short recorded conversation excerpts in res/raw.

Keep this check after the verified CC0 build importer, before Android compile.
It rejects accidental fallback to one long 19 second ambience recording.
"""
from pathlib import Path
import subprocess

RAW = Path("app/src/main/res/raw")
DURATIONS = [3.6, 4.0, 3.8, 4.1]


def main():
    payloads = []
    for index, seconds in enumerate(DURATIONS, 1):
        file = RAW / f"salon_whisper_{index:02}.ogg"
        assert file.is_file(), f"Missing voice excerpt: {file}"
        data = file.read_bytes()
        assert data.startswith(b"OggS") and len(data) > 10_000, f"Invalid voice file: {file}"
        duration = float(subprocess.check_output([
            "ffprobe", "-v", "error", "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1", str(file)
        ], text=True).strip())
        assert abs(duration - seconds) < .15, f"Wrong duration: {file} = {duration}"
        payloads.append(data)
        print(f"Voice fragment {index}: {duration:.2f}s, {len(data)} bytes")
    assert len(set(payloads)) == 4, "Conversation fragments must not be identical"


if __name__ == "__main__":
    main()

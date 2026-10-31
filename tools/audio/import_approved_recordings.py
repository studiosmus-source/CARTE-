#!/usr/bin/env python3
"""Import verified CC0 Freesound recordings to CARTE (build-time only).

A user-provided Ogg in res/raw always wins. All other recordings are sourced
from the approved original Freesound pages, using their official public HQ
preview. The Android app itself remains offline. A missing source is a HARD
build error; no hidden synthetic or placeholder fallback is allowed.
"""
from pathlib import Path
from urllib.request import Request, urlopen
import hashlib
import html
import re
import subprocess
import tempfile
import sys

OUT = Path("app/src/main/res/raw")
SOURCES = {
    "cards": ("MellowAudio", 591486),
    "fire": ("Davor", 382616),
    "voices": ("SpliceSound", 260124),
    "glass": ("AardsReal", 842180),
    "chair": ("SpliceSound", 218316),
    "steps": ("allrealsound", 161756),
    "claps": ("Jofae", 362954),
}
# Pin the verified HQ preview bytes from the successful 31 Oct 2026 CI run.
# A changed upstream source must be reviewed, not silently bundled.
SHA256 = {
    "cards": "e68fd839e3d19ebe078735be21f3306b545f5f8acf26aa5747b9cdf87c87fecc",
    "chair": "7ee3bd4021e3e1458ac9d8c736730de1a068d24587383ab7ff8114b965b7c4a9",
    "claps": "d87c42b8252448692a07bd54e33a83c8d3cce07b5a2156baccdeaa7df1075cde",
    "fire": "1e470bafa4a1a44d143beb0ae65664e574fe01243d82901a8428b4e41251320e",
    "glass": "c9151cdc996b764b23ae0b0fc0ea527c5b4315af4d74f89052c1ecfc38fc0a24",
    "steps": "6c36c7aa608e66065d37e73c5863118fad0f99d66f28da14307e23e9ffdb3588",
    "voices": "0e45102d17749ee2af11f02b4d7878ceb3326ef799984d88e3010e5c64d86011",
}
# Source start, playback length, output rate, processing chain.
# The original 3.0 / 1.2 / 2.0 second cuts were too long. In particular,
# the PLAY impact occurred ~1.1 seconds after playback began. These verified
# offsets place the first audible transient around 70-100 ms after the card
# moves, and end within a single visual transition. All three are genuine
# segments of the SAME CC0 recording, never synthetic foley.
CLIPS = {
    "salon_card_deal": ("cards", 0.06, 0.48, "highpass=f=140,lowpass=f=10500,loudnorm=I=-18:TP=-3:LRA=9,afade=t=out:st=0.40:d=0.08"),
    "salon_card_play": ("cards", 6.22, 0.28, "highpass=f=140,lowpass=f=10500,loudnorm=I=-18:TP=-3:LRA=9,afade=t=out:st=0.22:d=0.06"),
    "salon_card_collect": ("cards", 12.06, 0.39, "highpass=f=140,lowpass=f=10500,loudnorm=I=-18:TP=-3:LRA=9,afade=t=out:st=0.31:d=0.08"),
    "salon_fireplace": ("fire", 0.0, 70.0, "highpass=f=90,lowpass=f=2800,afftdn=nr=12:nf=-40,loudnorm=I=-24:TP=-5:LRA=15"),
    "salon_conversation": ("voices", 0.0, 19.0, "highpass=f=150,lowpass=f=4000,loudnorm=I=-22:TP=-4:LRA=12"),
    # Four genuine, non-identical sections of the verified conversation.
    # Fade both ends to prevent cuts; occasional playback overlap happens in
    # SalonAudio.kt, not by mixing permanently into a noisy ambient loop.
    "salon_whisper_01": ("voices", 0.8, 3.6, "highpass=f=180,lowpass=f=3800,loudnorm=I=-23:TP=-5:LRA=10,afade=t=in:st=0:d=0.20,afade=t=out:st=3.15:d=0.45"),
    "salon_whisper_02": ("voices", 4.9, 4.0, "highpass=f=180,lowpass=f=3800,loudnorm=I=-23:TP=-5:LRA=10,afade=t=in:st=0:d=0.22,afade=t=out:st=3.5:d=0.5"),
    "salon_whisper_03": ("voices", 9.5, 3.8, "highpass=f=180,lowpass=f=3800,loudnorm=I=-23:TP=-5:LRA=10,afade=t=in:st=0:d=0.20,afade=t=out:st=3.3:d=0.5"),
    "salon_whisper_04": ("voices", 14.0, 4.1, "highpass=f=180,lowpass=f=3800,loudnorm=I=-23:TP=-5:LRA=10,afade=t=in:st=0:d=0.22,afade=t=out:st=3.55:d=0.55"),
    "salon_glassware": ("glass", 0.0, 1.26, "highpass=f=130,loudnorm=I=-19:TP=-4:LRA=10"),
    "salon_chair": ("chair", 0.0, 4.0, "highpass=f=100,lowpass=f=5500,loudnorm=I=-20:TP=-4:LRA=10"),
    "salon_footsteps": ("steps", 2.0, 4.0, "highpass=f=95,lowpass=f=5100,loudnorm=I=-19:TP=-4:LRA=10"),
    "salon_applause": ("claps", 0.0, 2.25, "highpass=f=130,lowpass=f=4600,loudnorm=I=-21:TP=-4:LRA=10"),
}
MAX_DOWNLOAD = 12_000_000
USER_AGENT = "CARTE-offline-audio-import/1.0 (https://github.com/studiosmus-source/CARTE-)"


def get(url, limit):
    request = Request(url, headers={"User-Agent": USER_AGENT})
    with urlopen(request, timeout=40) as response:
        declared = response.headers.get("Content-Length")
        if declared is not None and int(declared) > limit:
            raise RuntimeError(f"Source too large: {url}")
        data = response.read(limit + 1)
    if len(data) > limit or len(data) < 500:
        raise RuntimeError(f"Invalid source size: {url}: {len(data)}")
    return data


def fetch_source(name, author, sound_id, dest):
    page_url = f"https://freesound.org/people/{author}/sounds/{sound_id}/"
    page = get(page_url, 1_000_000).decode("utf-8", "replace")
    if "Creative Commons 0" not in html.unescape(page):
        raise RuntimeError(f"Source lacks explicit CC0 marker: {page_url}")
    match = re.search(
        r'data-static-file-url=["\'](https://cdn\.freesound\.org/previews/[^"\']+-hq\.mp3)["\']',
        page,
    )
    if not match:
        raise RuntimeError(f"High quality Freesound preview not found: {page_url}")
    audio_url = html.unescape(match.group(1))
    audio = get(audio_url, MAX_DOWNLOAD)
    if audio[:3] != b"ID3" and not (audio[0] == 0xff and (audio[1] & 0xe0) == 0xe0):
        raise RuntimeError(f"Not an MP3 recording: {page_url}")
    digest = hashlib.sha256(audio).hexdigest()
    if digest != SHA256[name]:
        raise RuntimeError(f"CC0 recording checksum changed: {name}, {digest}")
    dest.write_bytes(audio)
    print(
        f"CC0 RECORDING: {name}, Freesound {sound_id}, {len(audio)} bytes, "
        f"SHA256={hashlib.sha256(audio).hexdigest()}"
    )


def render(source, target, offset, duration, filters):
    # Stereo 48 kHz, constrained bitrate for an installable offline package.
    command = [
        "ffmpeg", "-v", "error", "-nostdin", "-y",
        "-ss", str(offset), "-t", str(duration), "-i", str(source),
        "-filter:a", filters,
        "-ar", "48000", "-ac", "2", "-c:a", "libvorbis", "-q:a", "4",
        str(target),
    ]
    subprocess.run(command, check=True, timeout=75)
    if target.stat().st_size < 2000 or target.read_bytes()[:4] != b"OggS":
        raise RuntimeError(f"Invalid processed recording: {target}")


def main():
    needed = {out: spec for out, spec in CLIPS.items() if not (OUT / (out + ".ogg")).exists()}
    if not needed:
        print("CC0 RECORDINGS: all 9 prepackaged by user")
        return
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="carte-freesound-") as working:
        directory = Path(working)
        recordings = {}
        for source in sorted({spec[0] for spec in needed.values()}):
            author, sound_id = SOURCES[source]
            filename = directory / f"{source}.mp3"
            fetch_source(source, author, sound_id, filename)
            recordings[source] = filename
        for target, (source, start, seconds, filters) in needed.items():
            output = OUT / (target + ".ogg")
            try:
                render(recordings[source], output, start, seconds, filters)
            except Exception:
                output.unlink(missing_ok=True)
                raise
            print(f"CC0 INCLUDED: {target}.ogg, {output.stat().st_size} bytes")
    print("CC0 RECORDINGS: all approved external samples have been imported.")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print("CC0 RECORDINGS ERROR:", str(exc), file=sys.stderr)
        raise

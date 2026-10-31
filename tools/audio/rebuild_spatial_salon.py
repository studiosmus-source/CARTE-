#!/usr/bin/env python3
"""Rebuild spatial salon ambience from the pinned, license-verified real sources.

Do not use old OGGs unpacked from prior APKs: that would retain the
over-denoised fireplace, close-miked voices and weak three-clap applause.
Outputs are always replaced only after their genuine CC0 origins have passed
the importer SHA-256 check. No app network dependency.
"""
from pathlib import Path
from subprocess import run
import tempfile

from import_approved_recordings import OUT, SOURCES, fetch_source, render

# Start, seconds and physically motivated filtering; original pitch retained.
# Voice and room cues are gently band-limited, softened and given restrained
# early reflections. Dry, local playing-card sounds are handled separately.
SPATIAL = {
    "salon_fireplace": ("fire", 3.0, 67.0,
        "highpass=f=70,lowpass=f=9200,"
        "acompressor=threshold=-26dB:ratio=1.18:attack=12:release=220,"
        "volume=1.18,afade=t=in:st=0:d=0.7,afade=t=out:st=65.8:d=1.2"),
    "salon_whisper_01": ("voices", 0.8, 3.6,
        "highpass=f=185,lowpass=f=2400,"
        "acompressor=threshold=-25dB:ratio=1.7:attack=30:release=350,"
        "aecho=0.78:0.42:62|113:0.16|0.09,"
        "afade=t=in:st=0:d=0.65,afade=t=out:st=2.95:d=0.65"),
    "salon_whisper_02": ("voices", 4.9, 4.0,
        "highpass=f=185,lowpass=f=2350,"
        "acompressor=threshold=-25dB:ratio=1.7:attack=30:release=350,"
        "aecho=0.78:0.42:68|127:0.16|0.09,"
        "afade=t=in:st=0:d=0.72,afade=t=out:st=3.30:d=0.70"),
    "salon_whisper_03": ("voices", 9.5, 3.8,
        "highpass=f=185,lowpass=f=2500,"
        "acompressor=threshold=-25dB:ratio=1.7:attack=30:release=350,"
        "aecho=0.78:0.42:76|137:0.15|0.08,"
        "afade=t=in:st=0:d=0.70,afade=t=out:st=3.10:d=0.70"),
    "salon_whisper_04": ("voices", 14.0, 4.1,
        "highpass=f=185,lowpass=f=2250,"
        "acompressor=threshold=-25dB:ratio=1.7:attack=30:release=350,"
        "aecho=0.78:0.42:85|142:0.16|0.09,"
        "afade=t=in:st=0:d=0.72,afade=t=out:st=3.38:d=0.72"),
    "salon_glassware": ("glass", 0.0, 1.26,
        "highpass=f=180,lowpass=f=4400,"
        "aecho=0.87:0.38:47|109:0.12|0.07,"
        "afade=t=in:st=0:d=0.10,afade=t=out:st=1.02:d=0.24"),
    "salon_chair": ("chair", 0.0, 4.0,
        "highpass=f=95,lowpass=f=3200,"
        "aecho=0.86:0.35:53|117:0.13|0.07,"
        "afade=t=in:st=0:d=0.16,afade=t=out:st=3.55:d=0.45"),
    "salon_footsteps": ("steps", 2.0, 4.0,
        "highpass=f=100,lowpass=f=3450,"
        "aecho=0.87:0.30:49|119:0.12|0.07,"
        "afade=t=in:st=0:d=0.16,afade=t=out:st=3.57:d=0.43"),
}


def render_applause(source: Path, dest: Path) -> None:
    # Three original genuine handclaps, offset in three layers to sound like a
    # SMALL salon group. A controlled limiter keeps the approval restrained.
    filters = (
        "[0:a]highpass=f=160,lowpass=f=6300,"
        "asplit=3[front][left][right];"
        "[front]volume=1.25[f];"
        "[left]volume=0.43,adelay=92|92[l];"
        "[right]volume=0.36,adelay=216|216[r];"
        "[f][l][r]amix=inputs=3:duration=longest:normalize=0,"
        "alimiter=limit=0.91,"
        "aecho=0.91:0.24:61|116:0.09|0.055,"
        "afade=t=in:st=0:d=0.06,"
        "afade=t=out:st=2.29:d=0.31,"
        "apad=whole_dur=2.60,atrim=duration=2.60[out]"
    )
    run(["ffmpeg", "-v", "error", "-nostdin", "-y",
         "-ss", "0", "-t", "2.6", "-i", str(source),
         "-filter_complex", filters, "-map", "[out]",
         "-ar", "48000", "-ac", "2", "-c:a", "libvorbis", "-q:a", "5",
         str(dest)], check=True, timeout=75)
    if dest.stat().st_size < 2000 or dest.read_bytes()[:4] != b"OggS":
        raise RuntimeError("Processed applause is empty")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="carte-salon-distance-") as temp:
        work = Path(temp)
        input_paths = {}
        source_names = {spec[0] for spec in SPATIAL.values()} | {"claps"}
        for source in sorted(source_names):
            author, sound_id = SOURCES[source]
            path = work / f"{source}.mp3"
            fetch_source(source, author, sound_id, path)
            input_paths[source] = path

        # Atomic replacement: old sound stays until the complete new clip
        # is successfully rendered and checked.
        for target, (source, start, seconds, filters) in SPATIAL.items():
            temporary = OUT / f"{target}.new.ogg"
            try:
                # Restrict echo tails to the defined clip duration, so
                # scheduling and loop positions remain deterministic.
                render(input_paths[source], temporary, start, seconds,
                       filters + f",atrim=duration={seconds:.3f}")
                temporary.replace(OUT / f"{target}.ogg")
            finally:
                temporary.unlink(missing_ok=True)
            print(f"SPATIAL SOUND READY: {target} ({seconds}s)")

        temporary = OUT / "salon_applause.new.ogg"
        try:
            render_applause(input_paths["claps"], temporary)
            temporary.replace(OUT / "salon_applause.ogg")
        finally:
            temporary.unlink(missing_ok=True)
        print("APPLAUSE READY: genuine small-group claps with restrained early reflections")


if __name__ == "__main__":
    main()

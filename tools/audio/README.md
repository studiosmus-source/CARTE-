# CARTE — offline salon audio

Audio decisions approved 31 October 2026. This folder contains an **optional local bundle importer**, not generated or fake sound effects.

## Real card foley fallback

If the user-provided ZIP does not contain three card event recordings, the
Android build downloads the **Kenney Casino Audio** CC0 1.0 archive from
[OpenGameArt](https://opengameart.org/content/54-casino-sound-effects-cards-dice-chips),
originally published at [Kenney.nl](https://kenney.nl/assets/casino-audio).
The build imports real recorded sounds (card-shuffle.ogg, card-place-2.ogg and
card-shove-4.ogg) into salon_card_deal, salon_card_play, salon_card_collect
respectively. These are **candidates for Android listening tests**, not the
original Freesound recordings selected by the user. They are never downloaded
by the phone, and a user-supplied approved sample takes priority.

The previous synthesized fireplace and card hiss was removed entirely. An
unavailable recording stays silent rather than falling back to white noise.

## Build input

Put one file at `tools/audio/CARTE-Salon-Audio.zip` on the branch to be built. At the ZIP root, use the exact resource filenames below (Ogg Vorbis, nonempty). `python3 tools/audio/import_salon_audio.py` validates the entries and puts them into Android `res/raw` before Gradle compilation. The ZIP is not shipped separately in the APK.

| Resource | Source / purpose | Status |
| --- | --- | --- |
| `salon_jazz.ogg` | CARTE approved 3:12 mix of *Echoes on Bourbon Street* by MarkMusicStudio, remixed by the app's creator; original from Pixabay ID 572116 | Approved, processed; binary bundle supplied separately |
| `salon_fireplace.ogg` | Davor, *Fire crackling in fireplace*, Freesound 382616, CC0 | Approved candidate; actual file not yet obtained |
| `salon_card_deal.ogg` | MellowAudio, *Playing Cards.wav*, Freesound 591486, CC0 | Approved candidate; needs edited section |
| `salon_card_play.ogg` | MellowAudio, Freesound 591486, CC0 | Needs selected section |
| `salon_card_collect.ogg` | MellowAudio, Freesound 591486, CC0 | Needs selected section |
| `salon_conversation.ogg` | SpliceSound, *Indoor adult murmur*, Freesound 260124 and 260121, CC0 | Approved candidate; needs sample |
| `salon_glassware.ogg` | Glass / wine pours, CC0 Freesound candidates 842180 and 382028 | Approved candidate; needs sample |
| `salon_chair.ogg` | Chair drag, SpliceSound, Freesound 218316, verify exact licence | Approved candidate; needs sample |
| `salon_footsteps.ogg` | Dress shoes on wood floor, allrealsound, Freesound 161756, verify exact licence | Approved candidate; needs sample |
| `salon_applause.ogg` | Three real hand claps, Jofae, Freesound 362954, CC0 | Approved candidate; needs sample |
| `salon_door.ogg` | Optional rare door sound | Not yet selected |

The hand claps play only after a **winning match**, not after each Scopa hand. Defeats and draws deliberately have no compulsory applause.

## Licence and provenance

The user-provided music comes from Pixabay and must be **embedded in the game**, not distributed as an independent track or standalone audio collection. Check the [Pixabay Content License](https://pixabay.com/service/license-summary/) before distributing the game. Freesound samples need their exact sound page and licence checked before being incorporated. Keep the author and URL information in project credits even if CC0 makes credit optional.

## Quality / acceptance

Do not call the audio integration complete until the ZIP has been committed on this branch, the CI logs explicitly report the imported file count, the final APK contains the compiled resources, and a real Android-device test confirms: home jazz clean, black fade, empty table fade in, AVVIA deal, continuous music without restart, softly distant room EQ, restrained reactions, fade out.

When no ZIP is present, the build remains available for non-audio testing but prints a warning and contains **no approved sampled jazz**. The old procedural effects have been removed; only recorded files are played.

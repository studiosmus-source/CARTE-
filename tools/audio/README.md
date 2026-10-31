# CARTE — salotto sonoro, tracce reali (31 ottobre 2026)

## Un solo percorso di importazione

Il motore `Soundscape.kt` che generava fruscii sintetici è stato eliminato.
Le sessioni di Scopa e Briscola segnalano eventi di carte a
`SalonAudio.kt`, che riproduce soltanto registrazioni OGG locali. I nove rumori delle carte (tre registrazioni distinte per azione) sono caricati in anticipo tramite `SoundPool`, senza creare
un `MediaPlayer` a ogni giocata; un evento nuovo interrompe quello precedente.

Ordine della build:
1. `tools/audio/import_salon_audio.py` estrae in `app/src/main/res/raw/`
   gli OGG approvati dall'archivio `tools/audio/CARTE-Salon-Audio.zip`
   caricato dall'utente (contenente il jazz finale).
2. `tools/audio/import_approved_recordings.py` importa gli ambienti CC0 mancanti.
   `tools/audio/build_card_variations.py` scarica e verifica la registrazione
   originale CC0 delle carte (SHA-256 bloccato), seleziona nove prese acustiche
   differenti e crea nove brevi OGG senza compressione dinamica né pitch-shifting.
3. Gradle include nell'APK le registrazioni; CI verifica 40 carte HD e tutti
   e 10 i file audio richiesti. Non esiste streaming in gioco.

L'archivio dell'utente prevale sempre sugli altri suoni. I vecchi importatori
Kenney e Muges e la diagnostica temporanea sono stati eliminati.

## Registrazioni e provenienza

| Risorsa Android | Registrazione | Licenza |
|---|---|---|
| salon_jazz.ogg | Montaggio jazz noir **approvato** 3:12 di *Echoes on Bourbon Street* di MarkMusicStudio, da file inviato dall'utente | Pixabay Content License: integrata nell'app, non distribuire traccia singolarmente |
| salon_card_deal_01–03.ogg | [MellowAudio, 591486](https://freesound.org/people/MellowAudio/sounds/591486/) 3 prese reali distinte da 0,32 s | CC0 |
| salon_card_play_01–03.ogg | MellowAudio 591486, 3 prese reali distinte da 0,27 s | CC0 |
| salon_card_collect_01–03.ogg | MellowAudio 591486, 3 prese reali distinte da 0,40 s | CC0 |
| salon_fireplace.ogg | [Davor, 382616](https://freesound.org/people/Davor/sounds/382616/) spezzone 70 s, con riduzione rumore ed EQ | CC0 |
| salon_conversation.ogg | [SpliceSound, 260124](https://freesound.org/people/SpliceSound/sounds/260124/) due adulti, 19 s | CC0 |
| salon_glassware.ogg | [AardsReal, 842180](https://freesound.org/people/AardsReal/sounds/842180/) piccoli bicchieri, 1,26 s | CC0 |
| salon_chair.ogg | [SpliceSound, 218316](https://freesound.org/people/SpliceSound/sounds/218316/) sedia su legno, 4 s | CC0 |
| salon_footsteps.ogg | [allrealsound, 161756](https://freesound.org/people/allrealsound/sounds/161756/) scarpe sul legno, 4 s | CC0 |
| salon_applause.ogg | [Jofae, 362954](https://freesound.org/people/Jofae/sounds/362954/) tre battiti in 2,25 s | CC0 |

La porta `salon_door.ogg` resta facoltativa: non è stato selezionato un
campione definitivo. Vittorie: pochi battiti, sconfitte e pareggi: silenzio
o brusio già presente, nessun effetto televisivo.

## Stato delle verifiche

Il file presente in APK non equivale a qualità d'ascolto approvata.
CI prova solo integrità, decodifica, build e test automatici. Il 31 ottobre i campioni delle carte sono stati accorciati da 3/1,2/2 secondi
a circa 0,48/0,28/0,39 secondi, con attacco acustico anticipato e dissolvenza
breve. `tools/audio/check_card_cues.py` verifica in CI durata e inizio del
transiente; non sostituisce l'ascolto su telefono. Le soglie difensive in
`SalonAudio.kt` interrompono anche eventuali campioni troppo lunghi.
I volumi precedentemente approvati non vengono modificati.

La sequenza da controllare sul telefono: jazz nitido home -> musica che
si allontana -> schermo nero -> dissolvenza sul tavolo vuoto con
ambiente -> AVVIA e distribuzione -> musica continua e distante ->
uscita in dissolvenza. Le successive mani di Scopa non tornano al nero.

Non distribuire come release definitiva finché il livello dei suoni, il
carattere del camino, il montaggio delle carte, gli eventi casuali e il
loop musicale non sono stati approvati con un test funzionale.

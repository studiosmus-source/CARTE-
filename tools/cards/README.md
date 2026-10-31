# Carte napoletane — usura intensa

L'utente ha fornito il 31 ottobre 2026 l'archivio
`CARTE-HD-usura-intensa.zip`, con **40 JPEG originali da 1920×3173 px**,
già nominati come previsto da `CardRenderer.kt`.

Perché la pagina web GitHub non permette il caricamento di questo ZIP da
49 MB in un colpo solo, le immagini sono state **divise senza rielaborazione**
in due archivi da meno di 25 MB:

- `CARTE-usura-parte-1.zip`: denari (10) + bastoni (10), 24,09 MB.
- `CARTE-usura-parte-2.zip`: coppe (10) + spade (10), 24,84 MB.

Le immagini JPEG dentro questi due archivi sono **identiche byte per byte**
a quelle del file caricato dall'utente: niente ridimensionamento, perdita
di nitidezza o nuova compressione delle immagini.

## Importazione

Caricare entrambi gli ZIP in questa cartella sul ramo
`feat/salon-continuity`. `tools/cards/import_cards.py` controlla i due
SHA-256 approvati e l'insieme esatto delle 40 carte, poi sostituisce in modo
atomico l'insieme di file in `app/src/main/assets/cards/` durante la build.
Se manca uno dei due file la build si ferma, evitando mazzi misti.
L'APK è verificato anche confrontando ogni JPEG incorporato con
l'originale nei due ZIP.

Il progetto continua a supportare il vecchio
`app/src/main/assets/CARTE-HD-assets.zip` in assenza delle due nuove parti,
e conserva come ultima opzione lo scaricamento classico.

## Visibilità del tavolo

`CardGridGeometry.kt` e `AdaptiveCardGrid.kt` garantiscono che tutte
le carte del tavolo di Scopa rimangano in vista all'interno del feltro,
distribuite su più righe se necessario. La mano del giocatore, il motore
delle regole e la logica di presa restano invariati.

## Stato

La build prima del caricamento delle due parti ha verificato **codice e
geometria**, ma usa ancora il mazzo vecchio. L'uso effettivo di questo mazzo
verrà confermato solo dopo un nuovo build sul ramo con entrambi gli ZIP e
relativa verifica nel pacchetto APK. Serve infine un collaudo visivo Android.

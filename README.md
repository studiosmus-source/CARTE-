# CARTE 🃏

App Android offline per **Briscola** e **Scopa** con carte napoletane.

## Stato
Progetto iniziale: motore di gioco e scheletro Android in Kotlin. Multiplayer futuro previsto tramite separazione tra regole, giocatori e trasporto di rete. Nessun multiplayer online attivo.

## Build
Aprire il progetto in Android Studio con JDK 17 e Android SDK 35. È necessario un Gradle compatibile (wrapper da aggiungere prima della build riproducibile).

## Principi
- Regole deterministiche, indipendenti dalla UI.
- L'IA non legge le carte nascoste dell'avversario.
- Nessuna API a pagamento.
- Asset delle carte da aggiungere solo dopo verifica della licenza.

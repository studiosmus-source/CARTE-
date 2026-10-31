package it.studiosmus.carte.audio

/**
 * Card handling cues emitted by both game sessions.
 * Only approved, recorded samples are permitted to render these events.
 * Incomplete sound packs remain silent instead of synthesizing hiss.
 */
enum class CardSoundEvent { DEAL, PLAY, COLLECT }

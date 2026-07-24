package com.memora.app.data.security

/** Structured availability of the production Memora database. */
sealed interface DatabaseOpenAvailability {
    data object Ready : DatabaseOpenAvailability
    data object WaitingForUnlock : DatabaseOpenAvailability
}

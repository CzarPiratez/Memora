package com.memora.app.data.security

/**
 * Production on-device database file names and Keystore/journal identities.
 * Instrumentation tests may reuse these for disposable conversion proofs.
 */
object ProductionDatabaseIdentity {
    const val DATABASE_NAME = "memora.db"
    const val ENCRYPTED_CANDIDATE_NAME = "memora.db.encrypted_candidate"
    const val PLAINTEXT_RETAINED_NAME = "memora.db.plaintext_retained"
    const val KEY_ALIAS = "memora.db.wrap.v1"
    const val WRAPPER_FILE = "memora_db_wrap_v1.bin"
    const val JOURNAL_FILE = "memora_db_conversion_v1.journal"
    const val EXPECTED_SCHEMA_VERSION = 4
}

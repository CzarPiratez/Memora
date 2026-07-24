package com.memora.app.data.security

import android.content.Context
import android.os.UserManager

/**
 * Gate for credential-encrypted storage availability after reboot.
 * While the user profile is locked, Memora must not open, create, or replace databases.
 */
fun interface UserCredentialUnlockGate {
    fun isUserUnlocked(context: Context): Boolean
}

object AndroidUserCredentialUnlockGate : UserCredentialUnlockGate {
    override fun isUserUnlocked(context: Context): Boolean {
        val userManager = context.applicationContext.getSystemService(UserManager::class.java)
            ?: return true
        return userManager.isUserUnlocked
    }
}

class DeviceLockedException :
    Exception("Memora database open is deferred until the user unlocks the device")

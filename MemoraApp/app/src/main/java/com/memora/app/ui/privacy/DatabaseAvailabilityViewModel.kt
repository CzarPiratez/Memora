package com.memora.app.ui.privacy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.data.security.DatabaseOpenAvailability
import com.memora.app.data.security.MemoraDatabaseHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DatabaseAvailabilityUiState(
    val phase: DatabaseAvailabilityPhase = DatabaseAvailabilityPhase.Checking,
)

sealed interface DatabaseAvailabilityPhase {
    data object Checking : DatabaseAvailabilityPhase
    data object WaitingForUnlock : DatabaseAvailabilityPhase
    data object Ready : DatabaseAvailabilityPhase
}

@HiltViewModel
class DatabaseAvailabilityViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseHandle: MemoraDatabaseHandle,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DatabaseAvailabilityUiState())
    val uiState: StateFlow<DatabaseAvailabilityUiState> = mutableUiState.asStateFlow()

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_UNLOCKED) {
                refresh()
            }
        }
    }

    init {
        registerUnlockReceiver()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableUiState.value = DatabaseAvailabilityUiState(DatabaseAvailabilityPhase.Checking)
            val availability = withContext(Dispatchers.IO) {
                databaseHandle.availability()
            }
            mutableUiState.value = DatabaseAvailabilityUiState(
                phase = when (availability) {
                    DatabaseOpenAvailability.Ready -> DatabaseAvailabilityPhase.Ready
                    DatabaseOpenAvailability.WaitingForUnlock ->
                        DatabaseAvailabilityPhase.WaitingForUnlock
                },
            )
        }
    }

    override fun onCleared() {
        runCatching { context.unregisterReceiver(unlockReceiver) }
        super.onCleared()
    }

    private fun registerUnlockReceiver() {
        val filter = IntentFilter(Intent.ACTION_USER_UNLOCKED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(unlockReceiver, filter)
        }
    }
}

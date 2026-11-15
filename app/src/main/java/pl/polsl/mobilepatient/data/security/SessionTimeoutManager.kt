package pl.polsl.mobilepatient.data.security

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionTimeoutManager {

    const val DEFAULT_TIMEOUT_MS: Long = 15 * 60 * 1000L

    @Volatile
    private var lastUserInteraction: Long = SystemClock.elapsedRealtime()

    private val _isSessionTimedOut = MutableStateFlow(false)
    val isSessionTimedOut: StateFlow<Boolean> = _isSessionTimedOut.asStateFlow()

    fun onUserInteraction() {
        lastUserInteraction = SystemClock.elapsedRealtime()
    }

    fun checkTimeout(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        val elapsed = SystemClock.elapsedRealtime() - lastUserInteraction
        if (elapsed > timeoutMs) {
            _isSessionTimedOut.value = true
            return true
        }
        return false
    }

    fun resetTimeout() {
        lastUserInteraction = SystemClock.elapsedRealtime()
        _isSessionTimedOut.value = false
    }

    fun lockSession() {
        _isSessionTimedOut.value = true
    }
}

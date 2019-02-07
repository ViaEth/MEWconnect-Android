package com.myetherwallet.mewconnect.feature.auth.utils

import android.os.Handler
import android.os.SystemClock
import com.myetherwallet.mewconnect.core.persist.prefenreces.ApplicationPreferences
import java.util.concurrent.TimeUnit

/**
 * Created by BArtWell on 07.02.2019.
 */

private val TIMER_TIMEOUT = TimeUnit.MINUTES.toMillis(5)
private val ATTEMPTS_TIMEOUT = TimeUnit.MINUTES.toMillis(2)
private const val ATTEMPTS_LIMIT = 5

class AuthAttemptsHelper(
        private val handler: Handler,
        private val preferences: ApplicationPreferences,
        private val callback: (minute: Int, second: Int) -> Unit
) {

    private var isResumed = true

    init {
        if (getUptime() < preferences.getSavedUptime()) {
            reset()
            preferences.setSavedUptime(getUptime())
        } else {
            if (checkTimer()) {
                startTimer()
            }
        }
    }

    fun check(): Boolean {
        if (preferences.getAuthFirstAttemptTime() + ATTEMPTS_TIMEOUT > getUptime()) {
            if (preferences.getAuthAttemptsCount() + 1 >= ATTEMPTS_LIMIT) {
                reset()
                preferences.setAuthTimerTime(getUptime())
                startTimer()
                return true
            }
        } else {
            preferences.setAuthFirstAttemptTime(getUptime())
        }
        preferences.incrementAuthAttemptsCount()
        return false
    }

    fun reset() {
        preferences.resetAuthAttemptsCount()
        preferences.setAuthFirstAttemptTime(0L)
    }

    fun resume() {
        isResumed = true
        startTimer()
    }

    fun pause() {
        isResumed = false
    }

    private fun startTimer() {
        if (isResumed) {
            if (checkTimer()) {
                callTimerCallback()
                handler.postDelayed({
                    startTimer()
                }, 1000L)
            } else {
                callback(0, 0)
            }
        }
    }

    private fun callTimerCallback() {
        val left = ((TIMER_TIMEOUT - (getUptime() - preferences.getAuthTimerTime())) / 1000).toInt()
        val minute = left / 60
        callback(minute, left - 60 * minute)
    }

    private fun checkTimer() = preferences.getAuthTimerTime() + TIMER_TIMEOUT > getUptime()

    private fun getUptime() = SystemClock.elapsedRealtime()
}
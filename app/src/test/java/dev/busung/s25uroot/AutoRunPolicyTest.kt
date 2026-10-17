package dev.busung.s25uroot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoRunPolicyTest {
    @Test
    fun retriesStopAtTheConfiguredBudget() {
        assertTrue(AutoRunPolicy.shouldRetry(attempts = 1, maxAttempts = 3))
        assertTrue(AutoRunPolicy.shouldRetry(attempts = 2, maxAttempts = 3))
        assertFalse(AutoRunPolicy.shouldRetry(attempts = 3, maxAttempts = 3))
        assertFalse(AutoRunPolicy.shouldRetry(attempts = 4, maxAttempts = 3))
    }

    @Test
    fun singleAttemptBudgetNeverRetries() {
        assertFalse(AutoRunPolicy.shouldRetry(attempts = 1, maxAttempts = 1))
    }

    @Test
    fun backoffDoublesPerFailure() {
        assertEquals(60, AutoRunPolicy.retryDelaySeconds(baseDelaySeconds = 60, attempts = 1))
        assertEquals(120, AutoRunPolicy.retryDelaySeconds(baseDelaySeconds = 60, attempts = 2))
        assertEquals(240, AutoRunPolicy.retryDelaySeconds(baseDelaySeconds = 60, attempts = 3))
    }

    @Test
    fun backoffIsCappedAndNeverOverflows() {
        val cap = AutoRunPolicy.MAX_RETRY_DELAY_SECONDS
        assertEquals(cap, AutoRunPolicy.retryDelaySeconds(baseDelaySeconds = 120, attempts = 6))
        // Absurd attempt counts must clamp instead of wrapping into a negative
        // wait that would schedule the payload in the past.
        assertEquals(cap, AutoRunPolicy.retryDelaySeconds(baseDelaySeconds = 120, attempts = 64))
        assertEquals(cap, AutoRunPolicy.retryDelaySeconds(baseDelaySeconds = 120, attempts = Int.MAX_VALUE))
    }

    @Test
    fun zeroBaseDelayStillProducesAPositiveWait() {
        assertEquals(1, AutoRunPolicy.retryDelaySeconds(baseDelaySeconds = 0, attempts = 1))
        assertEquals(1, AutoRunPolicy.retryDelaySeconds(baseDelaySeconds = -30, attempts = 1))
    }

    @Test
    fun attemptBudgetIsClampedToASafeRange() {
        assertEquals(AutoRunPolicy.MIN_ATTEMPTS, AutoRunPolicy.clampMaxAttempts(0))
        assertEquals(AutoRunPolicy.MIN_ATTEMPTS, AutoRunPolicy.clampMaxAttempts(-5))
        assertEquals(AutoRunPolicy.MAX_ATTEMPTS, AutoRunPolicy.clampMaxAttempts(Int.MAX_VALUE))
        assertEquals(3, AutoRunPolicy.clampMaxAttempts(3))
    }
}
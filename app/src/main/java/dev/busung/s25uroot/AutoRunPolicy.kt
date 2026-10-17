package dev.busung.s25uroot

/**
 * Retry policy for the boot auto-apply loop, kept free of Android types.
 *
 * The loop reboots into the exploit on every attempt, so an off-by-one here
 * either retries forever (bootloop) or gives up after a single try. The
 * arithmetic is therefore unit tested instead of only exercised on device.
 */
internal object AutoRunPolicy {
    const val MIN_ATTEMPTS = 1
    const val MAX_ATTEMPTS = 5
    const val DEFAULT_MAX_ATTEMPTS = 3

    /** Upper bound for a single backoff wait, so a phone is never unusable. */
    const val MAX_RETRY_DELAY_SECONDS = 1_800

    fun clampMaxAttempts(value: Int): Int = value.coerceIn(MIN_ATTEMPTS, MAX_ATTEMPTS)

    /**
     * Wait before the attempt that follows a failure. `attempts` is the number
     * of attempts already spent in this loop, so 1 means "first failure".
     * Each failure doubles the wait until [MAX_RETRY_DELAY_SECONDS].
     */
    fun retryDelaySeconds(baseDelaySeconds: Int, attempts: Int): Int {
        val base = baseDelaySeconds.coerceAtLeast(1).toLong()
        // 16 doublings already overshoot the cap for every valid base, and
        // keep base << shift far away from Long overflow.
        val scaled = base shl (attempts - 1).coerceIn(0, 16)
        return scaled.coerceAtMost(MAX_RETRY_DELAY_SECONDS.toLong()).toInt()
    }

    fun shouldRetry(attempts: Int, maxAttempts: Int): Boolean =
        attempts < clampMaxAttempts(maxAttempts)
}
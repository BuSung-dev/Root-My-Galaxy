package dev.busung.s25uroot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetProfileTest {
    private val profile = TargetProfile(
        profileId = "galaxy-s25-series-kernel-6.6.98",
        displayName = "Galaxy S25 series",
        models = setOf("SM-S931B", "SM-S938N"),
        kernelVersions = setOf("6.6.98"),
        exploit = RemoteArtifact("https://example.invalid/exploit", 1),
        kernelSu = RemoteArtifact("https://example.invalid/ksud", 1),
    )

    @Test
    fun matchesRegionalS25OnSameKernelVersion() {
        assertTrue(profile.matches(snapshot("SM-S931B", "6.6.98-android15-8-build-a")))
        assertTrue(profile.matches(snapshot("SM-S938N", "6.6.98-android15-8-build-b")))
    }

    @Test
    fun rejectsUnlistedModelOrKernelVersion() {
        assertFalse(profile.matches(snapshot("SM-S928B", "6.6.98-android15-8-build")))
        assertFalse(profile.matches(snapshot("SM-S938N", "6.6.102-android15-8-build")))
    }

    @Test
    fun prefersProfileWithExactKernelRelease() {
        val zcs = profile.copy(
            profileId = "pa2q-S9360ZCSCCZG1",
            models = setOf("SM-S9360"),
            kernelVersions = setOf("6.6.98"),
        )
        val zhs = profile.copy(
            profileId = "pa2q-S9360ZHSCCZG1",
            models = setOf("SM-S9360"),
            kernelVersions = setOf(
                "6.6.98",
                "6.6.98-android15-8-pd6ff1cd-abogkiS9360ZHSCCZG1-4k",
            ),
        )

        val selected = listOf(zcs, zhs)
            .resolveFor(snapshot("SM-S9360", "6.6.98-android15-8-pd6ff1cd-abogkiS9360ZHSCCZG1-4k"))

        assertEquals("pa2q-S9360ZHSCCZG1", selected?.profileId)
    }

    @Test
    fun fallsBackToThreePartMatch() {
        val selected = listOf(profile)
            .resolveFor(snapshot("SM-S931B", "6.6.98-android15-8-build-a"))

        assertEquals("galaxy-s25-series-kernel-6.6.98", selected?.profileId)
    }

    @Test
    fun returnsNullWhenNothingMatches() {
        assertNull(listOf(profile).resolveFor(snapshot("SM-S928B", "6.6.98-android15-8-build-a")))
    }

    private fun snapshot(
        model: String,
        kernelRelease: String,
    ) = DeviceSnapshot(
        manufacturer = "samsung",
        model = model,
        device = "unused",
        kernelRelease = kernelRelease,
        kernelVersionInfo = "#1 SMP PREEMPT",
        machine = "aarch64",
        buildId = "BP4A.251205.006.S938BCZG1",
        fingerprint = "samsung/example",
        androidRelease = "16",
        sdk = 36,
        abi = "arm64-v8a",
        pageSize = 4096,
    )
}

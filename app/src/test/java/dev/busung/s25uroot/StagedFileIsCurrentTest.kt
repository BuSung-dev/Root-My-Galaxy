package dev.busung.s25uroot

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StagedFileIsCurrentTest {
    @Test
    fun restagesWhenContentDiffersAtIdenticalLength() {
        val source = file("payload", ByteArray(104128) { 0xAA.toByte() })
        val staged = file("staged", ByteArray(104128) { 0xBB.toByte() })

        assertTrue("fixture must reproduce the equal-length case", staged.length() == source.length())
        assertFalse(stagedFileIsCurrent(staged, source))
    }

    @Test
    fun reusesWhenContentIsIdentical() {
        val bytes = ByteArray(104128) { (it % 251).toByte() }

        assertTrue(stagedFileIsCurrent(file("staged", bytes), file("payload", bytes)))
    }

    @Test
    fun restagesWhenNothingIsStagedYet() {
        val source = file("payload", byteArrayOf(1, 2, 3))
        val missing = File(Files.createTempDirectory("stage-test").toFile(), "absent")

        assertFalse(stagedFileIsCurrent(missing, source))
    }

    @Test
    fun restagesWhenTheStagedFileCannotBeRead() {
        val source = file("payload", byteArrayOf(1, 2, 3))
        val unreadable = File(Files.createTempDirectory("stage-test").toFile(), "dir").apply { mkdirs() }

        assertFalse(stagedFileIsCurrent(unreadable, source))
    }

    @Test
    fun restagesWhenNeitherFileCanBeRead() {
        val root = Files.createTempDirectory("stage-test").toFile()
        val stagedDir = File(root, "staged").apply { mkdirs() }
        val sourceDir = File(root, "source").apply { mkdirs() }

        assertFalse(stagedFileIsCurrent(stagedDir, sourceDir))
    }

    @Test
    fun restagesWhenLengthsDifferToo() {
        val source = file("payload", ByteArray(104128) { 0xAA.toByte() })
        val staged = file("staged", ByteArray(4096) { 0xAA.toByte() })

        assertFalse(stagedFileIsCurrent(staged, source))
    }

    private fun file(name: String, bytes: ByteArray): File =
        File(Files.createTempDirectory("stage-test").toFile(), name).apply { writeBytes(bytes) }
}

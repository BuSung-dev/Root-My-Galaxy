package dev.busung.s25uroot

object NativeProbe {
    private var libraryLoaded: Boolean = false

    init {
        runCatching {
            System.loadLibrary("s25u_native")
            libraryLoaded = true
        }.onFailure {
            // Native library unavailable — external funcs will throw;
            // callers must check libraryLoaded before invoking them.
        }
    }

    external fun run(): String

    external fun isKernelSuActive(): Boolean

    /** Returns true if the native library loaded successfully. */
    fun isAvailable(): Boolean = libraryLoaded
}

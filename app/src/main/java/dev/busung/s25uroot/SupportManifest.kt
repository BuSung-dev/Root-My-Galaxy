package dev.busung.s25uroot

import org.json.JSONArray
import org.json.JSONObject

data class RemoteArtifact(
    val url: String,
    val size: Long,
    val verifySize: Boolean = true,
)

data class TargetProfile(
    val profileId: String,
    val displayName: String,
    val models: Set<String>,
    val kernelVersions: Set<String>,
    val exploit: RemoteArtifact,
    val kernelSu: RemoteArtifact,
) {
    init {
        require(models.isNotEmpty()) { "Payload must support at least one model" }
        require(kernelVersions.isNotEmpty()) { "Payload must support at least one kernel version" }
    }

    fun matchesDevice(snapshot: DeviceSnapshot): Boolean =
        models.any { it.equals(snapshot.model, ignoreCase = true) }

    fun matchesKernelVersion(snapshot: DeviceSnapshot): Boolean =
        snapshot.kernelVersion in kernelVersions

    fun matches(snapshot: DeviceSnapshot): Boolean =
        matchesDevice(snapshot) && matchesKernelVersion(snapshot)

    val supportedModels: String
        get() = models.joinToString()

    val supportedKernelVersions: String
        get() = kernelVersions.joinToString()
}

data class SupportManifest(
    val schemaVersion: Int,
    val targets: List<TargetProfile>,
) {
    companion object {
        fun parse(bytes: ByteArray): SupportManifest {
            val root = JSONObject(bytes.toString(Charsets.UTF_8))
            val schemaVersion = root.getInt("schemaVersion")
            require(schemaVersion == 3) { "Unsupported support manifest schema" }
            val payloadsJson = root.getJSONArray("payloads")
            val payloads = buildList {
                for (index in 0 until payloadsJson.length()) {
                    val payload = payloadsJson.getJSONObject(index)
                    val exploit = payload.getJSONObject("exploit")
                    val kernelSu = payload.getJSONObject("kernelsu")
                    add(
                        TargetProfile(
                            profileId = payload.getString("payloadId"),
                            displayName = payload.getString("displayName"),
                            models = payload.getJSONArray("models").strings(),
                            kernelVersions = payload.getJSONArray("kernelVersions").strings(),
                            exploit = RemoteArtifact(
                                url = exploit.getString("url"),
                                size = exploit.getLong("size"),
                                verifySize = exploit.optBoolean("verifySize", true),
                            ),
                            kernelSu = RemoteArtifact(
                                url = kernelSu.getString("url"),
                                size = kernelSu.getLong("size"),
                            ),
                        ),
                    )
                }
            }
            return SupportManifest(schemaVersion, payloads)
        }

        private fun JSONArray.strings(): Set<String> = buildSet {
            for (index in 0 until length()) add(getString(index))
        }
    }
}

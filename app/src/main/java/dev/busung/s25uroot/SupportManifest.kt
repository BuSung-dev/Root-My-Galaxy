package dev.busung.s25uroot

import org.json.JSONObject

data class RemoteArtifact(
    val url: String,
    val size: Long,
)

data class KernelSuArtifact(
    val artifact: RemoteArtifact,
    val kmi: String,
    val managerPackage: String,
)

data class TargetProfile(
    val profileId: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val buildDisplay: String,
    val buildFingerprint: String,
    val sdk: Int,
    val abi: String,
    val pageSize: Long,
    val exploit: RemoteArtifact,
    val kernelSu: KernelSuArtifact,
) {
    fun matchesModel(snapshot: DeviceSnapshot): Boolean =
        manufacturer.equals(snapshot.manufacturer, ignoreCase = true) &&
            model == snapshot.model &&
            device == snapshot.device

    fun matches(snapshot: DeviceSnapshot): Boolean =
        matchesModel(snapshot) &&
            buildDisplay == snapshot.buildId &&
            buildFingerprint == snapshot.fingerprint &&
            sdk == snapshot.sdk &&
            abi == snapshot.abi &&
            pageSize == snapshot.pageSize
}

data class SupportManifest(
    val schemaVersion: Int,
    val targets: List<TargetProfile>,
) {
    companion object {
        fun parse(bytes: ByteArray): SupportManifest {
            val root = JSONObject(bytes.toString(Charsets.UTF_8))
            val schemaVersion = root.getInt("schemaVersion")
            require(schemaVersion == 2) { "Unsupported support manifest schema" }
            val targetsJson = root.getJSONArray("targets")
            val targets = buildList {
                for (index in 0 until targetsJson.length()) {
                    val target = targetsJson.getJSONObject(index)
                    val exploit = target.getJSONObject("exploit")
                    val kernelSu = target.getJSONObject("kernelsu")
                    add(
                        TargetProfile(
                            profileId = target.getString("profileId"),
                            manufacturer = target.getString("manufacturer"),
                            model = target.getString("model"),
                            device = target.getString("device"),
                            buildDisplay = target.getString("buildDisplay"),
                            buildFingerprint = target.getString("buildFingerprint"),
                            sdk = target.getInt("sdk"),
                            abi = target.getString("abi"),
                            pageSize = target.getLong("pageSize"),
                            exploit = RemoteArtifact(
                                url = exploit.getString("url"),
                                size = exploit.getLong("size"),
                            ),
                            kernelSu = KernelSuArtifact(
                                artifact = RemoteArtifact(
                                    url = kernelSu.getString("url"),
                                    size = kernelSu.getLong("size"),
                                ),
                                kmi = kernelSu.getString("kmi"),
                                managerPackage = kernelSu.getString("managerPackage"),
                            ),
                        ),
                    )
                }
            }
            return SupportManifest(schemaVersion, targets)
        }
    }
}

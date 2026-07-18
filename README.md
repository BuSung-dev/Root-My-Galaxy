# Root My Galaxy

<img width="280" alt="KakaoTalk_20260718_170922353" src="https://github.com/user-attachments/assets/3f562ea4-8c39-4ade-bfd3-93eea1a1cc24" />
<img width="280" alt="KakaoTalk_20260718_170922353_02" src="https://github.com/user-attachments/assets/d6dd3be8-2383-46b7-9aed-b757ffdc3231" />
<img width="280" alt="KakaoTalk_20260718_170922353_01" src="https://github.com/user-attachments/assets/a7d06709-1f70-46e6-a8a8-2edbad0a3216" />


Root My Galaxy is a one-click installer for explicitly
supported Samsung firmware builds. The application itself is kept separate
from device offsets, native exploit payloads, and KernelSU build artifacts.

The signed device feed and native payloads are maintained in
[Root-My-Galaxy-Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads).
The standalone CVE proof of concept remains in
[CVE-2026-43499-S25U](https://github.com/BuSung-dev/CVE-2026-43499-S25U).

## Current target

```text
Samsung Galaxy S25 Ultra SM-S938N
pa3q / S938NKSUACZF1 / Android 16 / arm64-v8a / 4K
```

The app requires an exact signed match for the manufacturer, model, device,
full build display ID, full fingerprint, SDK, ABI, and page size.

## Download model

1. Resolve the current `main` commit of the payload repository.
2. Download the support manifest and Ed25519 signature from that exact commit.
3. Verify the signature with the public key pinned in the APK.
4. Match the complete device profile.
5. Download the exploit and KernelSU artifacts from the same immutable commit.

Per-artifact SHA-256 fields are intentionally not used. The signed manifest
and immutable Git commit are the trust and consistency boundaries.

## Interface

- Material 3 Expressive UI
- Tonal Spot palettes generated with the Material Color Utilities 2025 spec
- overview and settings navigation
- system dynamic, blue, violet, green, and orange Material palettes
- Korean, English, Japanese, and Simplified Chinese app languages
- dedicated non-dismissible installation screen with live native logs

## Build

Requirements:

- Android Studio JBR 21
- Android SDK 37
- Android NDK 28 or newer
- CMake 3.22.1

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Use only on devices you own or are explicitly authorized to test.

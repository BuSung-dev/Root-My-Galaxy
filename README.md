# Root My Galaxy

<img width="108" height="108" alt="sprout_icon_108" src="https://github.com/user-attachments/assets/2ba0e360-0876-489c-b256-f75df7589785" />

Root My Galaxy is a one-click installer for explicitly supported Samsung
firmware builds. The application itself is kept separate from device offsets,
native exploit payloads, and KernelSU build artifacts.

[Latest release](https://github.com/BuSung-dev/Root-My-Galaxy/releases)

The device feed and native payloads are maintained in
[Root-My-Galaxy-Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads).

## Application

<img width="200" alt="KakaoTalk_20260718_170922353" src="https://github.com/user-attachments/assets/3f562ea4-8c39-4ade-bfd3-93eea1a1cc24" />
<img width="200" alt="KakaoTalk_20260718_171127319" src="https://github.com/user-attachments/assets/8dde0443-12cf-4058-ba76-0337aefb92a0" />
<img width="200" alt="KakaoTalk_20260718_171030202" src="https://github.com/user-attachments/assets/f656e8af-60a6-4fcb-a3db-d4232bede613" />

The app automatically selects an exact match for the kernel release, complete
kernel version, full build display ID, SDK, ABI, and page size. Advanced mode
can select a profile manually and presents separate kernel-release and build
warnings.

## Tested SM-S938B profile

The companion payload contribution adds exact support for:

```text
Device: Samsung Galaxy S25 Ultra SM-S938B
Codename: pa3q
Build: BP4A.251205.006.S938BXXSBCZG3
Kernel: 6.6.98-android15-8-pd6ff1cd-abogkiS938BXXSBCZG3-4k
Android / SDK: 16 / 36
ABI: arm64-v8a
Page size: 4096
Bootloader: locked
Profile: pa3q-S938BXXSBCZG3
```

The exact profile was tested through the normal application flow. The exploit
acquired bootstrap root, staged the Samsung-KDP KernelSU build, and verified the
KernelSU control channel. Root is temporary: a complete device reboot removes
it, and the exploit must be run again.

The offsets, structure layouts, dedicated P0 fingerprint table, release exploit
binary, firmware hashes, and full hardware record are submitted separately in
[Root-My-Galaxy-Payloads PR #62](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads/pull/62).

## Zygisk on this post-boot root

This Root My Galaxy path is different from a normal boot-integrated KernelSU or
Magisk installation: KernelSU is loaded after Android and the current zygote
have already started. A Zygisk provider that only installs its usual boot-time
hooks can therefore appear installed while never injecting the running zygote.

The providers tested on the SM-S938B CZG3 device produced the following result:

- **Zygisk Next** was the only provider that worked without source changes. It
  became healthy after one user-initiated **Soft Reboot** from KernelSU Manager,
  and loaded Zygisk Assistant and LSPosed successfully.
- **Upstream NeoZygisk** did not work unchanged in this environment. Samsung
  DEFEX rejected the zygote opening the injection library from the persistent
  `/data/adb` module path, and the normal early-boot monitor lifecycle was
  already missed.
- **NeoZygisk PostBoot** is the open-source path validated for this root. The
  fork stages its live runtime under `/dev/.neozygisk`, attaches one monitor to
  init after the exploit, leaves the already-running zygote untouched, and
  waits for one Soft Reboot initiated from KernelSU Manager. It also rejects a
  deleted or mismatched monitor generation and performs live verification.
- **ReZygisk** was not compatible with the tested path. Its daemon could run,
  but zygote injection failed while loading the library from `/data/adb`.
  Restarting zygote directly reproduced Samsung's **Device Services
  Uninstalled** failure state and required a complete reboot.

The NeoZygisk fork was chosen because its source could be audited and adapted to
the post-boot lifecycle, its companion-FD behavior remained compatible with
Zygisk Assistant, and the final build worked with both Zygisk Assistant and
LSPosed. Zygisk Next remains the tested unmodified option; NeoZygisk PostBoot is
the tested open-source option. Do not install both providers at the same time.

### First installation

1. Run Root My Galaxy and confirm that KernelSU is active.
2. Install **Zygisk Next** or **NeoZygisk PostBoot**, not both.
3. Install or enable the dependent Zygisk modules.
4. Use **Soft Reboot** from KernelSU Manager once.
5. Wait for Android to return and verify the provider and modules.

Do not run `setprop ctl.restart zygote`, do not use the withdrawn automatic
ReZygisk bridge, and do not invoke a second Soft Reboot after a zygote crash.

### Updating the Zygisk provider

A provider update must not be activated while the old monitor is still alive in
the same kernel boot. The tested safe sequence is:

1. install the provider update, but do not Soft Reboot;
2. perform a complete device reboot;
3. run Root My Galaxy again;
4. confirm KernelSU is active;
5. use KernelSU Manager **Soft Reboot once**;
6. verify the new provider generation.

The complete test matrix, failure reproduction, NeoZygisk changes, and final
verification fields are recorded in
[`docs/SM-S938B-CZG3-POSTBOOT-ZYGISK.md`](docs/SM-S938B-CZG3-POSTBOOT-ZYGISK.md).

## Build

Requirements:

- Android Studio JBR 21
- Android SDK 37
- Android NDK 28 or newer
- CMake 3.22.1

```powershell
$env:JAVA_HOME='C:\Program Files\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Use only on devices you own or are explicitly authorized to test.

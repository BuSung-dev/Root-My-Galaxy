# Root My Galaxy

<img width="108" height="108" alt="sprout_icon_108" src="https://github.com/user-attachments/assets/2ba0e360-0876-489c-b256-f75df7589785" />


Root My Galaxy is a one-click installer for explicitly
supported Samsung model and kernel combinations. The application itself is kept separate
from device offsets, native exploit payloads, and KernelSU build artifacts.


[Latest release](https://github.com/BuSung-dev/Root-My-Galaxy/releases)

The device feed and native payloads are maintained in
[Root-My-Galaxy-Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads).

## Application

<img width="200" src="https://github.com/user-attachments/assets/da6d0a0a-e5aa-41c3-9b38-7ec01c08c3bd" />
<img width="200" src="https://github.com/user-attachments/assets/a9142c17-4e2f-4b18-8c03-fe17e531aa3a" />
<img width="200" src="https://github.com/user-attachments/assets/7af17f0c-7827-47c8-ab23-a985a019e972" />

The app selects a payload whose model list and three-part kernel version match
the phone. For example, `6.6.98-android15-8-...` matches `6.6.98`. Advanced
mode filters the catalog by both values and allows manual selection with model
and kernel-version warnings.

## Build

Requirements:

- JDK 21
- Android SDK 37 (`platforms;android-37.0`)
- Android NDK 28 or newer
- CMake 3.22.1

Point Gradle at your SDK by setting `sdk.dir` in `local.properties`, or export
`ANDROID_HOME` (or `ANDROID_SDK_ROOT`) before building.

### Linux / macOS

```bash
# One-time setup (adjust the SDK path as needed)
export JAVA_HOME="$JAVA_HOME"        # e.g. /usr/lib/jvm/java-21-openjdk
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
sdkmanager --install "platform-tools" "platforms;android-37.0" "build-tools;36.0.0" "cmake;3.22.1" "ndk;28.0.13004108"

./gradlew :app:assembleDebug
```

### Windows

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Use only on devices you own or are explicitly authorized to test.

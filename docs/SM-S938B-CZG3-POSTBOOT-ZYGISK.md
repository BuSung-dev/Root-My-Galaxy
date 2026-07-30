# SM-S938B CZG3 post-boot Zygisk test record

This document records the Zygisk work performed after adding Root My Galaxy
support for the international Galaxy S25 Ultra `SM-S938B` on
`S938BXXSBCZG3`. It is specific to the temporary, post-boot KernelSU session
created by this exploit path.

## Test device

```text
Device: Samsung Galaxy S25 Ultra SM-S938B
Codename: pa3q
Build: BP4A.251205.006.S938BXXSBCZG3
Kernel: 6.6.98-android15-8-pd6ff1cd-abogkiS938BXXSBCZG3-4k
Android / SDK: 16 / 36
ABI: arm64-v8a
Page size: 4096
Bootloader: locked
Root: temporary KernelSU loaded by Root My Galaxy after Android boot
```

The exact exploit payload, target symbols, structure layouts, P0 fingerprint
table and firmware hashes are part of the companion payload contribution:
[Root-My-Galaxy-Payloads PR #62](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads/pull/62).

## Why the normal provider lifecycle is not enough

Root My Galaxy late-loads KernelSU after Android has already started. By that
point `init`, `zygote64`, `system_server` and the application processes already
exist. A provider can install correctly in `/data/adb/modules` and even start a
daemon without having observed or injected the zygote generation that is
currently running.

This is the important difference from a KernelSU installation integrated into
the boot image. The fix cannot simply be “install a provider after root.” The
provider must either support the already-running system or be present before a
new Android userspace generation is created.

## Provider results

### Zygisk Next

Zygisk Next was the only provider tested here that worked in its normal,
unmodified form.

The tested build reported:

```text
name=Zygisk Next
version=1.4.3-817-e815170-release
root_status=KernelSU
zygote_states=1
inject_state=1
```

After the Root My Galaxy exploit, the modules were installed or enabled and one
**Soft Reboot** was started from KernelSU Manager. The new zygote generation was
injected and the tested dependent modules loaded:

```text
zygisk-assistant
zygisk_lsposed
```

No Root My Galaxy change or provider patch was required. This is the tested
choice for users who want the provider to work as released.

### ReZygisk

ReZygisk did not reach a working injected state on this firmware.

Its monitor/daemon processes could start, but the current zygote remained
uninjected. The diagnostic log included a failure while opening and mapping the
persistent module library:

```text
Failed to open remote file /data/adb/modules/rezygisk/lib64/libzygisk.so
remote CSOLoader mapping failed
Zygote64 restart too much times, stop injecting
```

Manual Start/Stop/Exit attempts and a targeted zygote restart were not a safe
recovery path. A direct:

```text
setprop ctl.restart zygote
```

reproduced Samsung's **Device Services Uninstalled** state. The phone stopped
returning to a usable Android session and required a complete reboot. The
automatic ReZygisk bridge and targeted-restart experiment were withdrawn.

### Upstream NeoZygisk

The upstream module was not directly compatible with this post-boot session for
two separate reasons:

1. its usual monitor and zygote relationship is established during the normal
   module/boot lifecycle, which had already been missed;
2. Samsung DEFEX rejected the root-credential zygote opening the injection
   library from the persistent `/data/adb` module path.

Starting a daemon alone did not solve either problem. Restarting zygote directly
was already known to be unsafe on this firmware.

## NeoZygisk PostBoot changes

NeoZygisk was retained as the open-source option because the relevant lifecycle
could be changed and audited without altering the Root My Galaxy exploit or the
KernelSU payload. Its existing companion-FD behavior also matched the flow used
by Zygisk Assistant, avoiding a redesign of that module's IPC lifecycle.

The validated fork made the following changes.

### DEFEX-safe runtime

The live injection library and sockets are staged under the existing
kernel-backed tmpfs path:

```text
/dev/.neozygisk
```

The persistent package remains in `/data/adb/modules/zygisksu`, but the zygote
maps:

```text
/dev/.neozygisk/lib64/libzygisk.so
```

The runtime was staged atomically and labelled so the zygote could open it. This
removed the `/data/adb` open failure seen in the unsuccessful providers.

### Post-boot monitor bootstrap

After Root My Galaxy makes KernelSU available, the module starts or reuses
exactly one `zygisk-ptrace64` monitor attached to `init`. It fails closed when:

- another tracer is already attached to `init`;
- more than one monitor exists;
- the running monitor executable is marked `(deleted)`;
- the running monitor hash differs from the installed binary;
- the runtime library hash differs from the installed library;
- the runtime reports `stopped(zygote crashed)`.

The existing zygote is intentionally left untouched. The module does not try to
inject that already-running process and does not request a targeted restart.

### External Android userspace lifecycle

Once the monitor is healthy, the user starts **Soft Reboot from KernelSU
Manager once**. This creates a new zygote generation while the monitor is
already attached to `init`.

The module itself contains no command for:

- `ksud soft-reboot`;
- `ctl.restart zygote`;
- device reboot;
- process killing;
- in-session recovery of a crashed monitor.

### Live verification

The final verifier recalculates the state every time. It does not treat an old
success file as proof that the current zygote is still injected.

The successful recovery run reported:

```text
PHASE=3.2
RESULT=INJECTION_VERIFIED
DETAIL=healthy same-generation NeoZygisk state verified
WORK=/dev/.neozygisk
RESTART_TRIGGERED_BY_MODULE=0
TARGETED_ZYGOTE_RESTART_USED=0
GLOBAL_SOFT_REBOOT_USED_BY_MODULE=0
MANUAL_KERNELSU_SOFT_REBOOT_REQUIRED=0
FULL_REBOOT_REQUIRED=0
BOOTSTRAP_RESULT=HEALTHY
MONITOR_HEALTHY=1
MONITOR_PID=13835
INIT_TRACER=13835
MONITOR_EXE_DELETED=0
MONITOR_BINARY_MATCH=1
RUNTIME_LIBRARY_MATCH=1
RUNTIME_MONITOR_CRASHED=0
ZYGOTE_PID=24563
SYSTEM_SERVER_PID=24853
DAEMON_PID=24565
RUNTIME_PROP_INJECTED=1
RUNTIME_PROP_DAEMON_RUNNING=1
ACTIVITY_READY=1
CP64_SOCKET_READY=1
LIBRARY_MAPPED_IN_ZYGOTE=1
```

The runtime state also listed both tested modules:

```text
monitor:  tracing
zygote64: injected
daemon64: running
Modules:
  zygisk-assistant
  zygisk_lsposed
```

## Provider update regression

Installing a newer NeoZygisk package while the old monitor remained alive, then
using KernelSU Soft Reboot in the same kernel boot, reproduced a mixed-generation
failure:

```text
monitor: stopped(zygote crashed)
zygote64: unknown
daemon64: running
```

An older verifier also continued printing a previously stored
`INJECTION_VERIFIED` result. The stable fork corrected both issues by checking
the live state and by comparing the running monitor and staged library with the
currently installed generation.

A provider update now requires a cold boundary:

1. install the provider update;
2. do not Soft Reboot in that kernel session;
3. perform a complete device reboot;
4. rerun the normal Root My Galaxy exploit;
5. use KernelSU Manager Soft Reboot once;
6. verify the new provider generation.

After `zygote crashed`, a deleted monitor, a generation mismatch or
`FULL_REBOOT_REQUIRED`, another Soft Reboot must not be attempted in the same
kernel boot.

## Supported choices for this profile

The hardware result supports two specific paths:

- **Zygisk Next**: tested working without provider source changes;
- **NeoZygisk PostBoot**: tested open-source fork with the Samsung post-boot and
  DEFEX changes described above.

This is not a claim that every Zygisk provider is interchangeable. ReZygisk
failed on the tested path, and upstream NeoZygisk required the post-boot fork.
Only one provider should be installed at a time.

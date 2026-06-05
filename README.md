# Dev Controls

A personal Android control surface for a single non-rooted phone (developed on a
Moto Edge 50 Pro, Android 14/15) that explores how far a normal app can be
pushed **without root, without Shizuku, and without device-owner** — then uses
those capabilities to security-test the author's own production app.

> Personal build, not on Play Store. Granting `WRITE_SECURE_SETTINGS` and friends
> via ADB is part of normal setup, not an exploit.

- `applicationId` / namespace: `pro.jayeshseth.devcontrols`
- `minSdk = 30` (Android 11), `targetSdk = 36`, `versionName = 1.0`
- Kotlin 2.2.10, AGP 9.2.1, Compose BOM 2026.02.01, Glance 1.1.1, DataStore 1.1.7
- libadb-android 3.1.1 + sun-security-android 1.1 + conscrypt-android 2.5.3 (JitPack)

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n pro.jayeshseth.devcontrols/.MainActivity
```

For the code-level walkthrough (every package and file, the shared patterns, the
data flow) see **[ARCHITECTURE.md](ARCHITECTURE.md)**. For the offensive/defensive
notes on hardening the production app see **[SECURITY_HARDENING.md](SECURITY_HARDENING.md)**.

---

## How to read this: the two dependency axes

Every feature sits somewhere on two independent axes. **This is the structure of
the app** — nothing else about the package layout matters as much as these two
questions:

1. **Does it need the accessibility service *bound and running*?** Only the three
   features that react to "which app is now in the foreground" do. Everything else
   is independent of accessibility — including, counter-intuitively, the
   Accessibility *Cloak* (which works by *unbinding* the service) and the
   Accessibility *Detection* screen (which only reads).
2. **Does it need a special, ADB-granted privilege?** i.e. anything beyond the
   normal manifest permissions the OS grants automatically. The three buckets are:
   *secure-settings* (`WRITE_SECURE_SETTINGS` / `WRITE_SETTINGS` / `GET_USAGE_STATS`
   appops), *embedded-ADB pairing* (a one-time TLS pair with the device's own
   wireless-debug server), and *nothing* (drop-in, runs even inside a locked-down
   third-party app).

### Master matrix

| Feature | A11y service running? | Special privilege needed | Entry point |
| --- | :---: | --- | --- |
| Glance widget | ✗ | secure-settings (for the toggle cells) | `widget/DevControlsWidget.kt` |
| Quick Settings tiles (×6) | ✗ | secure-settings (for the toggle tiles) | `tile/` |
| Main status panel | ✗ | none (read-only) | `MainActivity.kt` |
| 4 setting toggles (dev / USB / Wi-Fi ADB / timeout) | ✗ | `WRITE_SECURE_SETTINGS` + `WRITE_SETTINGS` | `data/DevSettingsRepository.kt` |
| **Kiosk Mode** | **✓ required** | `WRITE_SECURE_SETTINGS` (kiosk flag + fast-anim) | `kiosk/KioskAccessibilityService.kt` |
| **Hide Dev** | **✓ required** | `WRITE_SECURE_SETTINGS` | `ui/HideDevScreen.kt` |
| **Force Dark** | **✓ required** | `WRITE_SECURE_SETTINGS` | `ui/ForceDarkScreen.kt` |
| Focus Calls | ✗ | `READ_PHONE_STATE`, `ANSWER_PHONE_CALLS`, `GET_USAGE_STATS`; opt. DIALER role | `call/PhoneStateReceiver.kt` |
| `adb_c` wireless ADB console | ✗ | embedded-ADB pairing | `ui/AdbcScreen.kt` |
| `adb_pm` permission manager | ✗ | embedded-ADB pairing | `ui/PermissionManagerScreen.kt` |
| App Info Dump | ✗ | embedded-ADB pairing | `ui/InfoDumpScreen.kt` |
| **Accessibility Cloak** | ✗ (it *unbinds* a11y) | `WRITE_SECURE_SETTINGS` + `GET_USAGE_STATS` | `cloak/A11yCloakService.kt` |
| `adb_g` local shell | ✗ | **none** (runs as own UID) | `ui/AdbConsoleScreen.kt` |
| Haptics Lab | ✗ | **none** (`VIBRATE` is auto-granted) | `ui/HapticsLabScreen.kt` |
| USB Inspector | ✗ | **none** | `ui/UsbInspectorScreen.kt` |
| Predictive Back playground | ✗ | **none** | `ui/PredictiveBackPlaygroundScreen.kt` |
| ADB Detection | ✗ | **none** (pure reads) | `ui/AdbDetectionScreen.kt` |
| Accessibility Detection | ✗ | **none** (pure reads) | `ui/A11yDetectionScreen.kt` |

### Grouped the way you'll actually think about it

**Needs the accessibility service running** (all three live in the one
`KioskAccessibilityService`, multiplexed off its window-state event stream):

- **Kiosk Mode** — relaunch the target when the launcher appears.
- **Hide Dev** — zero dev-options / USB-debug while a target app is foreground.
- **Force Dark** — invert colours while a target app is foreground.

> If accessibility is **off**, these three silently do nothing; every other
> feature is unaffected.

**Needs an ADB-granted secure privilege but *not* accessibility:**

- The four core toggles + their widget cells & QS tiles (`WRITE_SECURE_SETTINGS`,
  `WRITE_SETTINGS`).
- **Accessibility Cloak** (`WRITE_SECURE_SETTINGS` + `GET_USAGE_STATS`).
- **Focus Calls** (`READ_PHONE_STATE`, `ANSWER_PHONE_CALLS`, `GET_USAGE_STATS`;
  optional `DIALER` role for the zero-flash variant).

**Needs an embedded-ADB pairing (and nothing else):**

- `adb_c` console, `adb_pm` permission manager, App Info Dump.

**Needs *nothing* — drop-in, works even inside a hardened third-party app:**

- `adb_g` local shell, Haptics Lab, USB Inspector, Predictive Back playground,
  ADB Detection, Accessibility Detection.

> The last bucket matters: ADB Detection and Accessibility Detection take no
> special permission *on purpose* — every probe they run is exactly what the
> hardened production app can run as-is. They're the source of the copy-paste
> detection snippets in those screens.

---

## One-time setup grants

The in-app permission banner shows what's missing with the exact command. After a
fresh install:

```sh
# Secure-settings tier — toggles, Kiosk flag, Hide Dev, Force Dark, Cloak
adb shell pm grant pro.jayeshseth.devcontrols android.permission.WRITE_SECURE_SETTINGS
# WRITE_SETTINGS is role-managed on A14+, so use appops (pm grant is rejected):
adb shell appops set pro.jayeshseth.devcontrols WRITE_SETTINGS allow
# Foreground detection — Focus Calls + Accessibility Cloak poller
adb shell appops set pro.jayeshseth.devcontrols GET_USAGE_STATS allow
# Allow ADB-enabling our own accessibility service on A14+ ("restricted settings")
adb shell appops set pro.jayeshseth.devcontrols ACCESS_RESTRICTED_SETTINGS allow

# Focus Calls
adb shell pm grant pro.jayeshseth.devcontrols android.permission.READ_PHONE_STATE
adb shell pm grant pro.jayeshseth.devcontrols android.permission.ANSWER_PHONE_CALLS
```

Enable the accessibility service (preserving any already-enabled services) — needed
**only** for Kiosk / Hide Dev / Force Dark:

```sh
existing=$(adb shell settings get secure enabled_accessibility_services | tr -d '\r')
adb shell settings put secure enabled_accessibility_services \
  "$existing:pro.jayeshseth.devcontrols/pro.jayeshseth.devcontrols.kiosk.KioskAccessibilityService"
adb shell settings put secure accessibility_enabled 1
```

Embedded-ADB pairing (needed for `adb_c` / `adb_pm` / Info Dump) is done **inside
the app**: open `adb_c` → **Pair** → enter the 6-digit code from Settings →
Developer options → Wireless debugging → Pair device with pairing code. No PC.

Optional — Focus Calls zero-flash variant only:

```sh
adb shell cmd role add-role-holder android.app.role.DIALER pro.jayeshseth.devcontrols
```

---

## Feature catalog

### Settings toggles & surfaces

**Four core toggles** — `data/DevSettingsRepository.kt`. Developer options
(`DEVELOPMENT_SETTINGS_ENABLED`), USB debugging (`ADB_ENABLED`), wireless debugging
(`ADB_WIFI_ENABLED`), never-timeout (`SCREEN_OFF_TIMEOUT` swapped to MAX and back).
The first three are `Settings.Global` (`WRITE_SECURE_SETTINGS`); the timeout is
`Settings.System` (`WRITE_SETTINGS`). All exposed as `ContentObserver`-backed Flows
so every surface stays live.

**Glance widget** — `widget/DevControlsWidget.kt`. A 6-cell widget plus an endpoint
row showing the auto-discovered wireless-debug `IP:PORT` (tap to copy). State is
read synchronously inside `provideGlance` (Glance's `collectAsState` is unreliable),
and refreshed three ways: a `ContentObserver` per tracked setting
(`DevSettingsRepository.registerWidgetRefresh`), the `WidgetRefresher` collecting
DataStore + NSD flows, and an immediate `updateAll` after each cell tap.

**Quick Settings tiles** — `tile/`. Six tiles: four simple toggles on
`BaseToggleTile`, a DataStore-backed `FocusCallsTile`, and `KioskModeTile` (opens
the picker if no target, else starts kiosk + launches target + collapses the shade).

### Accessibility-driven (needs the service running)

**Kiosk Mode** — `kiosk/KioskAccessibilityService.kt`. Motorola's launcher strips
the OS "Pin" option from Recents, so kiosk is done by an accessibility service:
when kiosk is active and the launcher foregrounds, it relaunches the chosen target.
Supporting pieces: `KioskPrefs` (target + active flag), `KioskNotification` (pinned,
with Stop action → `KioskControlReceiver`), `AnimationScales` (zero animation scales
to shave the relaunch flash, with escrow restore). `admin/KioskAdminReceiver` is a
dormant `DeviceAdminReceiver` stub for a *future* device-owner upgrade — not active.

**Hide Dev** — `ui/HideDevScreen.kt` + `KioskAccessibilityService.handleHideDev`.
Geto-style per-app hiding without Shizuku: while an allow-listed app is foreground
the service snapshots and zeroes `DEVELOPMENT_SETTINGS_ENABLED` + `ADB_ENABLED`
(and optionally `ADB_WIFI_ENABLED`), restoring on leave. Escrow lives in
`HideDevPrefs`. See ARCHITECTURE.md for the escrow pattern and the split-screen
limitation.

**Force Dark** — `ui/ForceDarkScreen.kt` + `handleForceDark`. While an allow-listed
app is foreground, sets `Settings.Secure.accessibility_display_inversion_enabled`
to invert colours (a dark-mode shim for apps without their own), restoring on leave.

### Calls

**Focus Calls** — `call/`. When a call rings while you're in an allow-listed app,
don't get yanked into the full-screen dialer. Two mutually exclusive architectures
(`FocusCallsPrefs`): **A — Relaunch** (default): `PhoneStateReceiver` re-launches
the stashed app on `OFFHOOK`; brief dialer flash. **B — Default dialer**: take the
`DIALER` role + `FocusInCallService` that never starts a full-screen UI; zero flash
but replaces the dialer for all calls. Optional auto-answer via
`TelecomManager.acceptRingingCall()`.

### Embedded ADB (needs a one-time pairing)

**ADB client** — `adb/`. Self-contained client that pairs with the device's own
wireless-debug TLS server over loopback — Shizuku-grade shell capability with
Shizuku *not* installed. `AdbConnectionManagerImpl` (RSA key + X.509 cert,
persisted), `AdbClient` (mutex-serialised `pair`/`exec`), `AdbPairingDiscoverer` &
`AdbEndpointDiscoverer` (NSD for the pairing and connect ports), `PairingService`
(foreground `specialUse` svc owning the pinned notification with a `RemoteInput`
code field), `AdbRecipes` (one-tap commands).

**`adb_c`** — `ui/AdbcScreen.kt`. Wireless-ADB REPL (UID 2000) + recipe chips +
pairing status strip.

**`adb_pm`** — `ui/PermissionManagerScreen.kt`. Shizuku-style: pick an app, toggle
runtime perms (`pm grant/revoke`) and appops (`appops set`, mapping in
`PermissionMapping.kt`), plus custom-grant field.

**App Info Dump** — `ui/InfoDumpScreen.kt` + `adb/InfoDumpRecipes.kt`. Runs ~16
shell recipes against a chosen app (paths, signatures, granted perms, components,
native libs, external storage, `run-as` access, process, network, services, file
scan, logs) — used to audit what the author's own prod app leaks once ADB bypasses
its checks.

### No special permission (drop-in)

**`adb_g`** — `ui/AdbConsoleScreen.kt`. Local `Runtime.exec("sh"/"su")` REPL at the
app's own UID; a baseline to compare against `adb_c`'s shell UID.

**Haptics Lab** — `ui/HapticsLabScreen.kt` + `haptics/`. Exhaustive vibration
playground: predefined effects, waveforms, primitives/compositions,
`HapticFeedbackConstants`, amplitude control.

**USB Inspector** — `ui/UsbInspectorScreen.kt` + `usb/`. Enumerates connected USB
devices/interfaces/endpoints via `UsbManager`, plus charging/power state from
`power_supply` sysfs.

**Predictive Back playground** — `ui/PredictiveBackPlaygroundScreen.kt`. Live
readout of `PredictiveBackHandler` — swipe progress, edge, velocity, commit/cancel.

**ADB Detection** — `ui/AdbDetectionScreen.kt` + `util/AdbDetection.kt`. 16 probes
(settings flags, debugger, `TracerPid`, build tags/type, debuggable flags, `getprop`
values, port probes for adbd/frida, su binaries) each with a copy-paste Kotlin
snippet for the prod app. Pure reads — no permission.

**Accessibility Detection** — `ui/A11yDetectionScreen.kt` + `util/A11yDetection.kt`.
Enumerates bound accessibility services and grades them (system vs third-party,
reads-window-content, performs-gestures), explicitly flagging this app's own
service. Four copy-paste detection recipes. Pure reads — no permission.

**Accessibility Cloak** — `cloak/A11yCloakService.kt` + `ui/A11yCloakScreen.kt`.
The counter-measure: a `UsageStats`-driven foreground service that, while a target
app is foreground, *strips this app's service from* `enabled_accessibility_services`
so detectors see a clean list, restoring on leave. Independent of accessibility by
design (it's what rebinds the unbound service). Optional "also hide dev options"
covers the dev/USB flags during the same window since a11y-based Hide Dev can't run
while unbound. Needs `WRITE_SECURE_SETTINGS` + `GET_USAGE_STATS`, not accessibility.

---

## Full permission matrix

| Permission / appop / role | Tier | Used by | Granted how |
| --- | --- | --- | --- |
| `WRITE_SECURE_SETTINGS` | secure | 4 toggles, Kiosk flag, Hide Dev, Force Dark, Cloak, fast-anim | `pm grant` (once) |
| `WRITE_SETTINGS` | secure | screen-timeout (`Settings.System`) | `appops set … allow` (role-managed on A14+) |
| `GET_USAGE_STATS` (`PACKAGE_USAGE_STATS`) | secure | Focus Calls + Cloak foreground detection | `appops set … allow` |
| `ACCESS_RESTRICTED_SETTINGS` | secure | one-time, to ADB-enable our a11y service on A14+ | `appops set … allow` |
| `READ_PHONE_STATE` | secure | `PHONE_STATE` broadcasts | `pm grant` |
| `ANSWER_PHONE_CALLS` | secure | auto-answer | `pm grant` |
| `android.app.role.DIALER` | role | Focus Calls Arch B only | `cmd role add-role-holder` |
| (embedded-ADB pairing) | pairing | `adb_c`, `adb_pm`, Info Dump | in-app pairing, no PC |
| `INTERNET` | normal | NSD + loopback ADB sockets | manifest |
| `ACCESS_WIFI_STATE` / `ACCESS_NETWORK_STATE` | normal | Wi-Fi IP for endpoint display | manifest |
| `QUERY_ALL_PACKAGES` | normal | installed-apps pickers | manifest |
| `POST_NOTIFICATIONS` | runtime | pairing / call / kiosk / cloak notifications | runtime prompt (A13+) |
| `MANAGE_OWN_CALLS` | normal | Focus Calls Arch B | manifest |
| `FOREGROUND_SERVICE` (+ `_SPECIAL_USE`, `_SHORT_SERVICE`) | normal | Pairing svc, Cloak svc | manifest |
| `REORDER_TASKS` | normal | Kiosk / Focus Calls relaunch flag | manifest |
| `VIBRATE` | normal | Haptics Lab | manifest |
| `BIND_ACCESSIBILITY_SERVICE` | system | Kiosk / Hide Dev / Force Dark | granted on service-enable |
| `BIND_INCALL_SERVICE` | system | Focus Calls Arch B | granted when default dialer |
| `BIND_QUICK_SETTINGS_TILE` | system | all six tiles | granted on tile add |
| `BIND_DEVICE_ADMIN` | system | dormant device-owner stub | granted on enable |

---

## Caveats / what can't be done on this device

- OS-level lock-task to a third-party app needs device-owner; the accessibility
  relaunch leaves a ~200–500 ms launcher flash (Fast Animations shrinks it).
- Hide Dev / Force Dark fire a few hundred ms after a target foregrounds — an app
  reading the flag in its very first frame can beat them (apps that re-check are
  covered). Same race applies to the Cloak's ~0.4–1 s poll.
- Hide Dev / Force Dark are single-foreground-app; in **split screen** the
  co-resident non-target window keeps tripping the "restore" branch, so they
  effectively don't engage (see ARCHITECTURE.md).
- While the Cloak is active its service is unbound, so Kiosk / Hide Dev / Force
  Dark pause for that window.
- `screencap` blanks `FLAG_SECURE` windows. `/system`, kernel, SELinux need root.
- Focus Calls Arch A always shows a brief dialer flash; Arch B is the zero-flash path.

---

## Verification cheatsheet

| Feature | How to verify |
| --- | --- |
| Widget toggles | tap cell → state flips instantly |
| Cross-surface refresh | flip USB debug in Settings → widget updates within ~1 s |
| QS tiles | shade → edit tiles → add any of 6 → toggle |
| Kiosk Mode | pick target → Start → swipe home → bounce back |
| Hide Dev | allow-list app → open it → `settings get global development_settings_enabled` = 0; leave → restored |
| Force Dark | allow-list app → open it → colours invert; leave → normal |
| Focus Calls A | allow-list Maps → call in Maps → snaps back after answer |
| Focus Calls B | grant DIALER → call shows only as heads-up notification |
| ADB pair | `adb_c` → Pair → enter code → connected |
| `adb_pm` | pick app → toggle perm → `dumpsys package <pkg>` shows grant |
| Info Dump | pick prod app → recipes return paths/sigs/perms |
| ADB / A11y Detection | open → severity dots populate; Copy snippet works |
| A11y Cloak | start poller → open target → `settings get secure enabled_accessibility_services` no longer lists us; leave → restored |

---

## Project history

- **v1** — Glance widget, four toggles, four QS tiles, ADB endpoint discovery, status panel.
- **v2** — widget refresh fix, Kiosk Mode (accessibility relaunch), Focus Calls A+B.
- **v3** — embedded ADB client, pinned-notification pairing, `adb_c`, `adb_pm`, Hide Dev.
- **v3.1** — `WidgetRefresher`, hackery icon, rename to Dev Controls / `pro.jayeshseth.devcontrols`.
- **v4** — Haptics Lab, USB Inspector, Predictive Back playground, Force Dark.
- **v5** — kill-via-ADB, on-launch notification permission + kiosk gate, App Info Dump.
- **v6** — ADB Detection, Hide Dev extended to wireless ADB.
- **v7** — Accessibility Detection, Accessibility Cloak; README + ARCHITECTURE rewrite.

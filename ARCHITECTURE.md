# Dev Controls — Architecture & code reference

Code-level companion to [README.md](README.md). The README answers *"what can it
do and what does each feature need"*; this file answers *"how is the code laid out
and why"*. Read the README's **two dependency axes** section first — the whole app
is organised around them.

---

## The package layout, and why it's by-type not by-feature

```
pro/jayeshseth/devcontrols/
├── MainActivity.kt        top-level Compose routing + status panel
├── DevControlsApp.kt      Application: Conscrypt, channels, refreshers, NSD start
├── adb/                   embedded ADB client + recipes + pairing service
├── admin/                 dormant DeviceAdminReceiver stub
├── call/                  Focus Calls (both architectures)
├── cloak/                 Accessibility Cloak foreground service
├── data/                  DataStore prefs + the Settings repository + NSD discoverer
├── haptics/               vibration engine + presets
├── kiosk/                 the accessibility service + kiosk support
├── tile/                  six Quick Settings tiles
├── ui/                    one Composable screen per feature + theme
├── usb/                   USB enumeration + power models
├── util/                  stateless helpers + the two Detection probe sets
└── widget/                Glance widget + refreshers + tap dispatchers
```

Packages group by **technical role** (data / ui / service / tile / widget), the
Android-idiomatic layout, *not* by feature. A single feature is therefore spread
across packages — e.g. **Hide Dev** = `ui/HideDevScreen.kt` (UI) +
`data/HideDevPrefs.kt` (state + escrow) + `kiosk/KioskAccessibilityService.kt`
(engine). The feature→requirement mapping that *does* cut across this is the
matrix in the README; this file documents the code role-by-role.

> A by-feature reshuffle (`feature/hidedev/…`) was deliberately **not** done: it
> would scatter UI/data/service concerns and force ~60 package moves with no VCS
> safety net. The README matrix is the feature view; the packages stay by-type.

---

## Cross-cutting patterns

These four patterns recur; understanding them once explains most files.

### 1. The accessibility service is multiplexed
`kiosk/KioskAccessibilityService.kt` is the **only** accessibility service, and
three unrelated features ride its single `onAccessibilityEvent` stream because all
three react to the same trigger — *"a new app came to the foreground"*:
`handleKiosk`, `handleHideDev`, `handleForceDark`. Each reads its own DataStore
prefs and no-ops if its feature is disabled. The service is the **eyes**
(foreground awareness); the **hands** (changing settings) are `WRITE_SECURE_SETTINGS`.

### 2. The escrow / snapshot-restore pattern
Any feature that *temporarily* mutates a global setting saves the prior value
before changing it, and restores exactly that on exit — never assumes the prior
state. Implemented in `HideDevPrefs.Escrow`, `ForceDarkPrefs`, `A11yCloakPrefs.Escrow`,
`TimeoutPrefs`, and `kiosk/AnimationScales`. The escrow carries an `active` flag so
re-entry doesn't re-snapshot an already-modified value (e.g. switching between two
hidden apps doesn't overwrite the saved "real" value with the hidden `0`).

### 3. DataStore-per-feature
Each feature owns one `preferencesDataStore` file (`HideDevPrefs`, `KioskPrefs`,
`FocusCallsPrefs`, `ForceDarkPrefs`, `A11yCloakPrefs`, `AdbConsolePrefs`,
`TimeoutPrefs`). Each exposes `Flow`s for Compose `collectAsState` and `…Once()`
suspend reads for services that need a one-shot value (`runBlocking` inside the
accessibility callback, suspend inside the cloak poller).

### 4. ContentObserver-backed setting Flows
`DevSettingsRepository` wraps each tracked `Settings.Global`/`Settings.System` key
in a `callbackFlow` over a `ContentObserver`, so a change from *any* source (this
app, the Settings UI, ADB) propagates to the widget, tiles, and status panel
identically. `registerWidgetRefresh()` fans the same observers out to `updateAll`.

---

## Package-by-package

### `MainActivity.kt`
Single `ComponentActivity`. Holds a `Route` enum and renders one screen at a time
(no Navigation library — the route set is small and flat). On launch it requests
`POST_NOTIFICATIONS` and reads the permission/appop state for the status panel.
The `DevControlsScreen` composable is the status panel + the column of feature
buttons; each button flips `route`. Kiosk start is gated on notifications + a11y.

### `DevControlsApp.kt`
`Application`. On `onCreate`: installs the Conscrypt security provider (needed for
the TLS-PSK handshake during ADB pairing), creates notification channels, calls
`DevSettingsRepository.registerWidgetRefresh()`, starts `WidgetRefresher`, and
starts `AdbEndpointDiscoverer` so the widget can show the connect endpoint.

### `adb/`
- **`AdbConnectionManagerImpl`** — `AbsAdbConnectionManager` subclass (libadb-android).
  Generates/persists an RSA-2048 key + self-signed X.509 cert (sun-security-android)
  in `filesDir`; the device trusts this key after the one-time pair.
- **`AdbClient`** — facade: `suspend pair(host, port, code)` and
  `suspend exec(command): AdbExecResult`. A single `Mutex` serialises everything
  because the underlying `AdbStream` isn't coroutine-safe. `exec` reads the stream
  into a `ByteArrayOutputStream` in a loop that tolerates a mid-read close (avoids
  the "stream closed" error that `readText()` produced). Pre-flight TCP-probes the
  pairing port to give a clear "reopen the pairing dialog" message.
- **`AdbExecResult`** — `(stdout, stderr, exit)` data class.
- **`AdbPairingDiscoverer`** / **`data/AdbEndpointDiscoverer`** — NSD listeners for
  `_adb-tls-pairing._tcp` (ephemeral, only while the system pair dialog is open) and
  `_adb-tls-connect._tcp` (long-lived). Both build a fresh `ResolveListener` per
  `resolveService` call — sharing one throws "listener already in use" and kills the
  ConnectivityThread.
- **`PairingService`** — foreground `specialUse` service (no 3-min short-service cap)
  owning the pinned pairing notification. The "Enter code" action is a `RemoteInput`
  whose `PendingIntent` is `getService` (not `getBroadcast` — the broadcast hop drops
  the `ClipData` payload). States: WaitingForService / Ready / Pairing / Success / Error.
- **`AdbRecipes`** — one-tap command registry for `adb_c`.
- **`InfoDumpRecipes`** — ~16 `DumpSection` shell recipes for App Info Dump.
- **`PermissionMapping`** — permission↔appop translation table for `adb_pm`.

### `admin/KioskAdminReceiver.kt`
Dormant `DeviceAdminReceiver` stub. Present so the user *could* later
`dpm set-device-owner …` for real OS lock-task without a manifest rewrite. Not
wired to anything today.

### `call/`
- **`PhoneStateReceiver`** — Arch A. On `RINGING`, if Focus Calls is on and the
  foreground app (via `ForegroundAppDetector`) is allow-listed, stash it (and
  auto-answer if enabled); on `OFFHOOK`, relaunch the stashed app.
- **`FocusInCallService`** — Arch B. `InCallService` that never starts a full-screen
  activity; calls surface only via `CallNotifications`.
- **`CallNotifications`** — heads-up call notifications (channel `focus_calls`) with
  Answer / Decline / Hangup.

### `cloak/A11yCloakService.kt`
Foreground `specialUse` service. Polls `ForegroundAppDetector` every 400 ms (the
independent "eyes" that survive unbinding). On a target foreground: snapshot
`enabled_accessibility_services` + `accessibility_enabled` (+ dev/adb if
`alsoHideDev`) to `A11yCloakPrefs.Escrow`, strip our component from the services
list (Android unbinds us), optionally zero dev/USB. On leaving: restore verbatim
(Android rebinds us). `onDestroy` best-effort un-cloaks so a kill never strands the
service stripped. See README for the catch-22 this design solves.

### `data/`
- **`DevSettingsRepository`** — the Settings facade (pattern #4). Flows + setters for
  dev-options/USB/Wi-Fi-ADB/kiosk-flag (`Settings.Global`) and screen-timeout
  (`Settings.System`); `currentX()` one-shot reads for services; `registerWidgetRefresh`.
- **`TimeoutPrefs`** — escrow of the pre-MAX screen-off timeout.
- **`KioskPrefs`** / **`FocusCallsPrefs`** / **`HideDevPrefs`** / **`ForceDarkPrefs`**
  / **`A11yCloakPrefs`** / **`AdbConsolePrefs`** — per-feature DataStores (pattern #3),
  several carrying escrow state.
- **`AdbEndpointDiscoverer`** — see `adb/`.

### `haptics/`
`HapticsEngine` (VibratorManager/Vibrator abstraction, amplitude, effects,
waveforms, primitives) + `HapticsPresets` (the catalog the lab screen renders).

### `kiosk/`
- **`KioskAccessibilityService`** — the multiplexed service (pattern #1).
- **`KioskNotification`** / **`KioskControlReceiver`** — pinned kiosk notification +
  its Stop action.
- **`AnimationScales`** — zero/restore `window/transition/animator` scales (escrow in
  SharedPreferences) to reduce the relaunch flash.

### `tile/`
`BaseToggleTile` (shared `TileService` toggle skeleton) + five toggle tiles +
`KioskModeTile` (custom: picker-or-launch behaviour).

### `ui/`
One Composable screen per feature (named in the README matrix) + `theme/`
(Color / Type / Theme). Screens are stateless over their DataStore/Flow inputs and
call into the data/service layer; none hold business logic beyond view state.

### `usb/`
`UsbInspector` (enumerate `UsbManager` devices/interfaces/endpoints + read
`power_supply` sysfs) + `UsbModels` (data classes).

### `util/`
- **`AdbDetection`** — 16 permission-free ADB/tamper probes, each with a copy-paste
  Kotlin snippet. Drives the ADB Detection screen.
- **`A11yDetection`** — permission-free accessibility-service enumeration + grading +
  detection snippets. Drives the Accessibility Detection screen.
- **`ForegroundAppDetector`** — `UsageStatsManager` current-foreground-package +
  appop check. Shared by Focus Calls and the Cloak.
- **`InstalledApps`** — shared installed-user-apps loader for the pickers.
- **`PermissionUtils`** — runtime permission / appop checks for the status panel.
- **`WifiUtils`** — current Wi-Fi IPv4 for endpoint display.

### `widget/`
`DevControlsWidget` (Glance composable + synchronous snapshot) +
`DevControlsWidgetReceiver` + `ToggleActionCallback` (cell-tap dispatcher) +
`CopyEndpointAction` (endpoint copy) + `WidgetRefresher` (debounced DataStore+NSD →
`updateAll`). Refresh model is pattern #4 plus an immediate post-tap `updateAll`.

---

## Detection ⇄ Cloak: the two halves

The defensive screens (`util/AdbDetection`, `util/A11yDetection`) and the offensive
features (Hide Dev, Force Dark, Accessibility Cloak) are deliberate mirror images:

| Detector reads | Counter-measure |
| --- | --- |
| `development_settings_enabled` / `adb_enabled` | Hide Dev zeroes them per-app |
| `adb_wifi_enabled` | Hide Dev "also hide wireless ADB" |
| `enabled_accessibility_services` lists our service | Accessibility Cloak strips it per-app |
| service `isAccessibilityTool=false`, sideload installer, non-system, reads-all-windows | (config-level — see SECURITY_HARDENING.md) |

The detection probes take **no** permission precisely so the hardened production app
can adopt them verbatim — the snippet shown in each card *is* the production check.

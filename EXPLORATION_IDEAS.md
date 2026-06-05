# Android Surfaces Worth Exploring Next

The features already in Dev Controls cover a slice of what an unrooted Android
app can do. Below is the rest of the surface, grouped by effort + impact.
Anything tagged "ADB-grantable" can be unlocked through the pairing we already
have working in `adb_c`.

## Already touched (for reference)

`Settings.Global / System` r/w, Quick Settings tiles, Glance widgets,
NotificationCompat + RemoteInput, foreground service, AccessibilityService
(`TYPE_WINDOW_STATE_CHANGED` only), AppWidgetReceiver, ContentObserver,
DataStore, NSD / mDNS, TelecomManager + InCallService + RoleManager
(`DIALER`), PHONE_STATE broadcasts, UsageStatsManager, AppOpsManager (read),
embedded ADB protocol client (libadb-android), DeviceAdminReceiver scaffold.

---

## Tier 1 — high-impact, mostly drop-in (afternoon each)

### NotificationListenerService

Reads *every* notification on the device — content, actions, posting app,
dismiss timing. Can `cancelNotification(key)` to remove any notification
programmatically; can `snoozeNotification` (Android 8+).

- Permission: `BIND_NOTIFICATION_LISTENER_SERVICE` (system) — granted by the
  user in Settings, **or** via
  `adb shell cmd notification allow_listener <component>` (we already wire
  this in the `adb_c` recipes file's "grant our own perms").
- Use cases: log / archive all notifications, auto-mute specific apps, scrape
  OTP codes from SMS-app notifications, build a system-wide notification rule
  engine.

### CallScreeningService + `ROLE_CALL_SCREENING`

Sees every incoming call *before it rings*. You can silently reject, mark as
blocked, or let it through. Different from Focus Calls Arch B because the
call hasn't reached the dialer yet.

- Role grant:
  `adb shell cmd role add-role-holder android.app.role.CALL_SCREENING pro.jayeshseth.devcontrols`.
- Use cases: spam-number block-list, only-allow-contacts mode, log every
  incoming number.

### CallRedirectionService + `ROLE_CALL_REDIRECTION`

Modifies *outgoing* calls before they're placed. Rewrite the dialed number,
route via a different SIM, or cancel.

- Same role-grant pattern.
- Use cases: auto-prefix work calls with a feature code, force a specific SIM
  for international, time-of-day routing.

### Default SMS handler + `ROLE_SMS`

Become the SMS app. Read every SMS / MMS, send programmatically
(`SmsManager.sendTextMessage`), build any UI.

- Role grant + runtime perms `READ_SMS`, `RECEIVE_SMS`, `SEND_SMS`.
- Use cases: parse OTP codes into a clipboard chip, archive everything to a
  JSON file, scriptable SMS automations.

### VpnService

The single most powerful surface on Android. You get a `tun` interface and
intercept *all* device traffic — TCP, UDP, ICMP. Per-app filtering, packet
logging, DNS rewriting, blocking, redirection through SOCKS / HTTP proxies.

- Permission: `BIND_VPN_SERVICE` (system) + user consent dialog (one tap).
- No root needed.
- Use cases: ad blocker, DNS over HTTPS, per-app firewall, packet logger,
  capture for analysis. Apps like NetGuard / Blokada / Personal DNS Filter
  are all built on this.

### InputMethodService (custom keyboard)

A keyboard is one of the most privileged user-facing components on Android.

- No special permission beyond declaring the IME service.
- Use cases: command-palette keyboard that types your favourite shell
  commands, clipboard history with snippets, terminal-style autocomplete,
  regex-replacement, multi-line snippet expansion.

### AutofillService

System-wide autofill provider (the slot 1Password / Bitwarden plug into).

- Permission: `BIND_AUTOFILL_SERVICE` (system, user-granted via Settings or
  ADB).
- Use cases: own password vault, auto-fill OTPs scraped from SMS, fill
  arbitrary forms from a key-value store.

### Expand the existing AccessibilityService

Today we only listen to `TYPE_WINDOW_STATE_CHANGED` / `TYPE_WINDOWS_CHANGED`.
Flip the manifest config to add `canPerformGestures="true"` +
`canRetrieveWindowContent="true"` and you can:

- Read text of every visible UI element (`AccessibilityNodeInfo` tree).
- Auto-click, scroll, fill text fields.
- OCR-style scraping of arbitrary app screens.
- Click "Pin" on Recents (gives you the OS-pin Moto's launcher hides).

Same service, more flags. No new ADB grant needed once accessibility is on.

---

## Tier 2 — novel rabbit holes (a weekend each)

### WallpaperService (live wallpaper)

Replace the home screen wallpaper with an animated Compose / OpenGL surface
that pulls real-time data — CPU load, battery, current Wi-Fi SSID,
notification count, weather, your own metrics.

### DreamService (screensaver)

What shows when the device is docked or idle. Full Activity-style surface,
but only active when idle. Build a system dashboard / clock / network
monitor.

### MediaSession + MediaController

Read what's playing in every other media app (Spotify, YouTube, Pocket Casts)
via `MediaSessionManager.getActiveSessions(componentName)`. Control playback,
scrub, change tracks. Permission is `BIND_NOTIFICATION_LISTENER_SERVICE`
(same one) — once you have a NotificationListener, you also get
media-session enumeration.

### AppOpsManager.startWatchingMode

Real-time notifications when any app's appop state changes. You can watch
*every other app's* permission usage live — `OPSTR_FINE_LOCATION`,
`OPSTR_CAMERA`, `OPSTR_RECORD_AUDIO` — and see what apps just used your mic /
camera / location.

- Reflection needed for the per-package variant on some versions; basic API
  is public.
- Use cases: real-time privacy dashboard ("Spotify just accessed location at
  3:42 PM").

### MediaProjection (screen recording w/ consent)

One-time consent prompt → continuous screen capture in a background service.
`FLAG_SECURE` windows are blanked but everything else (including system UI)
is recorded.

### DocumentsProvider

Implement a virtual filesystem that shows up in the system file picker /
Files app. Backing store can be anything — encrypted blobs, a remote API,
your DataStore prefs surfaced as files.

### Companion Device Manager (CDM)

Pair with a "companion device" (real BLE, or virtual). After pairing the OS
grants generous background-service exemptions (no battery optimisation),
persistent location, etc. Originally for smartwatch apps but works for any
pairing.

### Picture-in-Picture

Float a Compose window over other apps. Could turn the toggle panel into a
floating overlay during games / videos.

### SYSTEM_ALERT_WINDOW overlays

Grant via `adb shell appops set ... SYSTEM_ALERT_WINDOW allow`. Draws over
everything (subject to system limits). Use cases: a floating ADB-terminal
bubble that's always reachable, a tiny system-stats HUD.

### TileService advanced features

- **Activity tile** — tile *launches* a small popover instead of toggling.
  Good for picker UX from the shade.
- **Boolean tile** with state animation.
- `onTileAdded` / `onTileRemoved` callbacks → first-run flow on tile add.
- `requestListeningState` → preemptively refresh.

### App Shortcuts (dynamic & pinned)

Long-press app icon → static + dynamic shortcuts. Pinned shortcuts on the
home screen (separate from widgets). Could surface "Open ADB Console",
"Start Kiosk", "Toggle Hide Dev" as one-tap launcher shortcuts.

---

## Tier 3 — deeper, more experimental

### Mock location

Grant via Dev Settings → "Select mock location app" + `ACCESS_MOCK_LOCATION`.
Feed fake GPS to apps that respect mock locations. Doesn't fool Google Maps
for navigation anymore, but fools many other apps.

### NFC HCE (Host Card Emulation)

Emulate an NFC card. Tap your phone on a reader and present an arbitrary
ISO 7816 / Mifare-style response. Used for transit cards, building access.

### USB host / accessory mode

With a USB-OTG adapter, your phone can host other USB devices (keyboards,
gamepads, serial dongles, MIDI). API: `UsbManager` + `UsbDeviceConnection` +
bulk transfers. Phone-as-Arduino-flasher, U2F key reader.

### TextToSpeech + SpeechRecognizer

Voice-controlled toggles. Talk to your phone, our app parses commands.

### Health Connect

Read health data (steps, heart rate, sleep) from any compatible health app.

### Predictive haptics / advanced Vibration API

(Covered in the dedicated `Haptics Lab` screen.)

### Battery + thermal monitoring

`BatteryManager` + `PowerManager.OnThermalStatusChangedListener` → real-time
thermal throttling state, charging current / voltage, battery temperature.

### Reflection into `SystemProperties` / hidden APIs

`@hide` methods on system services hold a lot of power.
`SystemProperties.set("sys.x", "y")` for writable props (limited list for
shell user but real). On Android 9+ there's a hidden-api block-list; bypass
with `org.lsposed.hiddenapibypass`.

### Hide our own app from launchers

`PackageManager.setComponentEnabledSetting(MainActivity component,
COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)` removes the app icon from
the launcher while keeping the app installed. Still launchable via shortcuts,
intent, or a deep link.

### Device admin policies (without device-owner)

Even without device-owner, `DeviceAdminReceiver` enabled by the user grants:
`lockNow()`, force password change, disable camera, set password quality,
wipe data. User confirms an activation dialog once.

---

## Pure roles we haven't taken

`adb shell cmd role add-role-holder` works for all of these:

| Role | What it gives |
| --- | --- |
| `ROLE_DIALER` | (already touched) |
| `ROLE_SMS` | default SMS handler |
| `ROLE_BROWSER` | default browser intent target |
| `ROLE_CALL_SCREENING` | pre-ring call inspector |
| `ROLE_CALL_REDIRECTION` | outgoing-call rewriter |
| `ROLE_HOME` | default launcher (replaces stock launcher) |
| `ROLE_ASSISTANT` | Google Assistant slot — long-press home, "OK Google" |
| `ROLE_NOTES` | quick-note from lock screen (Android 14+) |
| `ROLE_SYSTEM_GALLERY` | gallery app surface |

`ROLE_HOME` is wild — taking it makes Dev Controls *the* launcher. We could
draw a fully custom home screen.

`ROLE_ASSISTANT` is also wild — long-press home anywhere on the device
invokes us. Could turn into a global command palette.

---

## Off-limits without root / device-owner

For completeness:

- `/system`, `/vendor`, `/product`, `/boot` modification.
- Kernel module loading; SELinux policy changes.
- `setprop` on `ro.*` and `ro.build.*` props.
- Real OS lock-task on third-party apps (we use the accessibility relaunch
  trick).
- Modifying other apps' private `/data/data/<pkg>` (SELinux per-app sandbox).
- Bootloader / fastboot / recovery operations.
- Carrier locks, IMEI changes, baseband.

---

## Top three picks to build next

Given the existing stack and the tinkering style:

1. **NotificationListenerService** — already 80 % set up (recipes file
   self-grants the listener appop). Massive new dataset (every notification on
   the device) for very little code.
2. **VpnService** — most powerful surface that doesn't need root. Per-app
   firewall + DNS-level ad blocker + packet logging. One-tap user consent,
   no shell perms needed.
3. **Custom InputMethodService (keyboard)** — every keystroke through code
   you wrote. Command palette, quick-paste snippets, regex search-replace
   mid-typing.

---

## v7 round — newly identified surfaces

Sorted into the working themes. Tags: ✅ built, 🔜 queued, 🧱 needs ADB/role grant.

### A. Window & visual tricks (continuation of the System Bar Sheet thread)

- **Background blur behind a window** ✅ — `Window.setBackgroundBlurRadius()` +
  `FLAG_BLUR_BEHIND` / `LayoutParams.blurBehindRadius` (API 31+). Frosted-glass
  scrim; the natural next step on the bottom sheet. Gated by
  `WindowManager.isCrossWindowBlurEnabled` (device + dev-setting dependent).
  → *Window FX Lab → Blur tab.*
- **AGSL runtime shaders** ✅ — `RuntimeShader` (AGSL) + `ShaderBrush` /
  `Modifier.graphicsLayer { renderEffect }` (API 33+). GPU fragment shaders in
  Compose: gradients, distortion, frosted glass. → *Window FX Lab → Shader tab.*
- **Raw `WindowManager` windows** 🔜 — add a `ComposeView` straight to the
  `WindowManager` with custom `LayoutParams` (exactly how `ModalBottomSheet`
  sidesteps the floating-`Dialog` black-bar problem). Non-floating, true
  edge-to-edge panels.
- **Cross-Activity shared-element transition** 🔜 —
  `ActivityOptions.makeSceneTransitionAnimation` + `transitionName`. The
  legitimate in-app cousin of the launcher icon-morph.

### B. Moneyview hardening (extends ADB / A11y Detection)

- **Screenshot detection** 🔜 — `Activity.registerScreenCaptureCallback()`
  (API 34) fires on screenshot; pair with `FLAG_SECURE` to block.
- **Overlay / tapjacking detection** 🔜 — `View.setFilterTouchesWhenObscured(true)`
  + `MotionEvent.FLAG_WINDOW_IS_OBSCURED`. Detect a malicious overlay over a
  login screen (the SYSTEM_ALERT_WINDOW attack).
- **KeyStore hardware-attestation inspector** 🔜 — generate a key with
  `setAttestationChallenge()`, pull the X.509 chain, verify TEE/StrongBox
  backing + verified-boot state. The real test of whether "secure keys" are
  hardware-protected — far stronger than the Info Dump file scan.
- **Deeper root/integrity probes** 🔜 — bootloader lock
  (`ro.boot.verifiedbootstate`, `ro.boot.flash.locked`), SELinux enforcing,
  Magisk/Zygisk/Shamiko traces, Frida-gadget scan via `/proc/self/maps`,
  runtime APK-signature self-check.
- **Play Integrity API** 🔜 — see the verdict Google returns for this
  (bypassed) device.

### C. Inspectors (parallel to USB Inspector / Haptics Lab)

- **BLE scanner + advertiser** 🔜 — enumerate nearby BLE advertisements;
  broadcast as a beacon (`BluetoothLeAdvertiser`). Needs `BLUETOOTH_SCAN`.
- **Radio / Cell inspector** 🔜 — `TelephonyManager.getAllCellInfo()`, live
  Wi-Fi scan list, `ConnectivityManager` diagnostics. Needs location.
- **Sensor Lab** 🔜 — every sensor live (accel/gyro/mag + uncalibrated, step,
  significant-motion, hinge, light, proximity). Same shape as Haptics Lab.

### D. System / process introspection

- **Refresh-rate monitor + display modes** ✅ — measured Hz via `withFrameNanos`,
  reported `Display.refreshRate`, `supportedModes`, request a mode via
  `LayoutParams.preferredDisplayModeId`. Scrollable sample list to drive the
  rate up. → *Display Lab.*
- **Process death inspector** ✅ — `ActivityManager.getHistoricalProcessExitReasons()`
  (API 30): why the app was last killed (ANR/crash/LMK/user/signal). → *Display Lab.*
- **App standby buckets** ✅ — `UsageStatsManager.getAppStandbyBucket()`
  (active/working/frequent/rare/restricted). → *Display Lab.*
- **Thermal + charging telemetry** ✅ — `PowerManager.addThermalStatusListener`
  + `BatteryManager` instantaneous current/voltage/temp. → *Display Lab.*

### E. New high-power components

- **Bubbles** 🔜 — the official floating chat-head API
  (`Notification.BubbleMetadata` + conversation shortcut), not the overlay hack.
- **VirtualDisplay + MediaProjection** 🔜 — render to a virtual display and
  capture/cast it.
- **Torch strength control** 🔜 — `turnOnTorchWithStrengthLevel()` (API 33):
  variable-brightness flashlight + strobe.

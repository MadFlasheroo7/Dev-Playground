# Securing a Production App Against Dev Controls-style Bypasses

A few thoughts before the recommendations — there's no perfect bulletproof
defense on Android, especially against an attacker with ADB on their own
device. The goal is **raising the bar** so the bypass takes more effort than
the attacker's payoff justifies. Concrete suggestions, organised by
effort/impact.

---

## Why Dev Controls' bypasses worked

Most apps check developer mode **once** at startup, then trust the result.
They also rely on **a single signal** (usually `DEVELOPMENT_SETTINGS_ENABLED`).
Dev Controls' Hide-Dev feature flips that signal to `0` only *while your app
is foregrounded* — by the time your app's `Application.onCreate` reads it, the
value is already `0`. After that, nothing checks again.

Dev Controls also abuses two things your app can't easily prevent but
absolutely can *detect*:

- An accessibility service is running (`KioskAccessibilityService`).
- Permissions were granted via `pm grant` after install (e.g.
  `WRITE_SECURE_SETTINGS` to a user app).
- The default dialer role was reassigned to a non-system app (Focus Calls
  Arch B).

The fix is to widen the signal surface and **re-check continuously**.

---

## Tier 1 — cheap, immediate wins (a few hours of work)

### 1. Don't trust a single signal; sample many

Read all of these at startup *and* every ~10 s via a polling job or
`ContentObserver`:

| Signal | Source | What it tells you |
| --- | --- | --- |
| `DEVELOPMENT_SETTINGS_ENABLED` | `Settings.Global` | Primary dev-mode flag |
| `ADB_ENABLED` | `Settings.Global` | USB debug ever on |
| `ADB_WIFI_ENABLED` | `Settings.Global` | Wireless debug (API 30+) |
| `Build.TYPE` | static | `userdebug` or `eng` → OS is non-prod |
| `Build.TAGS` | static | `test-keys` → custom ROM |
| `ApplicationInfo.FLAG_DEBUGGABLE` (self) | `applicationInfo.flags` | Your APK is debuggable |
| `Debug.isDebuggerConnected()` | runtime | JDWP attached |
| `/proc/self/status` → `TracerPid:` line | file read | non-zero → being ptraced (frida etc.) |
| `getprop init.svc.adbd` | shell call or reflection on `SystemProperties` | adbd daemon running |

If *any* of those flip from "clean" to "dirty" mid-session, treat that as
Hide-Dev temporal toggling and refuse sensitive operations.

### 2. Register a `ContentObserver` on the dev flags

This catches the Hide-Dev pattern explicitly. The temporal-toggle bypass
relies on you not noticing the flip:

```kotlin
contentResolver.registerContentObserver(
    Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
    false, observer
)
```

Inside `onChange`, sample the value, log a "tamper event," show a soft
warning, or lock the app to a safe state.

### 3. Enumerate enabled accessibility services

```kotlin
val am = getSystemService(AccessibilityManager::class.java)
val enabled = am.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK)
```

Build a small allow-list of services you accept (TalkBack, Switch Access, your
OEM's). Anything else → flag. Dev Controls' Kiosk service would show up as
`pro.jayeshseth.devcontrols/.kiosk.KioskAccessibilityService` — that's a dead
giveaway it's been used to bypass UI-flow checks.

### 4. Enumerate apps holding sensitive permissions

```kotlin
packageManager.getPackagesHoldingPermissions(
    arrayOf(Manifest.permission.WRITE_SECURE_SETTINGS),
    0
).map { it.packageName }
```

Anything not in `{Settings, SystemUI, your_oem_settings, …}` holding
`WRITE_SECURE_SETTINGS` was almost certainly granted via ADB or root. Same for
`BIND_ACCESSIBILITY_SERVICE` enumeration, `PACKAGE_USAGE_STATS`,
`WRITE_SETTINGS` appop.

### 5. Verify the dialer role for any call-sensitive flow

```kotlin
val rm = getSystemService(RoleManager::class.java)
val dialerHolders = rm.getRoleHolders(RoleManager.ROLE_DIALER)
```

Allow-list known dialer packages (Google Dialer + OEM stock). If a user app
holds it, Focus Calls Arch B is in play — show a warning, decline payment
flows, etc.

### 6. `FLAG_SECURE` everywhere it matters

```kotlin
window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, FLAG_SECURE)
```

Blocks `screencap`, `screenrecord`, MediaProjection capture, and most
accessibility-driven screenshot capture. Apply per-Activity to login, OTP,
payment, key-display screens. This is one line and very high value.

### 7. Strip release builds

- `android:allowBackup="false"`, `android:fullBackupContent=...`
- ProGuard/R8 on with aggressive obfuscation
- Strip all `Log.d`/`Log.v`/`Log.i` calls in release (R8 `-assumenosideeffects`)
- `android:debuggable="false"` (default, but assert)
- `android:extractNativeLibs="false"` if you have .so files (harder to patch)

---

## Tier 2 — medium effort, big impact

### 8. Play Integrity API — the only real attestation

This is the canonical answer. Google's signed verdict over: device integrity
(basic/strong/hardware-backed), app integrity (signature match, installed
from Play), and account licensing. Returns a JWT you verify on your backend.

- Hardware-backed verdict (TEE-attested) is *very* hard to fake — defeats
  most Magisk + Zygisk attestation hiders eventually as Google rolls out new
  signals.
- Limitation: a determined attacker on the latest Magisk + Shamiko/Zygisk-
  Assistant can still produce `BASIC` integrity on some devices, but not
  `STRONG` / `HARDWARE_BACKED`.
- For your prod app: require `STRONG` integrity for any high-value operation.
  Treat `BASIC`-only as a soft warning.

### 9. Native (JNI) integrity checks

Move the *result* of your hardening checks behind native code.
Reverse-engineering smali is 10× easier than reverse-engineering native
arm64. In your `.so`:

- Read `/proc/self/maps`, look for known frida/lsposed strings (`frida`,
  `gum-js-loop`, `linjector`, `re.frida.server`).
- Self-sign verify: compute SHA-256 of the app's signing certificate at
  runtime, compare against a hardcoded constant in native.
- Don't return a bool — return a pseudo-random value that you check; makes
  "patch the return to true" useless.

### 10. Re-verify your own signing certificate

```kotlin
val sigs = packageManager.getPackageInfo(
    packageName,
    PackageManager.GET_SIGNING_CERTIFICATES
).signingInfo.apkContentsSigners
val cert = MessageDigest.getInstance("SHA-256").digest(sigs[0].toByteArray())
```

Compare against your known-good hash hardcoded in native code. Repackaged
APKs (the most common attack) fail this check.

### 11. Hide hardening logic

Don't centralise all checks in `class HardeningChecker`. Inline them in
unrelated paths. Sample one signal in the login flow, another in the
order-placement flow, another in the receipt screen. Make the attacker patch
*every* check, not one.

### 12. Server-side correlation

Whatever you check client-side, also send the result to your backend along
with the actual transaction. If the client says "integrity OK" but the device
ID has previously failed integrity, or the install timestamp is 30 seconds
ago, or 50 devices share the same Play Integrity verdict signature → block
server-side.

---

## Tier 3 — high effort, used by banking apps

### 13. Commercial RASP SDKs

- **Promon SHIELD**, **Guardsquare DexGuard**, **Appdome**, **AppSealing** —
  heavy obfuscation, runtime instrumentation, native anti-tampering,
  anti-frida, anti-magisk.
- Plug-and-play but pricey. Banking apps usually use one of these.

### 14. Time-of-check vs time-of-use mitigations

A common bypass pattern: hook `Settings.Global.getInt` to return 0 only when
called from your app's classloader. Defense: don't go through the public API
— use reflection to access `SettingsProvider` directly, or use multiple call
paths (binder query, ContentResolver, file read), and compare. Disagreement
= hook.

### 15. Anti-frida specifically

- Look for the `frida-server` process via `ps`/proc walk.
- Look for the gadget library load (`frida-gadget`, `libfrida-gadget.so`) in
  your own process memory.
- TLS pinning + cert hash verification (frida hooks `OkHttpClient.Builder`
  etc.).
- Look for `D-Bus` socket in `/data/local/tmp` (frida's default IPC).

### 16. Anti-Magisk denylist

- Compare `mount` output with `mountinfo` — Magisk hides mounts from `mount`
  but not always from `/proc/self/mountinfo`.
- Read `/proc/self/maps` for unexpected `.dex` overlays.
- Check `/data/adb/magisk` accessibility (it's hidden by Magisk Hide, but
  enumeration tricks can leak it).

---

## Concrete recommendation for your specific situation

Since you tested with Dev Controls and confirmed bypass, the **highest ROI
in your prod app's next release**:

1. **Play Integrity API** (Tier 2) — wraps most of the other concerns.
   Integrate the SDK, gate sensitive flows on `STRONG` verdict.
2. **`FLAG_SECURE`** on every screen where you display secret state.
3. **Multi-signal dev-mode check + `ContentObserver`** so Hide-Dev's
   temporal trick is caught immediately.
4. **Accessibility enumeration** with an allow-list — refuse to launch if a
   non-allowlisted a11y service is enabled while you're on a sensitive
   screen.
5. **Self-signature verification** (native preferred) to defeat repackaging.
6. **`getPackagesHoldingPermissions`** check at startup for
   `WRITE_SECURE_SETTINGS` — flag any non-system holder.

Tier 1 items 1–7 are an afternoon's work and would have blocked everything
Dev Controls did in your test session. Tier 2 #8 (Play Integrity) takes a
sprint but is what banks use because it's hardware-backed.

If you want, a `prod_hardening_demo` module can be added — a drop-in Kotlin
file with the Tier-1 checks fully implemented (multi-signal dev detection,
accessibility allow-list, signing-cert hash, dialer role check,
permission-holder enumeration) — that can be pasted into the prod app and
gated on. Or a sample Play Integrity wrapper.

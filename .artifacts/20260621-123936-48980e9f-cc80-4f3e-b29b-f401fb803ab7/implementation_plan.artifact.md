# Implementation Plan - System-Wide Privacy Glow HUD

This feature provides a real-time, non-intrusive visual indicator of privacy-sensitive permission usage (Camera, Microphone, Location) across the entire Android OS. It uses `AppOpsManager.startWatchingMode` to detect usage and a `SYSTEM_ALERT_WINDOW` with an AGSL shader to draw a colored glow around the screen edges.

## User Review Required

> [!IMPORTANT]
> This feature requires two high-level permissions:
> 1. **SYSTEM_ALERT_WINDOW**: To draw the glow over other apps.
> 2. **PACKAGE_USAGE_STATS** (or similar): To potentially identify *which* app is using the permission, though `AppOpsManager` often provides the package name directly in the callback.
>
> The glow will be subtle but persistent while a sensitive op is active.

## Proposed Changes

### Core Logic & Service

#### [NEW] [PrivacyGlowService.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/service/PrivacyGlowService.kt)
- A foreground `Service` that manages the overlay window.
- Uses `AppOpsManager.startWatchingMode` for:
    - `OPSTR_CAMERA`
    - `OPSTR_RECORD_AUDIO`
    - `OPSTR_FINE_LOCATION` / `OPSTR_COARSE_LOCATION`
- Maintains a state of "Active Ops" and their associated colors:
    - Camera -> Red
    - Microphone -> Orange/Yellow
    - Location -> Blue
- Updates the overlay's shader parameters based on active ops.

### UI & Shaders

#### [NEW] [PrivacyGlowOverlay.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/PrivacyGlowOverlay.kt)
- A Composable that renders the AGSL glow.
- **AGSL Shader**: A fragment shader that draws a soft gradient glow along the screen bounds.
- Supports multi-color blending if multiple permissions are active simultaneously.

#### [NEW] [PrivacyGlowScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/PrivacyGlowScreen.kt)
- A control screen in Dev Controls to:
    - Toggle the service on/off.
    - Request necessary permissions (Draw over apps).
    - Customize glow intensity, width, and colors.
    - Show a history of recent privacy "events".

---

### Navigation & Integration

#### [MODIFY] [Route.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Route.kt)
- Add `PRIVACY_GLOW` to the `Route` enum.

#### [MODIFY] [MainActivity.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/MainActivity.kt)
- Wire the `PRIVACY_GLOW` route to `PrivacyGlowScreen`.

---

## Verification Plan

### Automated Tests
- Build verification: `./gradlew assembleDebug`

### Manual Verification
1. Launch Dev Controls and navigate to **Privacy Glow**.
2. Grant "Draw over apps" permission when prompted.
3. Toggle the service **ON**.
4. Open the Camera app -> Verify a **Red** glow appears at the screen edges.
5. Open a Voice Recorder app -> Verify an **Orange** glow appears.
6. Open Google Maps -> Verify a **Blue** glow appears.
7. Close the apps and verify the glow fades out.
8. Verify that if both Camera and Mic are used (e.g., Video recording), the glow blends colors.

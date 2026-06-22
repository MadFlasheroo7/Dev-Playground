# Walkthrough - System-Wide Privacy Glow HUD

The Privacy Glow HUD is a real-time, non-intrusive visual indicator of privacy-sensitive permission usage (Camera, Microphone, and Location) across the entire Android OS.

## Features

- **Real-Time Monitoring**: Uses `AppOpsManager.startWatchingActive` to detect exactly when an app starts or stops using a sensitive permission.
- **System Overlay**: Draws a subtle, colored glow around the edges of the screen using a `SYSTEM_ALERT_WINDOW`.
- **AGSL Shaders**: Leverages Android 13's Runtime Shaders to create a smooth, blended gradient glow.
- **Color Coding**:
    - **Red**: Camera in use.
    - **Orange**: Microphone in use.
    - **Blue**: Location in use.
- **Auto-Blending**: If multiple permissions are active, the colors blend together dynamically.

## Key Components

### [PrivacyGlowService.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/service/PrivacyGlowService.kt)
A foreground service that manages the lifecycle of the overlay window and registers the `AppOpsManager` listeners. It maintains the state of active operations and passes them to the Compose UI.

### [PrivacyGlowOverlay.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/PrivacyGlowOverlay.kt)
The UI layer for the glow itself. It uses a `Canvas` with a `ShaderBrush` powered by an AGSL shader. The shader calculates the distance to the screen edges to create the "glow" effect and handles color blending.

### [PrivacyGlowScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/PrivacyGlowScreen.kt)
The user-facing control screen in Dev Controls. It allows users to:
1. Grant the "Draw over other apps" permission.
2. Start and stop the `PrivacyGlowService`.
3. View the color legend.

## Verification Summary

### Automated Tests
- Build verified successfully with `./gradlew :androidApp:assembleDebug`.

### Manual Verification Steps
1. Navigate to **Security & Privacy** -> **Privacy Glow HUD**.
2. Grant "Draw over other apps" permission.
3. Toggle the service **ON**.
4. Open the **Camera** app -> Verify the **Red** edge glow.
5. Open a **Voice Recorder** -> Verify the **Orange** edge glow.
6. Open **Google Maps** -> Verify the **Blue** edge glow.
7. Close all apps -> Verify the glow fades out.

# Implementation Plan — Connected Device Audio Sync (`Route.AUDIO_SYNC`)

This plan details the implementation of a real-time, multi-device synchronized audio playback feature. One device functions as the Host (broadcasting commands and managing player states/volumes), and other devices on the same Wi-Fi function as Clients (discovering the Host, synchronizing their clocks, and matching playback states).

---

## 1. Synchronization & Networking Protocol

### 1.1 Discovery (UDP Broadcast)
- **Host**: Broadcasts a UDP packet to port `8098` every 2 seconds.
  - Format: `DEVAUDIOSYNC_BEACON|<host_ip>|<host_device_name>`
- **Client**: Opens a UDP socket on port `8098` and listens for beacons. Discovered hosts are shown in the UI for one-tap connection.

### 1.2 Communication Bridge (TCP)
- **Port**: `8099` (TCP)
- **Client Registration**: Upon connection, client sends:
  - `REGISTER|<device_name>|<client_ip>`
- **Clock Sync (NTP-style)**:
  - Client sends: `CSYNC_PING|<client_timestamp>`
  - Host replies: `CSYNC_PONG|<client_timestamp>|<host_timestamp>`
  - Client calculates round-trip time ($RTT$) and offset:
    - $RTT = \text{receive\_time} - \text{client\_timestamp}$
    - $\text{Estimated Host Time} = \text{host\_timestamp} + \frac{RTT}{2}$
    - $\text{Offset} = \text{Estimated Host Time} - \text{receive\_time}$
  - Client clock is adjusted: $\text{Adjusted Time} = \text{local\_time} + \text{offset}$
- **Control Commands**:
  - `PLAY|<stream_url>|<title>|<uploader>|<position_ms>|<target_host_time>`
  - `PAUSE`
  - `RESUME|<position_ms>|<target_host_time>`
  - `SEEK|<position_ms>`
  - `SET_VOLUME|<volume_float_0_to_1>`
- **Client Status Updates**:
  - Client periodically sends back: `STATUS|<volume>|<is_buffering>|<current_position_ms>`
  - Host UI shows the real-time status of each client (buffering, playing position, current volume).

---

## 2. File Modifying List

1. **Modify** `composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Route.kt`: Add `AUDIO_SYNC` to the route enum.
2. **Modify** `composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Categories.kt`: Add "Connected Audio Sync" under "Audio Lab".
3. **Modify** `composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/ScreenRegistry.kt`: Wire `Route.AUDIO_SYNC` to render `AudioSyncScreen`.
4. **Modify** `media/src/commonMain/kotlin/pro/jayeshseth/media/MediaController.kt`: Add `fun setVolume(volume: Float) {}`.
5. **Modify** `media/src/androidMain/kotlin/pro/jayeshseth/media/AndroidMediaController.kt`: Implement `setVolume(volume)` for ExoPlayer.
6. **Modify** `media/src/desktopMain/kotlin/pro/jayeshseth/media/DesktopMediaController.kt` / `MpvMediaController.kt`: Implement `setVolume(volume)`.
7. **Create** `composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/media/AudioSyncModels.kt`: Synchronized playback states and client metadata structures.
8. **Create** `composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/media/AudioSyncPlatform.kt`: Common expectations interface.
9. **Create** `composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/media/AudioSyncPlatform.android.kt`: Main JVM-sockets-based client and server network sync implementation.
10. **Create** `composeApp/src/desktopMain/kotlin/pro/jayeshseth/devcontrols/media/AudioSyncPlatform.desktop.kt`: Desktop wrapper reusing the JVM implementation or stubs.
11. **Create** `composeApp/src/iosMain/kotlin/pro/jayeshseth/devcontrols/media/AudioSyncPlatform.ios.kt` and `composeApp/src/webMain/kotlin/pro/jayeshseth/devcontrols/media/AudioSyncPlatform.web.kt`: Green stubs for non-JVM platforms.
12. **Create** `composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/audiolab/AudioSyncScreen.kt`: The comprehensive visual control/trace dashboard UI.

---

## 3. Verification Plan
- **Desktop KMP Build**: `./gradlew :composeApp:compileKotlinDesktop`
- **Android KMP Build**: `./gradlew :androidApp:assembleDebug`
- **Functional Validation**: Deploy to multiple emulator/physical targets and verify discovery, sync delay, and volume.

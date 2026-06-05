# Camera category — deep plan

A dedicated **Camera** `LabCategory`. Consolidates the existing camera screens
(Camera Filter, Barcode Scanner) and adds a pro-grade toolkit built on CameraX +
Camera2Interop. minSdk 30 → MediaStore inserts need no storage permission.

## Shared infrastructure (build first)
- `ui/camera/CameraScaffold.kt` — top bar + back + `PermGate(CAMERA)` + a `PreviewView`
  (COMPATIBLE mode, consistent with the Camera Filter fix) and a slot for controls/overlays.
- `camera/CameraEngine.kt` — wraps `ProcessCameraProvider`, lens selection, and binding of
  Preview + (ImageCapture | VideoCapture | ImageAnalysis). Exposes `Camera`/`CameraControl`
  /`CameraInfo` + a `Camera2CameraControl`/`Camera2CameraInfo` handle for manual controls.
- `camera/MediaSink.kt` — save JPEG/DNG/MP4 to `MediaStore` (Pictures/Movies/DevControls),
  return content URIs; list them for the in-app gallery.
- Gradle additions: `androidx.camera:camera-video`, `androidx.camera:camera-extensions`
  (same 1.4.1 train). Optional: `com.google.android.gms:play-services-mlkit-document-scanner`.

## Screens / slices

### C1. Lens Explorer  ★★  (do first — cheap, validates engine)
Enumerate every camera via `CameraManager`/`Camera2CameraInfo`: facing, hardware level,
focal lengths (→ ultrawide/wide/tele/macro), sensor size + computed FOV, supported FPS
ranges, output formats (JPEG/RAW/YUV/PRIVATE), AE/AF modes, flash, stabilization, zoom
range. Live lens switch. A real CameraCharacteristics inspector.

### C2. Pro Mode (manual controls)  ★★★★  (flagship)
Camera2Interop `CaptureRequestOptions` over CameraX: manual ISO
(`SENSOR_SENSITIVITY`), shutter (`SENSOR_EXPOSURE_TIME`), focus distance
(`LENS_FOCUS_DISTANCE` + AF off), white balance, EV compensation, zoom ratio. Auto/Manual
toggles per parameter, ranges read from characteristics. Live **RGB histogram** overlay
(ImageAnalysis). Capture JPEG → MediaStore with EXIF.

### C3. Exposure Tools overlay  ★★★
Analysis-driven overlays on the live preview: RGB/luma **histogram**, **zebra** stripes
on overexposed pixels (>threshold), **focus peaking** (Sobel edges highlighted), framing
grid + spirit level (accelerometer). Pure overlays, no capture — pairs with C2.

### C4. Video Recorder  ★★★
CameraX `VideoCapture`/`Recorder`: quality picker (SD/HD/FHD/UHD via `QualitySelector`),
record/pause/resume, elapsed timer, mute, torch, MP4 → MediaStore. Audio needs RECORD_AUDIO
(already granted in app).

### C5. RAW / DNG Capture  ★★★★  (device-dependent)
Camera2 `RAW_SENSOR` capture + `DngCreator` → save `.dng` (plus a JPEG). Gate on the
camera reporting the RAW capability; otherwise show "not supported" from C1's data.

### C6. CameraX Extensions  ★★★
`ExtensionsManager`: toggle HDR / Night / Bokeh / Face-Retouch / Auto when the device+lens
support them; query availability per mode and show which are real on this device.

### C7. High-speed / Slow-mo  ★★★★  (device-dependent)
Constrained high-speed session for 120/240 fps capture where supported (from C1 FPS
ranges); record a slow-motion clip. Fallback message when unsupported.

### C8. Intervalometer / Timelapse  ★★★
Interval capture (every N s, M shots) → frames to MediaStore; optional stitch to MP4 via
`MediaCodec` + `MediaMuxer`. Progress + preview of last frame.

### C9. Motion Detection cam  ★★★
ImageAnalysis frame-differencing; on motion above a sensitivity threshold, snapshot or
start recording; show a motion meter + event log. A no-root security-cam.

### C10. Document Scanner  ★★★
ML Kit Document Scanner (`GmsDocumentScanning`) for edge detect + perspective correct +
multi-page PDF/JPEG export; or a custom Sobel-contour + perspective-warp fallback.

### C11. Gallery  ★★
Grid of everything captured by the app (query the DevControls MediaStore collection);
tap → full view / share / delete. Supports C2/C4/C5/C8.

## Recommended build order
1. **Infra + C1 Lens Explorer** (proves the engine; pure info, low risk).
2. **C2 Pro Mode** + **C3 Exposure Tools** (the flagship pair).
3. **C4 Video Recorder** + **C11 Gallery** (capture + review loop).
4. **C6 Extensions**, then device-dependent **C5 RAW**, **C7 high-speed**.
5. **C8 Timelapse**, **C9 Motion cam**, **C10 Doc scanner**.
Optionally migrate the existing **Camera Filter** + **Barcode Scanner** screens into the
new category.

## Perms / notes
- CAMERA (have), RECORD_AUDIO (have) for video. MediaStore writes: no extra perm on
  minSdk 30 (scoped storage). RAW/high-speed are device-gated — read support from C1 and
  degrade gracefully. Manual controls require `LEVEL_3`/`FULL` hardware for the full range;
  `LIMITED`/`LEGACY` devices expose a subset — surface the level from C1.
- Reuse: `PermGate`, the COMPATIBLE PreviewView lesson, ImageAnalysis pattern from the ML
  screens, Sobel from the Edge Detect shader for focus peaking.

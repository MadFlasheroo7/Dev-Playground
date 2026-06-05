# E13 — On-device ML lab  ★★★★

## Goal
Offline ML on camera/audio + an on-device LLM chat. New "ML" category (or under
Device & Media). All inference local, no network.

## Dependencies (add to libs.versions.toml + app build)
- `com.google.mediapipe:tasks-vision` (hand/pose/image)
- `com.google.mediapipe:tasks-genai` (LLM Inference) — or TFLite as fallback
- CameraX already present. Consider ABI filters (arm64-v8a) to keep APK sane.

## Sub-features (each a screen)
1. **Hand landmarks** — CameraX `ImageAnalysis` → `HandLandmarker` (LIVE_STREAM) →
   overlay 21-point skeleton on a Compose `Canvas` aligned to the `PreviewView`. Model
   `hand_landmarker.task` bundled in assets (~7 MB).
2. **Pose landmarks** — same pipeline with `PoseLandmarker`; draw the body skeleton.
3. **Image classification / object detection** — `ImageClassifier`/`ObjectDetector` on
   frames; label + bbox overlay; show top-k + latency.
4. **On-device LLM chat** — `LlmInference` (MediaPipe GenAI) with a downloaded Gemma model
   (e.g. `gemma2-2b-it` int4 ~1.3 GB) cached in `filesDir`; streaming token output; chat
   UI (reuse a simple list + input). On supported devices, optionally route to **AICore /
   Gemini Nano**; fall back to the bundled Gemma path otherwise (robust default).

## Model delivery
- Small vision `.task` models → `assets/` (bundled).
- LLM (hundreds of MB–GB) → **download manager**: first-run download with progress to
  `filesDir`, checksum, resume; never bundle in APK. Gate the chat screen behind "model
  ready".

## Build order (vertical slices)
1. Add MediaPipe vision dep + bundle hand model; CameraX→HandLandmarker→Canvas overlay
   (the end-to-end slice that proves the pipeline + perm flow).
2. Pose + image classification screens (reuse the camera/overlay scaffold).
3. LLM: add tasks-genai, build the model download manager, then a streaming chat screen.
4. Polish: model switcher, FPS/latency readout, front/back camera toggle.

## Risks / notes
- Model size & storage: download-on-demand for the LLM; show size + progress; allow delete.
- Device support: Gemini Nano/AICore needs specific devices + may need allowlisting →
  default to MediaPipe Gemma (works broadly); detect AICore and prefer it when present.
- CameraX + ML threading: run inference off the main thread; throttle to analyzer cadence;
  COMPATIBLE PreviewView (consistent with Camera Filter fix) if overlaying RenderEffect.
- Reuses: CameraX setup from `CameraFilterLabScreen`, `PermGate(CAMERA)`, AppPicker N/A.
- APK size: arm64-only split or app bundle; keep vision models lean.

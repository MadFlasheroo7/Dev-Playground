# E2 — On-device app profiler  ★★★★

## Goal
A mini Android-Studio-Profiler for any package (esp. Moneyview): live CPU / memory / jank
/ battery charts + PID-filtered logcat, all via the self-ADB (UID 2000) connection.

## Prerequisite — streaming primitive
`AdbClient.openShellFlow(cmd: String): Flow<String>`:
- Opens `mgr.openStream("shell:$cmd")` on `Dispatchers.IO`, reads the InputStream by lines,
  `emit`s each line; closes the stream on cancellation.
- Runs on its **own** stream (NOT under the single-shot `exec` mutex) so a long-lived
  `logcat` doesn't block one-shot commands. `flowOn(IO)` + `buffer/conflate` for backpressure.
- This is the deferred piece from the System & ADB plan; build it first and smoke-test with
  `logcat -d` and `top -b -n 2`.

## Components
- `profiler/ProfilerRepository.kt`
  - `resolvePid(pkg)` via `pidof <pkg>` (fallback `ps -A | grep`).
  - **Metric sampler** (loop every ~1s through `exec`): `dumpsys meminfo <pkg>` → TOTAL PSS;
    `top -b -n1 -p <pid>` → CPU%; `dumpsys gfxinfo <pkg>` → jank% + 90/95/99th frame times;
    `dumpsys batterystats --charged <pkg>` deltas → drain estimate; thread count from `/proc`.
  - **Logcat streamer**: `openShellFlow("logcat -v threadtime --pid=$pid")`; crash buffer
    `openShellFlow("logcat -b crash -v threadtime")`. Ring buffer (~5k lines) + filters
    (level chips V/D/I/W/E, tag/text search). Detect `ANR in`, `FATAL EXCEPTION`.
  - **Time-series store**: per-metric `FloatArray` ring (~300 samples) in a holder.
- `ui/ProfilerScreen.kt`: AppPickerDialog → tabs:
  - **Overview**: sparkline/area charts (reuse Charts lab drawing) for CPU, PSS, jank;
    numeric current values; start/stop session.
  - **Logcat**: live filtered list, pause/clear/share.
  - **Events**: ANR/crash markers with timestamps.
  - Export: CSV of samples + saved logcat slice.

## Build order (vertical slices)
1. `openShellFlow` in AdbClient + a throwaway test (logcat -d into a Text).
2. Sampler for CPU% + PSS → two live charts for a picked app.
3. Logcat live tab with level/text filters.
4. gfxinfo jank + batterystats; ANR/crash detection in the Events tab.
5. Session export (CSV/text), session start/stop + duration.

## Risks / notes
- `dumpsys`/`top` output varies by OEM/version (Moto/Android 16) → tolerant regex, show
  raw line fallback when a field can't be parsed.
- Keep the metric sampler and the logcat stream on separate connections/coroutines so
  neither starves the other; cancel both on screen leave.
- Gated behind the shared `AdbToolScaffold` connect banner (already built).

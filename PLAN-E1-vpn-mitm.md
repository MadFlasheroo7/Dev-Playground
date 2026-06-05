# E1 — Traffic inspector / MITM (no root)

## Goal
Capture and inspect device traffic without root; TLS-intercept your own app (Moneyview)
to validate its network defenses. Two phases — proxy first (tractable), VPN second (raw).

## Why phased
Full userspace TCP/IP over a TUN is the hard part. An **HTTP(S) MITM proxy** needs no
TCP stack: set the system proxy (we already have `settings put global http_proxy` in the
Network lab) and run a local proxy. The VpnService raw-capture is a later phase for apps
that ignore the system proxy or use non-HTTP protocols.

## Phase 1 — MITM proxy  ★★★★
### Components
- `util/MitmCa.kt` — generate an in-app root CA keypair (Conscrypt/BouncyCastle; both
  `conscrypt-android` + `sun-security-android` already in deps). Export CA cert (PEM/DER)
  + an "Install CA" intent (`Settings.ACTION_SECURITY_SETTINGS` / `KeyChain.createInstallIntent`).
  Show SHA-256 fingerprint.
- `proxy/MitmProxyService.kt` — a foreground `Service` running a `ServerSocket` on
  `127.0.0.1:<port>`. Threads/coroutines per connection:
  - Plain HTTP: read request line + headers, forward to origin, stream response, log.
  - HTTPS (`CONNECT host:443`): reply `200`, then `SSLServerSocket` to the client using a
    **dynamically minted leaf cert for the SNI host signed by our CA** (cache per host),
    and an `SSLSocket` to the real origin (validate normally). Relay decrypted bytes,
    parse HTTP/1.1 (and note h2 → fall back to tunnel-only).
- `data/FlowStore.kt` — in-memory ring of `Flow(id, ts, app?, method, scheme, host, path,
  reqHeaders, reqBody, status, respHeaders, respBody, reqBytes, respBytes, ms)`. Optional
  Room for persistence + HAR/PCAP-less HAR export.
- System-proxy wiring: reuse `NetworkProxyLabScreen` to point `http_proxy` at our port;
  toggle on/off; restore on stop.
### UI (`ui/TrafficInspectorScreen.kt`)
Flow list (filter host/status/app, search), detail with Request/Response tabs (pretty
JSON, hex for binary), "resend" (replay a request), export HAR, clear. Banner: CA install
state + proxy state.
### Build order (vertical slices)
1. CA gen + fingerprint + install-intent screen.
2. Plain-HTTP proxy + system-proxy toggle → see cleartext flows in a list.
3. TLS MITM (CONNECT + per-host leaf certs) → decrypt HTTPS.
4. Flow detail UI + JSON pretty-print + HAR export.
5. Replay/edit-and-resend.

## Phase 2 — VpnService raw capture  ★★★★★
- `CaptureVpnService : VpnService` → `Builder().addAddress("10.0.0.2",32).addRoute("0.0.0.0",0)
  .addDnsServer(...).addAllowedApplication(targetPkg).establish()` → TUN fd.
- Read IP packets; parse IPv4/IPv6 + TCP/UDP. For relay, `protect(socket)` real sockets so
  they bypass the VPN; bridge TUN flows ↔ protected sockets (minimal userspace TCP state
  machine, or bundle a tun2socks + in-app SOCKS relay). UDP via `DatagramChannel`.
- Feed reassembled HTTP into the same `FlowStore`; non-HTTP shown as raw flows; PCAP export.

## Risks / notes
- TLS MITM only works if the target trusts **user** CAs (its `network-security-config`).
  Banking apps often pin or trust system-only → **uninterceptable, which is the finding**
  for Moneyview. Surface that verdict explicitly.
- HTTP/2 + WebSocket: phase-1 can tunnel-without-decode; full h2 decode is later.
- Foreground service + persistent notification (have FOREGROUND_SERVICE, POST_NOTIFICATIONS).
- New perm for phase 2: `BIND_VPN_SERVICE` on the service; user consent dialog each enable.
- Self-testing own app only.

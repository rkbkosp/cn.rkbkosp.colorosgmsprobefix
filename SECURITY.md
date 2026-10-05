# Security policy

Report security issues privately to the owner of [the source repository](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix), using GitHub private vulnerability reporting when available. If it is unavailable, ask the owner for a private reporting channel without posting vulnerability details publicly. Do not publish signing material, passwords, private device logs/dumps, proxy credentials or a working exploit in an issue.

The module runs only inside the privileged `com.oplus.athena` vendor process and is firmware-specific. Proxy absence, unsuccessful responses and internal probe errors retain the original detector; this fallback is not a guarantee against all hook or process failures. Recheck compatibility after ROM/vendor APK updates. TLS verification must remain enabled, and only actual HTTP 204 may produce a success result.

Runtime logs include response/status, readiness/hook events and errors. Sanitize identifying data before sharing; private archival evidence is not part of the public repository. Requests pass through the configured local HTTP proxy and its upstream network. No additional analytics/log-upload service is provided by this project.

To recover the original detector, disable the module in LSPosed and reboot. Keep an independent recovery method before enabling it. This does not promise restoration of an earlier vendor policy snapshot. Releases use the existing signing identity; the expected signer certificate SHA-256 is `59ea4ac3a16001cf66899275068c39c4ae5fbeab74537305a8bb7f5f51063263`.

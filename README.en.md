# ColorOS GMS Probe Fix: Google connectivity probe module for ColorOS

[简体中文](README.md) | English

ColorOS GMS Probe Fix is an **LSPosed / Xposed API 82 module** for ColorOS vendor Google detection. It runs a real HTTP 204 probe through a local Mihomo HTTP/mixed proxy and leaves GMS restriction policy updates to the original controller. It primarily addresses a problem for transparent proxy users in rule-based proxy mode: the system fails to recognize that the proxy is running, concludes that GMS is unreachable, and blocks GMS network access.

**This module depends on specific firmware internals. It does not guarantee a fix for every Google Play or Google services connectivity problem. Device testing has passed on the OnePlus Ace 3 and OPPO Find X9 Ultra with ColorOS 16/17, but effectiveness on other models is not guaranteed.**

[Download APK](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases/latest) · [Module catalog repository](https://github.com/Xposed-Modules-Repo/cn.rkbkosp.colorosgmsprobefix) · [Changelog](CHANGELOG.md) · [Report an issue](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/issues) · [MIT license](LICENSE)

## Before you install

- A working LSPosed implementation supporting **Xposed API 82 / legacy** is required. There is no launcher entry or settings screen; manage the module in LSPosed.
- Athena must be able to reach a local **HTTP/mixed proxy at `127.0.0.1:7890`**. There is no port setting in the UI.
- Select **both `com.oplus.battery` and `com.oplus.athena`**. Do not select Android/System Framework or Google apps.
- Hooks run in a privileged vendor process. Back up important data and prepare a device-appropriate way to disable the module or recover before enabling it.

## Download and installation

1. Download `ColorOSGmsProbeFix-*.apk` from the assets on [Releases](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases/latest). The source ZIP is not an installable APK. Use that release's `SHA256SUMS` to check the file if needed.
2. Install the APK, enable **ColorOS GMS Probe Fix** in LSPosed, set the two scopes above again, and reboot.
3. Ensure the local HTTP/mixed proxy is available, check the logs below, and validate Google access, network transitions and full reboots on your own device. Rebooting is an installation step, not a claim of completed full-reboot acceptance.

## Behavior and boundaries

The module installs hooks only in the `com.oplus.athena` process. Select **both `com.oplus.battery` and `com.oplus.athena`** in LSPosed; shared-process loading is deduplicated. Do not select Android/System Framework.

It hooks `com.oplus.battery.restrictdynamicfeature.google.NetworkDetector.a(Context,int)` (the integer is a retry count, not a timeout). With an active system network and a listening HTTP proxy at **`127.0.0.1:7890`**, it reads the firmware's RUS address list through helper `h6.a` and probes those addresses using `Proxy.Type.HTTP`. If configuration lookup fails or the list is empty, the sampled firmware defaults are used:

- `https://www.google.com/generate_204`
- `http://www.google.com/gen_204`

Only an actual **HTTP 204** produces the corresponding Wi-Fi/mobile success result. Connection and read timeouts are 3 seconds each; redirects and response caching are disabled. TLS certificate verification remains intact. HTTP 200, redirects, unavailable proxy, unsuccessful probes, and internal hook errors do not manufacture success: the original detector runs instead.

This uses Mihomo's HTTP/mixed port **7890**, not its **TPROXY port 7895**. Other applications may continue using TPROXY. A different HTTP port requires changing the source port constant and rebuilding.

After capturing the original Google restriction controller's Handler, the module checks local proxy readiness first after 10 seconds, then every 30 seconds. A transition to ready queues the original controller's `R(long,int)` with `R(0L,1)`. Failed detection gets at most three readiness-triggered attempts in that cycle; success stops retries while readiness monitoring continues for proxy restart. Short outages/restarts under 30 seconds can be missed. Original network-change handling remains active.

Successful detection follows the vendor controller's normal policy update path. The module does not hook policy setters, directly edit BPF maps, fake a VPN, force unconditional success, or bypass the GMS switch, configuration, network checks, or HTTP 204 condition.

## Troubleshooting and recovery

- **No hook log:** confirm enablement, both scopes and a reboot. Look for `installed NetworkDetector.a(Context,int)`; `firmware signature mismatch or hook failure` calls for a fresh firmware review.
- **Proxy unavailable:** check for `proxy not ready; continue original detector`. Verify an HTTP/mixed listener at `127.0.0.1:7890` that Athena can reach.
- **Probe unsuccessful:** look for `probe response=204` and `real HTTP 204 via Mihomo; Google detection succeeds`. HTTP 200 and redirects do not qualify; failed probes fall back to the original detector.
- **Proxy starts late:** check `controller captured; proxy readiness recheck enabled` and `queued original controller check`. A `readiness hook unavailable` or `controller capture failed` log means readiness rechecks cannot be assumed to work; natural network checks may still run.
- **Instability or boot failure:** disable the module and reboot if the device is usable; otherwise use your prepared device/framework recovery method. The normal LSPosed UI may not be reachable.

For reports, include device model, exact ROM/Android build, whether it is a port, LSPosed/module versions, scopes, proxy port and sanitized logs. Distinguish an Athena restart from a full reboot; see [Contributing](CONTRIBUTING.md).

A 204 log is not proof of Google Play UI success. If restriction state remains after a successful probe, investigate vendor policy persistence/synchronization instead of forcing success. If policies are clear but traffic still fails, inspect proxy routing, IPv6 and DNS separately.

To recover the original detection implementation, disable the module in LSPosed and reboot. This does not promise restoration of an earlier restriction policy snapshot; the original controller manages its policy state.

## Privacy, risks, and disclaimer

Runtime logs cover response/status, hook/readiness events and errors; sanitize them before sharing. Probes send network requests through the configured local proxy, whose operator and upstreams can observe traffic according to their configuration. This project does not add an analytics or log-upload service.

The module runs inside a privileged vendor process. Incompatible hooks, proxy failures, or firmware changes can affect connectivity or process stability. It is provided **as is**, without warranty or guaranteed compatibility; make sure you can disable it if the device becomes unstable. See [SECURITY.md](SECURITY.md) and [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT, copyright © 2026 rkbkosp; see [LICENSE](LICENSE). Retain the copyright and license notices when redistributing. No warranty or liability is provided under the license.

ColorOS, OPPO, Google and other names are trademarks of their respective owners. This independent project is not affiliated with, endorsed by, or authorized by those organizations.

Third-party build components: Xposed API (`de.robv.android.xposed:api:82`, Apache License 2.0) is referenced at compile time only; the Gradle wrapper is Apache License 2.0. Vendor decompiled implementation/evidence is not included in the public source distribution.


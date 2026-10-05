# ColorOS GMS Probe Fix: Google connectivity probe module for ColorOS

[简体中文](README.md) | English

ColorOS GMS Probe Fix is an **LSPosed / Xposed API 82 module** for ColorOS vendor Google detection. It runs a real HTTP 204 probe through a local Mihomo HTTP/mixed proxy and leaves GMS restriction policy updates to the original controller.

**This module depends on specific firmware internals. It does not guarantee a fix for every Google Play or Google services connectivity problem. Full device acceptance of the public package has not been established; see the evidence limits below.**

[Download APK](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases/latest) · [Module catalog repository](https://github.com/Xposed-Modules-Repo/cn.rkbkosp.colorosgmsprobefix) · [Changelog](CHANGELOG.md) · [Report an issue](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/issues) · [MIT license](LICENSE)

## Before you install

- A working LSPosed implementation supporting **Xposed API 82** is required. There is no launcher entry or settings screen; manage the module in LSPosed.
- Athena must be able to reach a local **HTTP/mixed proxy at `127.0.0.1:7890`**. A TPROXY-only listener is insufficient; there is no port setting in the UI.
- Select **both `com.oplus.battery` and `com.oplus.athena`**. Do not select Android/System Framework or Google apps.
- The build minimum is **Android 9 / API 28**, not a promise of compatibility with every Android 9+ device. Vendor methods, configuration and controller internals must match.
- Hooks run in a privileged vendor process. Back up important data and prepare a device-appropriate way to disable the module or recover before enabling it.

## Download and installation

1. Download `ColorOSGmsProbeFix-*.apk` from the assets on [Releases](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases/latest). The source ZIP is not an installable APK. Use that release's `SHA256SUMS` to check the file if needed.
2. Disable the old **ColorOSGmsUnblock** module. If installed, disable and uninstall the private **`dev.local.colorosgmsprobe`** package first: both package and signing identities changed, so this is not an in-place update.
3. Install the APK, enable **ColorOS GMS Probe Fix** in LSPosed, set the two scopes above again, and reboot.
4. Ensure the local HTTP/mixed proxy is available, check the logs below, and validate Google access, network transitions and full reboots on your own device. Rebooting is an installation step, not a claim of completed full-reboot acceptance.

Application ID: `cn.rkbkosp.colorosgmsprobefix`. Version `0.2.1` (versionCode `3`, tag `3-0.2.1`) has APK and checksum assets on the [source Release](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases/tag/3-0.2.1) and [catalog Release](https://github.com/Xposed-Modules-Repo/cn.rkbkosp.colorosgmsprobefix/releases/tag/3-0.2.1). Publication does not establish device compatibility.

## Behavior and boundaries

The module installs hooks only in the `com.oplus.athena` process. Select **both `com.oplus.battery` and `com.oplus.athena`** in LSPosed; shared-process loading is deduplicated. Do not select Android/System Framework.

It hooks `com.oplus.battery.restrictdynamicfeature.google.NetworkDetector.a(Context,int)` (the integer is a retry count, not a timeout). With an active system network and a listening HTTP proxy at **`127.0.0.1:7890`**, it reads the firmware's RUS address list through helper `h6.a` and probes those addresses using `Proxy.Type.HTTP`. If configuration lookup fails or the list is empty, the sampled firmware defaults are used:

- `https://www.google.com/generate_204`
- `http://www.google.com/gen_204`

Only an actual **HTTP 204** produces the corresponding Wi-Fi/mobile success result. Connection and read timeouts are 3 seconds each; redirects and response caching are disabled. TLS certificate verification remains intact. HTTP 200, redirects, unavailable proxy, unsuccessful probes, and internal hook errors do not manufacture success: the original detector runs instead.

This uses Mihomo's HTTP/mixed port **7890**, not its **TPROXY port 7895**. Other applications may continue using TPROXY. A different HTTP port requires changing the source port constant and rebuilding.

After capturing the original Google restriction controller's Handler, the module checks local proxy readiness first after 10 seconds, then every 30 seconds. A transition to ready queues the original controller's `R(long,int)` with `R(0L,1)`. Failed detection gets at most three readiness-triggered attempts in that cycle; success stops retries while readiness monitoring continues for proxy restart. Short outages/restarts under 30 seconds can be missed. Original network-change handling remains active.

Successful detection follows the vendor controller's normal policy update path. The module does not hook policy setters, directly edit BPF maps, fake a VPN, force unconditional success, or bypass the GMS switch, configuration, network checks, or HTTP 204 condition.

## Compatibility and evidence limits

The imported analysis identifies the sample as **PMA110 / ColorOS V17.0.0 / Android 17**, with `Battery.apk` SHA-256:

```text
b12a0d3466e20c0d3001f844c47a74a36c45422d413452e615f1678d1fe99030
```

These are attributions from the original record, not a newly established device identity. The surrounding maintenance workspace concerns an Ace3 port; it does not establish that this public release was tested on an Ace3 or on a stock PMA110.

The **2026-10-02 archival evidence for private version 0.2** records successful hook installation in Athena, real proxy HTTP 204, `RESULT_WIFI_SUCCESS`, a queued readiness recheck, `google_restric_info` changing from 1 to 0, and both recorded network restriction BPF maps changing from three entries to zero. Only Athena was restarted; there was **no full-device reboot acceptance**, and **Google Play UI access was not confirmed**. The complete boot-before-proxy-start scenario was not directly reproduced.

Version **0.2.1 (versionCode 3)** changes public packaging/build/release identity, not runtime behavior. The archival observations are **not device acceptance of the newly packaged release**. Other ROMs, vendor APK revisions, and system updates need fresh method/configuration review and device validation. Private logs, vendor decompiled source, original test APKs, and device dumps are not published in this repository.

## Troubleshooting and recovery

- **No hook log:** confirm enablement, both scopes and a reboot. Look for `installed NetworkDetector.a(Context,int)`; `firmware signature mismatch or hook failure` calls for a fresh firmware review.
- **Proxy unavailable:** check for `proxy not ready; continue original detector`. Verify an HTTP/mixed listener at `127.0.0.1:7890` that Athena can reach.
- **Probe unsuccessful:** look for `probe response=204` and `real HTTP 204 via Mihomo; Google detection succeeds`. HTTP 200 and redirects do not qualify; failed probes fall back to the original detector.
- **Proxy starts late:** check `controller captured; proxy readiness recheck enabled` and `queued original controller check`. A `readiness hook unavailable` or `controller capture failed` log means readiness rechecks cannot be assumed to work; natural network checks may still run.
- **Instability or boot failure:** disable the module and reboot if the device is usable; otherwise use your prepared device/framework recovery method. The normal LSPosed UI may not be reachable.

For reports, include device model, exact ROM/Android build, whether it is a port, LSPosed/module versions, scopes, proxy port and sanitized logs. Distinguish an Athena restart from a full reboot; see [Contributing](CONTRIBUTING.md).

A 204 log is not proof of Google Play UI success. If restriction state remains after a successful probe, investigate vendor policy persistence/synchronization instead of forcing success. If policies are clear but traffic still fails, inspect proxy routing, IPv6 and DNS separately.

To recover the original detection implementation, disable the module in LSPosed and reboot. This does not promise restoration of an earlier restriction policy snapshot; the original controller manages its policy state.

## Build and public release

Requirements: **JDK 17**, Android SDK Platform **35**, Build Tools **35.0.0**. The project pins **AGP 8.7.3** and **Gradle 8.9**. Xposed API 82 is `compileOnly`, not bundled in the APK.

```sh
./gradlew :app:assembleDebug :app:assembleRelease
```

<details>
<summary>Maintainers: signing, releases and catalog synchronization</summary>

Release builds are **unsigned by default**. The public release workflow uses the existing PKCS#12 identity (alias `rkbkosp`), not a newly generated key. Configure repository Actions secrets:

- `GMS_PROBE_KEYSTORE_BASE64`: base64-encoded PKCS#12 keystore.
- `GMS_PROBE_KEYSTORE_PASSWORD`: its password.

Never commit either secret or the keystore. The signed GitHub workflow must verify this APK signer certificate SHA-256 before publication:

```text
59ea4ac3a16001cf66899275068c39c4ae5fbeab74537305a8bb7f5f51063263
```

Tags use `versionCode-versionName`; this release is **`3-0.2.1`**. The signed APK and SHA-256 checksum are attached to the matching GitHub Release. Use a new tag matching the build configuration for a new version; do not republish the old tag. Build reproducibility here means pinned build inputs and a documented build path, not a claim of demonstrated bit-for-bit identical APKs.

The catalog repository description is the module name: keep `ColorOS GMS Probe Fix`. Put the short description in extensionless [`SUMMARY`](SUMMARY) and the full description in `README.md`, as required by the [official submission guide](https://github.com/Xposed-Modules-Repo/submission/blob/master/README.md). Source and catalog repositories are separate; merging a source PR does not synchronize catalog documentation. Use absolute source-repository links in catalog copies.

</details>

## Privacy, risks, and disclaimer

Runtime logs cover response/status, hook/readiness events and errors; sanitize them before sharing. Probes send network requests through the configured local proxy, whose operator and upstreams can observe traffic according to their configuration. This project does not add an analytics or log-upload service.

The module runs inside a privileged vendor process. Incompatible hooks, proxy failures, or firmware changes can affect connectivity or process stability. It is provided **as is**, without warranty or guaranteed compatibility; make sure you can disable it if the device becomes unstable. See [SECURITY.md](SECURITY.md) and [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT, copyright © 2026 rkbkosp; see [LICENSE](LICENSE). Retain the copyright and license notices when redistributing. No warranty or liability is provided under the license.

ColorOS, OPPO, Google and other names are trademarks of their respective owners. This independent project is not affiliated with, endorsed by, or authorized by those organizations.

Third-party build components: Xposed API (`de.robv.android.xposed:api:82`, Apache License 2.0) is referenced at compile time only; the Gradle wrapper is Apache License 2.0. Vendor decompiled implementation/evidence is not included in the public source distribution.


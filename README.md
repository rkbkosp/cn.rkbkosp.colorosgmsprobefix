# ColorOS GMS Probe Fix

[简体中文](README.zh-CN.md)

An Xposed API 82 / LSPosed compatibility module that lets ColorOS perform a **real Google connectivity probe through the local HTTP proxy**, while retaining the vendor's normal restriction/unrestriction logic. Application ID and namespace: `cn.rkbkosp.colorosgmsprobefix`.

- [Source](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix)
- [Releases](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases)
- [LSPosed/Xposed Modules Repo catalog](https://github.com/Xposed-Modules-Repo/cn.rkbkosp.colorosgmsprobefix): [submission #2024](https://github.com/Xposed-Modules-Repo/submission/issues/2024) approved and repository created. An installable catalog listing requires a signed APK release; repository creation alone is not listing verification.

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

## Install, migrate, and recover

1. Have a working LSPosed installation and Mihomo HTTP/mixed listener at `127.0.0.1:7890`, reachable from Athena. Keep an independent recovery path.
2. Disable the old **ColorOSGmsUnblock** module; do not enable both approaches together.
3. Disable and uninstall the private **`dev.local.colorosgmsprobe`** build before installing this public APK. Both the package ID and signing identity changed, so this is not an in-place update; LSPosed enablement and scope must be set again.
4. Install the release APK, enable ColorOS GMS Probe Fix, select only `com.oplus.battery` and `com.oplus.athena`, then reboot. Reboot is an installation instruction, not a claim that full-reboot acceptance has already passed.
5. Check LSPosed logs for `installed NetworkDetector.a(Context,int)`, `probe response=204`, and `real HTTP 204 via Mihomo; Google detection succeeds`. Validate your own Google access, Wi-Fi/mobile transitions, and restriction state.

A 204 log is not proof of Google Play UI success. If restriction state remains after a successful probe, investigate vendor policy persistence/synchronization instead of forcing success. If policies are clear but traffic still fails, inspect proxy routing, IPv6 and DNS separately.

To recover the original detection implementation, disable the module in LSPosed and reboot. This does not promise restoration of an earlier restriction policy snapshot; the original controller manages its policy state.

## Build and public release

Requirements: **JDK 17**, Android SDK Platform **35**, Build Tools **35.0.0**. The project pins **AGP 8.7.3** and **Gradle 8.9**. Xposed API 82 is `compileOnly`, not bundled in the APK.

```sh
./gradlew :app:assembleDebug :app:assembleRelease
```

Release builds are **unsigned by default**. The public release workflow uses the existing PKCS#12 identity (alias `rkbkosp`), not a newly generated key. Configure repository Actions secrets:

- `GMS_PROBE_KEYSTORE_BASE64`: base64-encoded PKCS#12 keystore.
- `GMS_PROBE_KEYSTORE_PASSWORD`: its password.

Never commit either secret or the keystore. The signed GitHub workflow must verify this APK signer certificate SHA-256 before publication:

```text
59ea4ac3a16001cf66899275068c39c4ae5fbeab74537305a8bb7f5f51063263
```

Tags use `versionCode-versionName`; this release is **`3-0.2.1`**. A signed APK and SHA-256 checksum belong on the matching GitHub Release. GitHub publication and catalog approval are distinct; a source URL or catalog target alone does not prove either has completed. Build reproducibility here means pinned build inputs and a documented build path, not a claim of demonstrated bit-for-bit identical APKs.

## Privacy, risks, and disclaimer

Runtime logs cover response/status, hook/readiness events and errors; sanitize them before sharing. Probes send network requests through the configured local proxy, whose operator and upstreams can observe traffic according to their configuration. This project does not add an analytics or log-upload service.

The module runs inside a privileged vendor process. Incompatible hooks, proxy failures, or firmware changes can affect connectivity or process stability. It is provided **as is**, without warranty or guaranteed compatibility; make sure you can disable it if the device becomes unstable. See [SECURITY.md](SECURITY.md) and [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT, copyright © 2026 rkbkosp; see [LICENSE](LICENSE). Retain the copyright and license notices when redistributing. No warranty or liability is provided under the license.

ColorOS, OPPO, Google and other names are trademarks of their respective owners. This independent project is not affiliated with, endorsed by, or authorized by those organizations.

Third-party build components: Xposed API (`de.robv.android.xposed:api:82`, Apache License 2.0) is referenced at compile time only; the Gradle wrapper is Apache License 2.0. Vendor decompiled implementation/evidence is not included in the public source distribution.

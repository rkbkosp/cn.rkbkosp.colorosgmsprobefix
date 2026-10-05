# Changelog

## 0.2.1 (version code 3)

- Package the preserved 0.2 runtime as an independent public project, with application ID and namespace `cn.rkbkosp.colorosgmsprobefix`.
- Pin AGP 8.7.3, Gradle 8.9, JDK 17, SDK 35 and Build Tools 35.0.0; document reproducible build inputs and unsigned-by-default release builds.
- Prepare public GitHub release packaging and existing-identity signing with certificate verification; release tag `3-0.2.1`.
- Add English/Chinese installation, migration, compatibility, privacy and recovery documentation. Exclude private evidence, vendor decompiled source and signing material.
- No new runtime behavior fixes. The package/signing identity differs from `dev.local.colorosgmsprobe`; uninstall the old build and set LSPosed scope again.
- Historical 0.2 observations are not acceptance of this newly packaged release. Full-device reboot and Google Play UI access remain unconfirmed by the archival evidence.

## 0.2 (historical private build)

- Fix the confirmed enum lookup failure by comparing `Enum.name()` over `getEnumConstants()` instead of looking up JADX-recovered field names.
- Capture the original controller Handler and check proxy readiness after 10 seconds, then every 30 seconds; queue original `R(0L,1)` detection with at most three attempts per readiness cycle, stopping retries on success while monitoring restart.
- Deduplicate hooks when both selected packages load in the shared Athena process.
- The 2026-10-02 archival record reports installed hooks, real proxy HTTP 204 / `RESULT_WIFI_SUCCESS`, a queued readiness recheck, restriction state changing to zero and two restriction maps clearing. It records an Athena process restart only, not full reboot or Google Play UI acceptance.

## 0.1 (historical private build)

- Introduce proxy-based real HTTP 204 detection with original-detector fallback.
- Device hook installation failed with `NoSuchFieldError`: `RESULT_WIFI_SUCCESS` was a JADX display name, not the original obfuscated enum field name. This version did not establish successful restriction removal.

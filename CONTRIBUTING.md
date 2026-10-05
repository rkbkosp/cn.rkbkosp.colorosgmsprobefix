# Contributing

Bug reports and reviews are welcome at [the source repository](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix). Include module and LSPosed versions, exact ROM/build, whether the ROM is a port, the device model as actually established, the relevant vendor APK hash, selected scopes and sanitized `ColorOSGmsProbeFix` log lines. Describe proxy HTTP port/readiness and whether the observation used an Athena restart or a full reboot. Do not infer a tested device identity from the archival PMA110 record.

Before changing hooks, establish the target firmware's `NetworkDetector.a(Context,int)`, result enum names, RUS helper `h6.a`, and controller `R(long,int)` shapes. Preserve the narrow Athena-only runtime, both package scopes, real HTTP 204 requirement, normal TLS checks and original-detector fallback. Explain every intentional behavior change and its evidence; build success is not device acceptance. State explicitly whether full-reboot, delayed-proxy startup, Wi-Fi/mobile transitions, restriction-state changes, Google Play UI and disable/recovery were actually checked.

Do not commit private device evidence, full logs/dumps, account identifiers, vendor decompiled code/APKs, generated APKs, signing keys/passwords, local SDK paths or proxy credentials. Summarize reproducible findings without publishing private source material. Build with `./gradlew :app:assembleDebug :app:assembleRelease`; release output is unsigned by default. Public signing belongs to the existing protected release identity, never a replacement key added to a patch.

Contributions are licensed under the project's MIT license (inbound = outbound).

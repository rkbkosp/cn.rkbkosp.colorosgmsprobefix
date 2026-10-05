# ColorOS GMS Probe Fix：ColorOS Google 可达性检测修复模块

简体中文 | [English](README.en.md)

ColorOS GMS Probe Fix 是面向 **ColorOS 厂商 Google 检测逻辑**的 **LSPosed / Xposed API 82 模块**，让检测经本地 Mihomo HTTP/mixed 代理完成真实的 HTTP 204 探测，再由原控制器处理 GMS 限制与解除。

**模块依赖特定固件实现，不保证解决所有 Google Play / Google 服务联网问题。当前公开包尚无完整的实机验收记录，详见下文。**

[下载 APK](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases/latest) · [模块目录仓库](https://github.com/Xposed-Modules-Repo/cn.rkbkosp.colorosgmsprobefix) · [更新记录](CHANGELOG.md) · [反馈问题](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/issues) · [MIT 许可证](LICENSE)

## 使用前必读

- 需要已安装且正常工作的 LSPosed，并支持 **Xposed API 82**。本模块没有桌面启动入口或设置界面，请通过 LSPosed 管理。
- 需要 Athena 可访问的本地 **HTTP/mixed `127.0.0.1:7890`** 代理。只有 TPROXY 监听不满足条件；端口不可在界面中修改。
- 作用域同时选择 **`com.oplus.battery` 和 `com.oplus.athena`**，不要选择 Android/系统框架或 Google 应用。
- 构建下限是 **Android 9 / API 28**，不代表兼容所有 Android 9 及以上设备；厂商方法名、配置和控制器必须匹配。
- 模块运行在高权限厂商进程内。启用前备份重要数据，并准备适合自己设备的模块停用/救援方法。

## 下载与安装

1. 从 [Releases](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases/latest) 的资源列表下载 `ColorOSGmsProbeFix-*.apk`，不要下载源码 ZIP 作为安装包。同版本 `SHA256SUMS` 可用于核对文件。
2. 停用旧 **ColorOSGmsUnblock**。如安装过私有包 **`dev.local.colorosgmsprobe`**，先停用并卸载；公开包的包名与签名身份均已变化，不能覆盖升级。
3. 安装 APK，在 LSPosed 启用 **ColorOS GMS Probe Fix**，重新确认上述两个作用域，然后重启设备。
4. 确认本地 HTTP/mixed 代理可用，检查下文日志，并在自己的设备上验证 Google 访问、网络切换与完整重启后的表现。重启是安装步骤，不表示本公开版本已通过整机重启验收。

应用 ID：`cn.rkbkosp.colorosgmsprobefix`。当前 `0.2.1`（versionCode `3`，tag `3-0.2.1`）已在[源码 Release](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases/tag/3-0.2.1)和[目录 Release](https://github.com/Xposed-Modules-Repo/cn.rkbkosp.colorosgmsprobefix/releases/tag/3-0.2.1)提供 APK 与校验文件。发布存在不等于兼容性验收通过。

## 功能与边界

只在 **`com.oplus.athena` 进程**安装 hook。LSPosed 作用域需要同时选择 **`com.oplus.battery` 和 `com.oplus.athena`**；共享进程重复加载会去重。不要选择 Android/系统框架。

模块 hook `com.oplus.battery.restrictdynamicfeature.google.NetworkDetector.a(Context,int)`，第二个参数是重试次数而非超时。当系统存在活动网络且 **`127.0.0.1:7890`** HTTP 代理接受连接时，通过固件 RUS helper **`h6.a`** 读取地址列表，使用 `Proxy.Type.HTTP` 请求原地址。配置读取失败或列表为空时，使用提取固件中的默认地址：

- `https://www.google.com/generate_204`
- `http://www.google.com/gen_204`

只有实际收到 **HTTP 204** 才返回对应 Wi-Fi/移动网络成功枚举。连接、读取超时各 3 秒，不跟随重定向、不使用响应缓存，**不修改 TLS 证书验证**。HTTP 200、重定向、代理不可用、探测失败或 hook 内部异常都不会伪造成功，而是继续原厂检测。

探测显式使用 Mihomo 的 HTTP/mixed **7890** 端口，**不是 TPROXY 7895**。其他应用仍可使用 TPROXY。更换 HTTP 端口需要修改源码端口常量并重新构建。

捕获原 Google 限制控制器的 Handler 后，首次在 10 秒后检查本地代理就绪状态，此后每 30 秒检查一次。代理从不可用变为就绪时，通过原控制器 `R(long,int)` 的 `R(0L,1)` 排入正常检测队列；该周期内失败时最多尝试三次就绪补测，成功后停止补测，但继续观察代理重启。持续不足 30 秒的停止/重启可能漏检，原系统网络变化事件仍继续工作。

成功检测由原控制器自然更新限制策略。模块不 hook 策略设置方法、不直接编辑 BPF 表、不伪造 VPN、不无条件成功，也不绕过 GMS 开关、配置启用判断、网络条件或 HTTP 204 要求。

## 兼容性与证据边界

原始分析将样本记录为 **PMA110 / ColorOS V17.0.0 / Android 17**，对应 `Battery.apk` SHA-256：

```text
b12a0d3466e20c0d3001f844c47a74a36c45422d413452e615f1678d1fe99030
```

以上是原记录的归属描述，不是此次重新确认的设备身份。外围维护工作区涉及 Ace3 移植系统，不能据此断言本公开版本已在 Ace3 或原厂 PMA110 上测试。

**2026-10-02 私有 0.2 版本的归档证据**记录：Athena 内 hook 安装成功，代理探测真实返回 HTTP 204 与 `RESULT_WIFI_SUCCESS`，就绪补测进入原控制器队列，`google_restric_info` 从 1 变为 0，记录中的两张网络限制 BPF 表均从三项变为零项。操作只重启了 Athena 进程；**未完成整机重启验收，未确认 Google Play 界面最终访问效果**，也未直接复现完整开机早于代理启动的场景。

**0.2.1（versionCode 3）**只调整公开包身份、构建和发布包装，不改变既有运行逻辑。归档观察**不是新公开包的实机验收**。其他 ROM、厂商 APK 修订和系统更新需要重新核对方法/配置并进行设备验证。本仓库不发布私有日志、厂商反编译源码、原始测试 APK 或设备转储。

## 排查与恢复

- **找不到 hook 日志：**确认模块启用、两个作用域正确且已经重启，查找 `installed NetworkDetector.a(Context,int)`。出现 `firmware signature mismatch or hook failure` 时，需要重新核对固件实现。
- **代理未就绪：**检查 `proxy not ready; continue original detector`，确认监听是 HTTP/mixed `127.0.0.1:7890`，且 Athena 能访问。
- **探测未成功：**查找 `probe response=204` 与 `real HTTP 204 via Mihomo; Google detection succeeds`。HTTP 200/重定向不满足成功条件；失败时回到原检测。
- **代理晚于系统启动：**检查 `controller captured; proxy readiness recheck enabled` 和 `queued original controller check`。出现 `readiness hook unavailable` 或 `controller capture failed` 时，补测不可视为已工作；原有网络检测仍可能运行。
- **系统不稳定或无法开机：**能进入系统时停用模块并重启；否则使用事先准备的设备/框架救援方法，普通 LSPosed 界面可能不可用。

反馈时提供设备型号、完整 ROM/Android 版本、是否为移植 ROM、LSPosed/模块版本、作用域、代理端口和脱敏日志，并区分 Athena 重启与整机重启，见[贡献指南](CONTRIBUTING.md)。

204 日志不等于 Google Play 界面访问成功。成功探测后限制仍存在时，应排查厂商策略持久化/同步，而不是添加无条件成功 hook。策略已清除但仍无法联网时，请分别检查代理路由、IPv6 和 DNS。

恢复原检测实现：在 LSPosed 停用模块并重启。这不承诺恢复到此前的限制策略快照，策略状态由原控制器管理。

## 构建与公开发布

需要 **JDK 17**、Android SDK Platform **35**、Build Tools **35.0.0**；项目固定 **AGP 8.7.3 / Gradle 8.9**。Xposed API 82 为 `compileOnly`，不打包进 APK。

```sh
./gradlew :app:assembleDebug :app:assembleRelease
```

<details>
<summary>维护者：签名发布与模块目录同步</summary>

Release **默认未签名**。正式 GitHub 工作流使用现有 PKCS#12 签名身份（alias `rkbkosp`），不新建密钥。配置仓库 Actions Secrets：

- `GMS_PROBE_KEYSTORE_BASE64`：PKCS#12 keystore 的 Base64 内容。
- `GMS_PROBE_KEYSTORE_PASSWORD`：keystore 密码。

密钥、密码和上述 Secret 均不得提交。签名发布工作流必须在发布前检查 APK 签名证书 SHA-256：

```text
59ea4ac3a16001cf66899275068c39c4ae5fbeab74537305a8bb7f5f51063263
```

tag 格式为 `versionCode-versionName`，本次为 **`3-0.2.1`**。签名 APK 与 SHA-256 校验文件附在对应 GitHub Release；发布新版本时使用与构建配置一致的新 tag，不要重复发布旧 tag。这里的可复现构建指固定构建输入与公开构建步骤，不宣称已证明 APK 逐字节完全一致。

目录仓库描述用作模块名称，保留 `ColorOS GMS Probe Fix`；功能摘要放在无扩展名的 [`SUMMARY`](SUMMARY)，完整说明放在 `README.md`，见[官方提交说明](https://github.com/Xposed-Modules-Repo/submission/blob/master/README.md)。源码仓库与目录仓库独立，源码 PR 合并不会自动同步目录说明。同步到目录的文档链接应指向源码仓库的绝对地址。

</details>

## 隐私、风险与免责声明

运行日志记录响应/状态、hook/就绪事件及错误，分享前请脱敏。探测经本地代理发送网络请求，代理运营方及其上游可按配置观察流量。本项目不额外提供遥测或日志上传服务。

模块运行在具有高权限的厂商进程内。不兼容 hook、代理故障或固件变化可能影响网络与进程稳定性。项目**按现状提供**，不保证兼容或可用；启用前请确保设备异常时能停用。另见 [SECURITY.md](SECURITY.md) 与 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 许可证

MIT，Copyright © 2026 rkbkosp，见 [LICENSE](LICENSE)。分发时保留版权声明和许可证；许可证不提供担保或责任承诺。

ColorOS、OPPO、Google 等名称属于各自权利人的商标。本项目独立开发，与上述组织无关联，未经其授权或背书。

第三方构建组件：Xposed API（`de.robv.android.xposed:api:82`，Apache License 2.0）仅用于编译期引用；Gradle wrapper 为 Apache License 2.0。公开源码不包含厂商反编译实现或私有证据。


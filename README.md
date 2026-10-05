# ColorOS GMS Probe Fix：ColorOS Google 可达性检测修复模块

简体中文 | [English](README.en.md)

ColorOS GMS Probe Fix 是面向 **ColorOS 厂商 Google 检测逻辑**的 **LSPosed / Xposed API 82 模块**，让检测经本地 Mihomo HTTP/mixed 代理完成真实的 HTTP 204 探测，再由原控制器处理 GMS 限制与解除，主要解决透明代理用户在规则代理模式下，系统不能正确识别代理运行情况，以此判断用户无法连通 GMS，并阻止 GMS 联网的问题。

**模块依赖特定固件实现，不保证解决所有 Google Play / Google 服务联网问题。已在 ColorOS 16/17  Oneplus Ace 3 和 OPPO Find X9 Ultra 上实机测试通过，但不能保证其他机型仍能生效**

[下载 APK](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases/latest) · [模块目录仓库](https://github.com/Xposed-Modules-Repo/cn.rkbkosp.colorosgmsprobefix) · [更新记录](CHANGELOG.md) · [反馈问题](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/issues) · [MIT 许可证](LICENSE)

## 使用前必读

- 需要已安装且正常工作的 LSPosed，并支持 **Xposed API 82/legacy**。本模块没有桌面启动入口或设置界面，请通过 LSPosed 管理。
- 需要 Athena 可访问的本地 **HTTP/mixed `127.0.0.1:7890`** 代理。端口不可在界面中修改。
- 作用域同时选择 **`com.oplus.battery` 和 `com.oplus.athena`**，不要选择 Android/系统框架或 Google 应用。
- 模块运行在高权限厂商进程内。启用前备份重要数据，并准备适合自己设备的模块停用/救援方法。

## 下载与安装

1. 从 [Releases](https://github.com/rkbkosp/cn.rkbkosp.colorosgmsprobefix/releases/latest) 的资源列表下载 `ColorOSGmsProbeFix-*.apk`，不要下载源码 ZIP 作为安装包。同版本 `SHA256SUMS` 可用于核对文件。
2. 安装 APK，在 LSPosed 启用 **ColorOS GMS Probe Fix**，重新确认上述两个作用域，然后重启设备。
3. 确认本地 HTTP/mixed 代理可用，检查下文日志，并在自己的设备上验证 Google 访问、网络切换与完整重启后的表现。重启是安装步骤，不表示本公开版本已通过整机重启验收。

## 功能与边界

只在 **`com.oplus.athena` 进程**安装 hook。LSPosed 作用域需要同时选择 **`com.oplus.battery` 和 `com.oplus.athena`**；共享进程重复加载会去重。不要选择 Android/系统框架。

模块 hook `com.oplus.battery.restrictdynamicfeature.google.NetworkDetector.a(Context,int)`，第二个参数是重试次数而非超时。当系统存在活动网络且 **`127.0.0.1:7890`** HTTP 代理接受连接时，通过固件 RUS helper **`h6.a`** 读取地址列表，使用 `Proxy.Type.HTTP` 请求原地址。配置读取失败或列表为空时，使用提取固件中的默认地址：

- `https://www.google.com/generate_204`
- `http://www.google.com/gen_204`

只有实际收到 **HTTP 204** 才返回对应 Wi-Fi/移动网络成功枚举。连接、读取超时各 3 秒，不跟随重定向、不使用响应缓存，**不修改 TLS 证书验证**。HTTP 200、重定向、代理不可用、探测失败或 hook 内部异常都不会伪造成功，而是继续原厂检测。

探测显式使用 Mihomo 的 HTTP/mixed **7890** 端口，**不是 TPROXY 7895**。其他应用仍可使用 TPROXY。更换 HTTP 端口需要修改源码端口常量并重新构建。

捕获原 Google 限制控制器的 Handler 后，首次在 10 秒后检查本地代理就绪状态，此后每 30 秒检查一次。代理从不可用变为就绪时，通过原控制器 `R(long,int)` 的 `R(0L,1)` 排入正常检测队列；该周期内失败时最多尝试三次就绪补测，成功后停止补测，但继续观察代理重启。持续不足 30 秒的停止/重启可能漏检，原系统网络变化事件仍继续工作。

成功检测由原控制器自然更新限制策略。模块不 hook 策略设置方法、不直接编辑 BPF 表、不伪造 VPN、不无条件成功，也不绕过 GMS 开关、配置启用判断、网络条件或 HTTP 204 要求。

## 排查与恢复

- **找不到 hook 日志：** 确认模块启用、两个作用域正确且已经重启，查找 `installed NetworkDetector.a(Context,int)`。出现 `firmware signature mismatch or hook failure` 时，需要重新核对固件实现。
- **代理未就绪：** 检查 `proxy not ready; continue original detector`，确认监听是 HTTP/mixed `127.0.0.1:7890`，且 Athena 能访问。
- **探测未成功：** 查找 `probe response=204` 与 `real HTTP 204 via Mihomo; Google detection succeeds`。HTTP 200/重定向不满足成功条件；失败时回到原检测。
- **代理晚于系统启动：** 检查 `controller captured; proxy readiness recheck enabled` 和 `queued original controller check`。出现 `readiness hook unavailable` 或 `controller capture failed` 时，补测不可视为已工作；原有网络检测仍可能运行。
- **系统不稳定或无法开机：** 能进入系统时停用模块并重启；否则使用事先准备的设备/框架救援方法，普通 LSPosed 界面可能不可用。

反馈时提供设备型号、完整 ROM/Android 版本、是否为移植 ROM、LSPosed/模块版本、作用域、代理端口和脱敏日志，并区分 Athena 重启与整机重启，见[贡献指南](CONTRIBUTING.md)。

204 日志不等于 Google Play 界面访问成功。成功探测后限制仍存在时，应排查厂商策略持久化/同步，而不是添加无条件成功 hook。策略已清除但仍无法联网时，请分别检查代理路由、IPv6 和 DNS。

恢复原检测实现：在 LSPosed 停用模块并重启。这不承诺恢复到此前的限制策略快照，策略状态由原控制器管理。

## 隐私、风险与免责声明

运行日志记录响应/状态、hook/就绪事件及错误，分享前请脱敏。探测经本地代理发送网络请求，代理运营方及其上游可按配置观察流量。本项目不额外提供遥测或日志上传服务。

模块运行在具有高权限的厂商进程内。不兼容 hook、代理故障或固件变化可能影响网络与进程稳定性。项目**按现状提供**，不保证兼容或可用；启用前请确保设备异常时能停用。另见 [SECURITY.md](SECURITY.md) 与 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 许可证

MIT，Copyright © 2026 rkbkosp，见 [LICENSE](LICENSE)。分发时保留版权声明和许可证；许可证不提供担保或责任承诺。

ColorOS、OPPO、Google 等名称属于各自权利人的商标。本项目独立开发，与上述组织无关联，未经其授权或背书。

第三方构建组件：Xposed API（`de.robv.android.xposed:api:82`，Apache License 2.0）仅用于编译期引用；Gradle wrapper 为 Apache License 2.0。公开源码不包含厂商反编译实现或私有证据。


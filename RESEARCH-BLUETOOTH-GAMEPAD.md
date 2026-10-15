# Android 手机作为 Windows 蓝牙手柄：资料核对

检索及核对日期：2026-10-14。本文记录网上项目、源码与问题讨论的证据；不代表已在本项目的 iQOO / Windows / 星露谷组合上验证它们。下面保留当时的研究与故障分析；用户随后决定移除 Xbox BLE，3.0 的当前实现和验收步骤见 [README](README.md)。

## 结论

Android 手机作为真正的蓝牙 HID 手柄已有实现。经典 HID 是正式的输入协议；不出现在 XInput 的四个槽位中，不代表没有识别成手柄。我们此前已经通过 Windows WinMM 读到经典 HID 的轴变化，Steam 日志也发现了 Pocket Gamepad。这条已验证到输入层的路线应作为主要兼容修复对象。

Windows / 浏览器 / 游戏所理解的布局不一定相同。手机成功发送字节、Windows 枚举出轴、浏览器显示 standard、Steam 映射正确、目标游戏可玩，是不同的验证步骤。此前把解决映射问题主要转向 Xbox BLE，缺少 Android 上完整 Xbox 兼容案例的支撑。

## Android 经典 HID 项目

### DaveHanak/BtGamepad

- [项目及 README](https://github.com/DaveHanak/BtGamepad)
- [Windows DirectInput 描述符](https://github.com/DaveHanak/BtGamepad/blob/main/app/src/main/java/com/umpa/btgamepad/HidReportDescriptorDirect.kt)
- [DirectInput 输入编码](https://github.com/DaveHanak/BtGamepad/blob/main/app/src/main/java/com/umpa/btgamepad/GamepadInputDirect.kt)
- [Android HID 注册与连接](https://github.com/DaveHanak/BtGamepad/blob/main/app/src/main/java/com/umpa/btgamepad/GamepadDevice.kt)

README 明确定位 Android 9+ 手机模拟通用蓝牙 HID 手柄，分别提供 DirectInput 与 Raw 模式。源码中 DirectInput 描述符有 10 个按钮、X/Y/Z/Rx/Ry 和 Hat；右摇杆使用 Rx/Ry，左右扳机共用 Z。说明面向 Windows 的 HID 设计已有可参考实现。

其共用 Z 的触发方式不能满足独立读取两个模拟扳机的目标。作者还报告实时输入存在延迟，并明确不排除自身实现原因；这不是手机硬件不可能胜任的证明。没有发现仓库许可证，不直接复制代码。

### 4zyz4/GKME

- [项目及 README](https://github.com/4zyz4/GKME)
- [经典 HID 传输及平台描述符](https://github.com/4zyz4/GKME/blob/master/android/src/main/java/com/zyz4/gkme/service/ClassicHidTransport.kt)
- [各平台输入报告编码](https://github.com/4zyz4/GKME/blob/master/android/src/main/java/com/zyz4/gkme/service/GamepadStateMapper.kt)
- [连接与传输文档](https://github.com/4zyz4/GKME/blob/master/docs/service-transport.md)

源码确认蓝牙直连采用 Android BluetoothHidDevice，区分 Windows、Android、Linux，并有仅手柄模式。Windows 布局为 18 个按钮加填充、四个 16 位有符号轴，共 11 字节，独立于其 WiFi / USB 接收端方案。注册过程处理旧注册清理、回调、超时与重连。

不能把整个项目的震动能力直接归到蓝牙模式：传输文档明确指出蓝牙输出报告目前丢弃。Windows 描述符没有独立模拟扳机字段，轴注释也存在笔误（0x33 实际是 Rx，注释写 Ry），因此只能参考设计经验，不能整套照搬。README 声明 GPLv3；直接复用源码需按其许可证处理。本次没有把该项目代码加入 App。

### jobrobse/Pocket-Pad

- [项目介绍](https://github.com/jobrobse/Pocket-Pad)
- [映射问题 #5](https://github.com/jobrobse/Pocket-Pad/issues/5)
- [源码公开请求 #2](https://github.com/jobrobse/Pocket-Pad/issues/2)

README 声称 Android 蓝牙 HID 能直连 Windows，不需接收端；仓库没有可核对的实现源码。Issue #5 报告扳机被当成右摇杆、肩键变成摇杆按下等问题，与本项目现象接近，但该用户的主机为 Linux。这是平台映射差异的相关经验，不是 Windows 上的修复方案或成功测试记录。

## Android BLE HID 与 Xbox BLE

### kshoji/BLE-HID-Peripheral-for-Android

- [项目 README](https://github.com/kshoji/BLE-HID-Peripheral-for-Android)
- [Windows 读完 HID Information 后断开 #2](https://github.com/kshoji/BLE-HID-Peripheral-for-Android/issues/2)
- [Windows 手机配对后自动断开 #13](https://github.com/kshoji/BLE-HID-Peripheral-for-Android/issues/13)
- [已合并的 Windows 连接与输入修复 PR #18](https://github.com/kshoji/BLE-HID-Peripheral-for-Android/pull/18)

PR #18 于 2026-09-23 创建，查询时已合并。修复内容包括串行注册服务、服务就绪后广播、Report Protocol 默认值 0x01、Android 13+ 显式通知 payload 和订阅后登记主机。提交者勾选了 Windows 10 配对、连接稳定及 HID 输入发送的测试。

测试方案是 Keyboard，Mouse 为可选；不是 Windows Xbox/XInput 手柄验证。我们的现有实现已经包含其中若干措施（串行建服务、服务完成后广播、Protocol Mode=1、Android 13+ 显式通知），不能把这些再次加入就声称解决了故障。

### 04Alek04/BLE_HID_EXAMPLE

- [项目 README](https://github.com/04Alek04/BLE_HID_EXAMPLE)
- [Android 14 能配对但无输入 #3](https://github.com/04Alek04/BLE_HID_EXAMPLE/issues/3)

README 包含 BLE 触控 Gamepad，作者在 #3 评论称 v2.0 应修复输入问题。可以继续检查服务与通知实现，但现有资料不足以证明其 Gamepad 在 Windows 11 原生 XInput 与震动可用。

### Mystfit/ESP32-BLE-CompositeHID

- [项目](https://github.com/Mystfit/ESP32-BLE-CompositeHID)
- [XInput 重连后无输入 #32](https://github.com/Mystfit/ESP32-BLE-CompositeHID/issues/32)
- [首次可用、重连失效 #28](https://github.com/Mystfit/ESP32-BLE-CompositeHID/issues/28)
- [把连接标志延后到认证完成的修复](https://github.com/Mystfit/ESP32-BLE-CompositeHID/commit/a0d4a02b796f461fccb92b803df7873379a93f46)
- [v0.3.1 的 Windows 重连问题 #68](https://github.com/Mystfit/ESP32-BLE-CompositeHID/issues/68)

这个项目有 Windows Xbox/XInput、Steam 识别与震动的用户报告，也是当前 Xbox 描述符来源。但它运行于 ESP32 / NimBLE，不是 Android 手机。其 Windows 重连问题和修复说明认证、连接状态及通知流程有实际影响；不能直接套用其 API，也不能把成功案例当作 Android 可行性证明。

本次找到 Android 经典 HID 及 Android BLE HID 的实际实现，没有找到足以证明“普通无 Root Android 手机原生模拟 Xbox BLE，Windows 自动 XInput 并回传游戏震动”全流程稳定可用的案例。这是检索结果的范围，不是判定这种方案不可能。

## 官方与浏览器资料

- [Microsoft：XInput 与 DirectInput](https://learn.microsoft.com/en-us/windows/win32/xinput/xinput-and-directinput)。文档明确：仅使用 XInput 的游戏不会支持传统 DirectInput 设备；XInput 不会识别这些设备。文档关于双扳机合并及震动缺失的说明，针对 Xbox/XUSB 设备通过 DirectInput 的访问，不应扩展成“任何 DirectInput 手柄都不支持力反馈”。
- [Chromium：Windows 标准手柄映射](https://github.com/chromium/chromium/blob/main/device/gamepad/gamepad_standard_mappings_win.cc)。GetGamepadStandardMappingFunction 根据产品名、VID/PID 找已知设备及映射函数，找不到时映射函数为空。这支持“浏览器能读到 HID 不代表自动有 standard 布局”的判断。

## 与本项目故障的关系及实施方向

1. 经典 HID 已读到实时轴变化，应先针对 Windows 调整右摇杆与扳机的 Usage、按钮编号及主机映射，再验证 Steam 与星露谷。当前描述符右摇杆为 Z/Rz，扳机为 Rx/Ry，值得优先检查 Windows / 浏览器轴枚举顺序。网页的原始轴和按钮应逐项实测，不能只看标准 Xbox 标签判断漂移。
2. 按钮位置、扳机和摇杆按下应使用明确的 Windows 配置。BtGamepad 的共用扳机轴、GKME 的按钮式扳机各有取舍，不能为追求网页标签一致而丢掉所需功能。
3. 通用 HID 的原生识别与目标游戏兼容要分别验证。先验证 joy.cpl 的中心、轴、按钮，再配置并验证 Steam Input，最后检查星露谷响应。不能把 XInput 未连接单独作为 HID 失败的判断。
4. Xbox BLE 保留为实验路线。用户提供的 2.1 日志显示配对成功、MTU 517、60 次应用可见读取、响应提交无失败，但没有任何输入 CCCD 写入，输入通知为零。它已不是简单的缺权限或触控不产生报告；现有资料尚不能确定 Windows 服务枚举 / 驱动初始化停止的具体原因。
5. 游戏驱动的震动需要单独建立成功证据：主机确实发送输出报告、手机解码并震动。不能把本地按键触感、GKME 的 WiFi 震动或 ESP32 的成功案例视为 Android 蓝牙震动已经解决。

参考源码缓存于 build/protocol-research/android-survey；仅用于阅读与比较。研究完成后，3.0 采用独立编写的 Windows HID 布局：四条摇杆轴 X/Y/Z/Rx，扳机 Ry/Rz，常见原始按钮索引和 Hat。研究中所述 Xbox BLE 路线已从 App 移除。

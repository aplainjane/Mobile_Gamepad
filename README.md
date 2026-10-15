<p align="center"><img src="docs/images/icon.png" width="112" height="112" alt="Pocket Gamepad icon" /></p>

<h1 align="center">Pocket Gamepad</h1>

<p align="center">让手机换个工作，给电脑当手柄。</p>

<p align="center"><b>简体中文</b> · <a href="README.en.md">English</a></p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-9%2B-AFC6A1?style=flat-square" alt="Android 9+" />
  <img src="https://img.shields.io/badge/PC-Windows-AECFDF?style=flat-square" alt="Windows" />
  <img src="https://img.shields.io/badge/Bluetooth-HID-EFCDA2?style=flat-square" alt="Bluetooth HID" />
</p>

<p align="center"><a href="https://github.com/aplainjane/Mobile_Gamepad/releases">下载 APK</a> · <a href="#开始玩">开始玩</a> · <a href="#几个小问题">常见问题</a> · <a href="https://github.com/aplainjane/Mobile_Gamepad/issues">反馈</a></p>

起初只是想试试：手机有触屏，也有蓝牙，能不能临时客串一下电脑手柄？于是做了 Pocket Gamepad。补上双摇杆和一整套按键，再把界面画得像个小玩具，这件事本身就挺好玩的。

它是一个 Android 蓝牙手柄 App，通过手机系统的 HID 接口连接 Windows。**不需要 Root，也不用安装本项目的电脑接收程序或虚拟手柄驱动。** 配好 Steam Input 后，就可以拿手机试着玩电脑游戏了。

## 长这样

<p align="center"><img src="docs/images/gamepad-landscape.jpg" width="1080" alt="Pocket Gamepad 横屏实机截图：浅蓝背景、奶油色面板和粉彩手柄按键" /></p>

双摇杆、粉彩按键，以及一点点卡通的立体感。上面是当前版本的实机截图。

## 可以做什么

- 双摇杆、ABXY、方向键、肩键和扳机，Select / Start / Home、L3 / R3 也都在。
- 支持多点触控，可以一边推摇杆一边按键；松手后摇杆回中。
- 手机通过蓝牙直接发送手柄输入，不需要账号，也不用连接同一个 Wi-Fi。
- 按键有手机本地触感；误触或输入没松开时，可以点「复位输入」。
- 保持前台时屏幕常亮，适合把手机横过来当一个临时手柄。

触屏当然少了实体按键的手感。不过，想临时试个游戏，或者单纯好奇手机还能做什么时，拿出来玩玩也不错。

## 开始玩

准备一台支持蓝牙 HID Device 的 **Android 9 或更高版本手机**，以及一台带蓝牙的 Windows 电脑。Android 版本满足要求，也需要手机厂商开放这个系统功能。

### 1. 安装和配对

从 [Releases](https://github.com/aplainjane/Mobile_Gamepad/releases) 下载 APK，装到手机上。当前版本为 **3.0.3 测试版**，安装包名含 `debug`。

1. 打开 App，点「连接电脑」，按提示授予蓝牙权限并启动手柄。
2. 点「允许电脑发现手机」，然后在 Windows 的「蓝牙和设备 → 添加设备 → 蓝牙」中选择手机，确认两端验证码。
3. 回到 App，在已配对设备里选择电脑，点「连接」。连接后关闭弹窗，就可以操作了。

### 2. 确认电脑收到了输入

Windows 按 `Win + R`，输入 `joy.cpl`，打开手柄属性。推一下两个摇杆，再试试按钮和方向键。

如果用过修改 HID 描述符之前的旧版本，升级后需要在 Windows 删除旧配对再重新添加。3.0 到 3.0.3 之间的界面更新无需因此重新配对。

### 3. 交给 Steam

打开「Steam → 设置 → 控制器」，找到 Pocket Gamepad 或通用手柄，测试输入并设置按钮布局。在目标游戏的「属性 → 控制器」中启用 Steam Input，然后从 Steam 启动游戏。

Steam 的菜单名称会随版本调整。通用 HID 手柄可能需要先定义布局，连接后没有弹出通知也不等于连接失败。

**玩的时候让 App 保持前台。** 切到后台或退出时，会释放输入并断开连接，返回后需要重新连接。

## 几个小问题

**所有 Steam 游戏都能用吗？**

还不能这样保证。目前主要在 Android 15 手机与 Windows 上测试，已有通过 Steam 配置布局后使用的反馈。具体游戏仍取决于它自己的控制器支持和 Steam Input 配置。非 Steam 游戏可以尝试添加到 Steam 库中启动。

**电脑识别到了，按钮位置却不对？**

先在 Steam 中核对布局。Windows、网页测试器和游戏不一定用相同方式解释通用手柄；网页没有显示 `standard` 映射，也不代表设备没被识别。[输入布局说明](docs/PROTOCOL.md)里列出了原始轴和按键编号。

**Steam 写着「控制器支持未知」？**

这类提示与游戏的支持信息和手柄分类有关。先以控制器设置中的输入测试、以及游戏里的实际操作为准。

**能收到游戏震动吗？扳机能半按吗？**

当前只有手机按键的本地触感，不接收游戏震动。触屏扳机目前也是按下 / 松开两种状态。

**连不上怎么办？**

确认 App 在前台、蓝牙权限已授予，并且没有其他蓝牙键鼠模拟 App 占用 HID 功能。仍然失败时，可以删除两端旧配对再试；部分手机系统可能没有开放 HID Device 接口。

## 自己编译

项目使用 Kotlin、Jetpack Compose 和 Android `BluetoothHidDevice`。用 Android Studio 打开根目录，准备 JDK 21 与 Android SDK 36.1，等待 Gradle 同步完成。

```powershell
.\gradlew.bat :app:assembleDebug
```

APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。需要检查输入报告和代码时，可以运行：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

图标原图保存在 [artwork/launcher-icon.png](artwork/launcher-icon.png)，装好 Pillow 后可运行 `python tools/Generate-LauncherIcons.py` 重新生成图标资源。HID 字段与只读调试脚本见 [协议说明](docs/PROTOCOL.md)。

## 一起折腾

欢迎在 [Issues](https://github.com/aplainjane/Mobile_Gamepad/issues) 里反馈你用的手机、Android 版本，以及连接的游戏和实际表现。机型兼容、按钮布局、界面细节，有具体问题都可以聊；如果能附上复现步骤，会更容易定位。

想自己发布这个项目的话，我也留了一份 [GitHub 与 Release 操作说明](docs/PUBLISHING.md)。

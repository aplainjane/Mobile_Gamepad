# 把 Pocket Gamepad 放到 GitHub

[返回首页](../README.md)

目标仓库：[aplainjane/Mobile_Gamepad](https://github.com/aplainjane/Mobile_Gamepad)。2026-10-15 检查时，它还是空仓库。下面的首次上传步骤按这个状态编写；如果你已经上传过内容，就不用重新初始化。

## 先看一下准备好的东西

| 文件 | 用途 |
| --- | --- |
| `README.md` | 中文首页，GitHub 默认显示它 |
| `README.en.md` | 英文介绍，两页顶部可互相切换 |
| `docs/images/icon.png` | 首页图标 |
| `docs/images/gamepad-landscape.jpg` | 你提供的实机截图 |
| `docs/PROTOCOL.md` | 从旧 README 拆出的输入布局与调试说明 |
| `docs/releases/v3.0.3.md` | 可以直接粘贴到 GitHub 的中英文更新说明 |
| `release/v3.0.3/PocketGamepad-3.0.3-debug.apk` | 本地准备好的安装包，上传为 Release 附件 |
| `release/v3.0.3/SHA256SUMS.txt` | 安装包校验值，可一并上传 |

`release/` 是本地发布材料目录，已加入 `.gitignore`。GitHub 的 Releases 是网页里的独立发布区：把 APK 拖到那里才会成为下载附件，上传源码不会自动上传 APK。

## 第一步：上传源码

在项目目录打开 PowerShell，依次运行：

```powershell
cd F:\AndroidApp\GamePad
git init -b main
git add .
git status --short
```

看一眼待提交文件，应该有源码、README、截图、图标和 Gradle Wrapper。`app/build/`、`build/`、`local.properties`、签名密钥和 APK 都不应该出现；项目的忽略规则已配置好。

确认后继续：

```powershell
git commit -m "Add Pocket Gamepad app and bilingual documentation"
git remote add origin https://github.com/aplainjane/Mobile_Gamepad.git
git push -u origin main
```

首次推送可能打开浏览器让你登录 GitHub，按提示完成即可。不要把 GitHub 账户密码填成 Git 的 HTTPS 密码。

如果提交提示没有作者信息，先设置你想公开显示的名字和提交邮箱，再运行 `git commit`。邮箱也可以用 GitHub「Settings → Emails」提供的 noreply 地址：

```powershell
git config user.name "你的显示名称"
git config user.email "你的提交邮箱"
```

如果提示远程仓库已有提交，先停下并查看远程内容，保留两边的工作后再合并，不要用强制推送覆盖。

上传完成后打开[仓库首页](https://github.com/aplainjane/Mobile_Gamepad)，确认中文 README、图标和截图正常显示，再点 English 检查语言切换。

## 第二步：把 APK 放进 Release

1. 打开 [新建 Release 页面](https://github.com/aplainjane/Mobile_Gamepad/releases/new)。如果还没有推送代码，先完成上一步。
2. 在 **Choose a tag** 中输入 `v3.0.3`，选择创建新标签；目标分支选 `main`。
3. 标题填 `Pocket Gamepad 3.0.3 Preview`。
4. 打开本项目的 [更新说明](releases/v3.0.3.md)，把正文复制到 Release 描述里。
5. 在页面底部附件区拖入本地 `release/v3.0.3/` 里的 **APK 和 SHA256SUMS.txt**。等两个文件都上传完成。
6. 勾选 **Set as a pre-release**，然后点 **Publish release**。也可以先保存草稿，确认页面后再发布。

现在的 APK 是 Android debug 签名的测试包，所以标题和附件都明确写了测试属性。它可以用于当前阶段的体验，不把它标成正式签名发行包。

发布后打开 [Releases](https://github.com/aplainjane/Mobile_Gamepad/releases)，展开 Assets，应该能看到 `.apk`。GitHub 自动生成的 Source code 压缩包只是源码，不是手机安装包。首页下载链接指向 Releases 列表，也能看到这个预发布版本。

如果想核对本地 APK：

```powershell
Get-FileHash .\release\v3.0.3\PocketGamepad-3.0.3-debug.apk -Algorithm SHA256
```

结果应与同目录的 `SHA256SUMS.txt` 一致。

## 以后更新

修改 `app/build.gradle.kts` 的 `versionCode` 和 `versionName`，重新构建 APK，再创建一个新标签和 Release。`versionCode` 需要递增；每次发布保留对应的更新说明。版本号变化时也更新中英文 README 中的下载版本说明。

等准备正式发行时，用 Android Studio 的「Build → Generate Signed App Bundle or APK → APK」配置自己的长期签名密钥并生成 release APK。密钥和密码不要提交进仓库。正式签名与当前 debug 签名不同，已安装测试包的手机通常需要先卸载测试包才能安装正式包；后续正式版保持使用同一个签名密钥。

仓库目前还没有许可证。如果希望别人明确知道能否修改、分发或用于商业用途，发布前由你选定许可证并添加 `LICENSE`；这里没有替你默认决定。

## 想补更多截图

把图片放到 `docs/images/`，用简短英文文件名，再分别更新两份 README 的截图区域即可。现在只使用你提供的这张实机截图，没有把示意图当作额外实测截图。

本次排版参考了 [LocalSend](https://github.com/localsend/localsend) 的语言切换、截图组织方式，以及 [scrcpy](https://github.com/Genymobile/scrcpy) 开头直接介绍用途和下载的写法。项目介绍为 Pocket Gamepad 单独撰写，没有照搬它们的介绍或功能声明。

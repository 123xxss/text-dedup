# 文字去重

一个完全离线的 Android 文字去重工具。自己的图标、独立安装，不依赖 DSH，也不申请网络权限。

按行、按段落或按分隔符去重；支持忽略大小写、空格、空行、标点、零宽字符；支持保留第一次/最后一次出现、拼音排序、长度排序；实时统计，复制，导出 TXT。

<p align="center">
  <img src="docs/icon.svg" width="128" height="128" alt="文字去重图标">
</p>

## 下载安装

- [下载 APK](dist/text-dedup-1.0.1.apk)（Android 10+，约 29 KB）
- 也可以在仓库的 **Releases** 页面下载构建产物

安装步骤：

1. 下载 `text-dedup-1.0.1.apk`。
2. 点开下载完成通知，或到「文件 → 下载」里找到它。
3. 允许浏览器安装未知应用，然后点「安装」。
4. 桌面会出现「文字去重」图标，打开即可使用。

> APK 使用本地调试证书签名。如果你想持续发布自己的版本，请保留第一次构建生成的 `android/signing.keystore`，后续更新要用同一把钥匙。

## 功能

- **三种模式**：按行、按段落（空行分段）、按分隔符（逗号、顿号、分号、竖线、空格、自定义）
- **比较规则**：忽略大小写、首尾空格、连续空白、空行/空项、标点符号、零宽字符，Unicode NFC 规范化
- **保留策略**：保留第一次出现，或保留最后一次出现
- **排序**：保持原顺序、拼音/字母 A→Z、长度从短到长、从长到短
- **结果处理**：实时统计、复制到剪贴板、保存到「下载/文字去重」、替换原文、撤销
- **体验**：深色模式、自动记住设置和上次文本、移动端优先布局
- **隐私**：纯本地处理，不联网，不需要任何权限

## 网页版

`index.html` 是一个零依赖的单文件网页应用，可以直接用浏览器打开，也可以部署到 GitHub Pages 作为在线版。功能和 APK 内嵌版本一致。

## 构建 APK

依赖：

- JDK 17+
- Android SDK Platform 34
- Android Build Tools 34（`aapt2`、`aapt`、`zipalign`、`apksigner`、`d8`）
- Python 3
- 在 ARM Linux 上构建时，可以使用发行版自带的 `aapt2` / `zipalign`

```sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
export BUILD_TOOLS_DIR="$ANDROID_SDK_ROOT/build-tools/34.0.0"
export ANDROID_JAR="$ANDROID_SDK_ROOT/platforms/android-34/android.jar"
export AAPT2="$BUILD_TOOLS_DIR/aapt2"
export ZIPALIGN="$BUILD_TOOLS_DIR/zipalign"
export AAPT_BIN="$BUILD_TOOLS_DIR/aapt"

./android/build.sh
```

产物：

- `android/text-dedup.apk`
- `dist/text-dedup-<version>.apk`

第一次构建会自动生成 `android/signing.keystore`（密码 `android`，别名 `dsh`）。这个文件不会提交到仓库，请自行备份；正式发布请换成自己的签名。

开发时修改根目录的 `index.html` 即可，构建脚本会自动复制到 APK 的 `assets/`。

## 自动构建（可选）

把 [`docs/github-actions-build.yml`](docs/github-actions-build.yml) 复制到 `.github/workflows/build.yml`，之后 push / PR 会自动构建 APK 并上传 artifact；推送 `v*` 标签时会自动创建 GitHub Release。

```sh
mkdir -p .github/workflows
cp docs/github-actions-build.yml .github/workflows/build.yml
git add .github/workflows/build.yml
git commit -m "ci: build APK"
git tag v1.0.1
git push origin main --tags
```

## 项目结构

```
.
├── index.html                 # 单文件网页版，也是 APK 内嵌界面
├── install.html               # 手机安装引导页
├── dist/text-dedup-1.0.1.apk  # 已签名安装包
├── docs/icon.svg              # 应用图标
├── docs/github-actions-build.yml  # 可选的 CI 构建模板
└── android/
    ├── AndroidManifest.xml
    ├── build.sh               # 无 Gradle 构建脚本
    ├── res/                   # 图标、主题、字符串
    └── src/com/dsh/textdedup/
        └── MainActivity.java  # WebView 外壳 + 剪贴板/保存桥
```

## 技术说明

- Android 原生 WebView 外壳，`file:///android_asset/index.html` 加载离线页面。
- 通过 `@JavascriptInterface` 暴露 `copy`、`readClipboard`、`saveText`、`setTheme`，让网页版在 App 内也能可靠地读写剪贴板和导出文件。
- 保存 TXT 使用 MediaStore 写入 `Download/文字去重`，不需要存储权限。
- `targetSdk 34`，避免 Android 15 强制边到边导致内容被状态栏遮挡。
- 没有网络权限，页面也没有任何外部字体、脚本或图片。

## License

[MIT](LICENSE)

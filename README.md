# BASS ENGINE · Global DSP

> **由 DeepSeek 开发** · Developed by **DeepSeek**
>
> 本项目的守护进程、模块脚本、控制 App 与文档均由 DeepSeek（AI）编写，
> 由 BASS ENGINE 项目维护者负责测试、调音与发布。

Android 全局音质引擎。通过 KernelSU 根模块在开机时拉起一个 `app_process`
守护进程，把系统 **DynamicsProcessing** 挂到 audio session 0，让所有 App
（音乐、视频、游戏、系统提示音）共用同一条 DSP 链。

**不注入进程、不加 LSPosed 作用域、不改内核、不写分区、不动 `/vendor`。**
模块只安装在 `/data/adb/modules`。

## 特性

- 10 段图形 EQ（20 Hz–16 kHz，±15 dB），实时频响曲线
- 低频引擎：低音增强 / 虚拟低频（EQ 近似）/ 自适应 / 超低频冲击 / 低音炮模式
- 环绕：wide / cinema / near + air / vocal / focus，40–70% 强度
- 8 套预设 + 自定义预设，JSON / 分享码导入导出
- 设备 Profile（扬声器 / 有线 / 蓝牙各自记忆）
- 当前输出设备识别：显示扬声器 / 有线耳机 / 蓝牙及设备名
- 控制中心磁贴 ×3（总开关 / 预设循环 / 低频开关）
- 安全限幅：输入静态 headroom + 限幅器，防削波
- 开机自启、配置持久化、守护进程健康检查、日志轮转

## 工作原理

```
控制 App  ──写──▶  /sdcard/Android/data/com.bassengine.control/files/global_config.json
                          │
root 守护进程 (GlobalDaemon) 每 400 ms 看一次文件（仅 mtime/size 变化才读）
                          │
                          ▼
        DynamicsProcessing(0, 0)  ←─ attach 到 session 0
        · PreEQ 10 段（EQ + 低频 + 环绕映射）
        · InputGain（预增益 − 静态 headroom）
        · MBC 2 段（低频压缩 / 高频控制）
        · Limiter
                          │
                          ▼
                 所有 App 的混音输出
```

守护进程把状态写回 `global_status.json`（`attached / effectId / pid / error`，
2 s 心跳），App 轮询后显示「全局 DSP 已连接 · 效果 #ID」。

开机时如果 App 还没启动，守护进程会先套用
`/data/adb/bassengine/last_config.json`，所以重启后不用先开 App 也有声音处理。

## 要求

- 已 root 的 Android 设备，使用 **KernelSU**（Magisk 理论可用，未测试）
- Android 12+（DynamicsProcessing 需 API 28+；本模块在 Android 17 上验证）
- 控制 App：Android 8.0+（minSdk 26）

## 安装

1. 下载 `dist/BASS-ENGINE-Global-KernelSU-v0.2.0.zip`，在 KernelSU 管理器里
   从本地安装，然后重启。
2. 安装 `dist/BASS-ENGINE-control-v7.2.7-debug.apk`。
3. 打开 App，首页应显示「全局 DSP 已连接 · 效果 #ID」。

命令行安装（root）：

```sh
su -c 'sh install.sh'
```

## 卸载

```sh
su -c 'sh /data/adb/modules/bassengine_global/uninstall.sh'
```

然后在 KernelSU 里删除 `bassengine_global` 模块，App 可直接卸载。

## 构建守护进程

需要 JDK 17、Android SDK（`platforms/android-34/android.jar`、`build-tools/*/d8`）。

```sh
cd daemon
SDK=$ANDROID_HOME ./build.sh
```

脚本会 `javac` 编译 `GlobalDaemon.java`，用 `d8` 生成 `classes.dex`，
再打包成 `module/global-daemon.jar`。

## 已知限制

- DynamicsProcessing 只有 EQ / 多段压缩 / 限幅：**虚拟低频与立体声宽度只能用
  EQ 近似**，没有真正的谐波低频和 mid/side 扩展。
- `adaptive` 依赖实时音量，守护进程拿不到音量值，目前是固定 +1 dB 近似。
- 全局生效意味着所有声音（包括系统提示音）都会过这条链。
- 与厂商音效（如 OplusAudioX）叠加，听感对比时建议先关掉其他音效模块。

## 日志与排查

- 守护进程日志：`/data/adb/bassengine/daemon.log`
- 状态文件：`/sdcard/Android/data/com.bassengine.control/files/global_status.json`
- 体检脚本：`sh verify.sh`

## 许可

MIT，见 [LICENSE](LICENSE)。

## 致谢

- 开发：[DeepSeek](https://www.deepseek.com/)
- AndroidX / Jetpack Compose（Apache-2.0）
- Android 平台 DynamicsProcessing 效果器

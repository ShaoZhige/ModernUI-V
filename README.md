# ModernUI-V

让 Fabric 1.21.1 的 Modern UI 能在 VulkanMod 的环境下运行。

## 这是什么

**Modern UI - V**（现代化UI V版）是 [Modern UI](https://github.com/BloCamLimb/ModernUI-MC) 的**非官方分支**，
原作者 [BloCamLimb](https://github.com/BloCamLimb)。

上游 Modern UI 依赖的图形渲染是 OpenGL，而 VulkanMod 会把它替换掉。本分支
把 UI 渲染接到 Vulkan 上，以求兼容安装了VulkanMod下的环境，并修复了一些问题。
除此之外与上游几乎一致。

> ⚠️ **这不是官方构建。** 请**不要**向上游项目反馈问题、崩溃或请求功能 —— 请提交至本仓库。

## 从源码构建

需要 **JDK 21**。构建缓存在 `gradle-home/`（内含 Gradle 8.8
发行版与全部依赖），因此可以完全离线构建：

```bat
cd /d "<项目路径>"
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "GRADLE_USER_HOME=<项目路径>\gradle-home"
gradlew.bat remapJar --console=plain --offline
```

产物：`build/libs/ModernUI-V-Fabric-1.21.1-3.13.0.1-universal.jar`


## 目录结构

本仓库是上游 `ModernUI-MC` 的 Fabric 1.21.1 分支的**扁平化快照** —— 上游的 `fabric/`
模块被提到了根工程，`common/` 保持为唯一子项目：


## 许可证

本项目以 **GNU Lesser General Public License v3.0 or later** 发布，与上游保持同一许可证，全文见 [LICENSE](LICENSE)。

- 原始 Modern UI 由 BloCamLimb 开发，以 **LGPL-3.0-or-later** 授权，版权归 BloCamLimb 所有。
  各源文件头部的原始版权与许可声明均予保留，未作改动。
- 本分支作为其**修改版本**，依 LGPL-3.0 同样以 **LGPL-3.0-or-later** 发布，不改变许可证。
- 本分支的修改由 **Shao_Zhige** 完成。
- 内置 HarmonyOS Sans SC 字体的许可见 `assets/modernui/harmonyossans-license.txt`（位于构建产物中）。

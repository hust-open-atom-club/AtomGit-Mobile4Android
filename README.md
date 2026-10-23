# AtomGit Mobile

[![CI Build](https://github.com/hust-open-atom-club/AtomGit-Mobile4Android/actions/workflows/ci.yml/badge.svg)](https://github.com/hust-open-atom-club/AtomGit-Mobile4Android/actions/workflows/ci.yml)

AtomGit 的 Android 客户端，基于 Kotlin 和 Jetpack Compose 开发，数据来自 AtomGit 开放接口，接口文档见 [docs.atomgit.com/docs/apis](https://docs.atomgit.com/docs/apis/)。

包含「动态」「待办」「通知」「我的」四个入口，支持对 Issue 和 PR 评论、批准等功能。

## 环境

- JDK 21，Gradle 9.3.1
- Android Studio 或命令行构建
- Android SDK：`compileSdk 36` / `minSdk 24` / `targetSdk 36`
- 主要依赖：Kotlin、Jetpack Compose、Material 3、Navigation 3、Hilt、Retrofit/OkHttp、Room、DataStore、Coil

```bash
./gradlew :app:assembleDebug        # 构建 Debug APK
./gradlew :app:testDebugUnitTest    # 单元测试
./gradlew :app:lintDebug            # 静态检查
```

## 规范

开发与提交规范见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 协议

[Apache License 2.0](LICENSE)

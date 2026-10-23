# 贡献规范

感谢关注 AtomGit Mobile。本项目处于早期开发阶段，欢迎通过 Issue 反馈问题与建议。

## 先提 Issue

动手前先开 Issue 说明要解决的问题或需求，确认方向后再切分支，避免重复劳动。

## 开发环境

- JDK 21，Android Studio 或命令行构建
- 拉取代码后先确认构建与单元测试能跑通，命令见 README

## 分支与提交

- 从 `main` 切分支，命名 `<type>/<短描述>`，例如 `feat/hilt-di`、`fix/token-expiry`
- 提交信息使用 `type: 描述` 格式，例如 `feat: add todo list pagination`
- 一个 PR 只做一件事，保持小而聚焦，方便审查

## PR

- 描述写清楚：做了什么、为什么做、如何验证（构建、单测）
- 关联 Issue 时写 `Closes #编号`
- 提交前确认 CI 通过、测试通过、没有密钥泄露

## 代码风格

- 遵循 Kotlin 官方代码风格（`gradle.properties` 已设置 `kotlin.code.style=official`）
- Compose 保持单向数据流（UDF）与状态提升

## 安全

- 不提交 Token、密钥、`local.properties`、keystore 等敏感信息
- 不在 Issue / PR 中粘贴凭据或私密接口响应

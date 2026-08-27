# 构建状态与边界

## 当前结论

源码与助手配置已经静态收口。本地当前执行环境不能生成可信 APK，也不能代替小米 / 澎湃OS 真机验收；功能分支附带独立 GitHub Actions 工作流用于构建并验证内部测试 APK。

## GitHub CI

- `.github/workflows/zhixing-internal-build.yml` 在 `feat/team-custom-assistant` 推送时自动运行，也支持手动触发。
- 工作流复用上游完整 Android 依赖准备链，固定执行 `:app:assembleInternal`。
- 三份外部依赖归档先校验固定 SHA-256，再进入依赖准备步骤。
- 构建后校验包名 `com.zhixing.ai`、APK 签名和 SHA-256，并分别上传 APK 与诊断日志。
- 未配置团队 release keystore 时，产物使用 CI 运行器生成的 debug 签名，只适合首轮设备验证。

## 当前环境缺项

- 只有 JRE 17，没有项目构建所需的完整 JDK 21 / `javac`
- 没有 Android SDK 34/36、Build Tools 34/35、NDK 25.1.8937393、CMake 3.22.1 和 Ninja
- Gradle与 Maven 依赖缓存为空，当前网络策略不能补齐全部依赖
- `terminal` / `hotbuild` 子模块未初始化
- `app/libs`、`jniLibs`、`assets/subpack` 缺少上游构建所需的 AAR、原生库和子包资产
- 缺少 ripgrep 原生库、Rust 产物、STT 模型和 Web Chat 构建资产
- 没有团队 release keystore

## 已执行的本地验证

- Git diff whitespace 检查
- Android XML 解析与资源重复键检查
- bundle / 角色卡 JSON 解析和角色 ID 唯一性检查
- Bash、Python、修改过的 JavaScript 语法检查
- 助手包及内部 Skill ZIP 完整性检查
- 旧运行目录、旧应用专属 action、错误短组件名和模板占位符静态扫描

这些检查不能替代 Kotlin/Java 编译、Android Lint、单元测试、Instrumentation、APK 签名验证与真机行为测试。

## 建议构建路径

将覆盖包应用到用户自己的 GitHub fork，然后使用与上游等价、已配置完整依赖的 CI。签名前先生成一次 dev-signed 内部 APK 做功能验证；验收通过后创建并离线保管团队 keystore，重新构建稳定签名 APK。不要用 debug 签名包作为后续团队升级链的起点。

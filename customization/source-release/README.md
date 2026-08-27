# 知行 AI 源码覆盖包

本覆盖包把 Operit 指定基础提交改造成“知行 AI”团队内部版源码。它不是 APK，也不包含 API Key、签名密钥、Android SDK/NDK 或上游未纳入 Git 的预构建依赖。

## 生成覆盖包

功能分支提交后，在仓库根目录执行：

```bash
customization/source-release/scripts/package_source_overlay.sh
```

默认模式比较 `BASE_COMMIT..HEAD`，并且所有覆盖文件都直接读取当前 `HEAD` 的 Git 对象；未提交修改和未跟踪文件不会混入。包内 `SOURCE.txt` 会记录来源模式、基础提交和目标提交。

提交前需要预览当前工作树时，必须显式执行：

```bash
customization/source-release/scripts/package_source_overlay.sh --working-tree
```

工作树模式以 `BASE_COMMIT` 为基线，包含已跟踪修改及未被 `.gitignore` 排除的未跟踪文件。两种模式都排除 `customization/**/dist/`，并分别在 `files/`、`DELETE_FILES.txt` 中记录新增/修改内容与删除路径。生成结果属于可重建产物，不应提交到 Git。

## 已完成

- 独立 applicationId：`com.zhixing.ai`
- 中文名称“知行 AI”，其他 locale 使用“Zhixing AI”
- 独立图标、公共目录、广播 action、快捷方式、端口和更新策略
- DeepSeek 与澎湃OS团队助手包
- 普通权限 + 无障碍基线，Shizuku 可选，Root 不要求且不默认初始化
- 外部聊天和工作流入口共用受控 Token，并只接受显式 Receiver component
- 内部构建变体；debug/clone 使用各自的 applicationId、目录和端口

## 应用覆盖包

1. 克隆上游仓库，并检出 `BASE_COMMIT` 中的精确提交。
2. 确认工作树干净；不要把覆盖包复制到含有未保存修改的仓库。
3. 把 `files/` 目录内容原样复制到仓库根目录。
4. 按 `DELETE_FILES.txt` 删除被替代的文件。
5. 用 `FILES.sha256` 校验复制后的覆盖文件。
6. 查看 `SOURCE.txt`，确认来源模式和基础、目标提交符合预期。
7. 阅读 `BUILD_STATUS.md`，补齐上游构建依赖和团队签名。
8. 执行 `./gradlew :app:assembleInternal`。

内部变体没有配置团队 release key 时会使用 debug 签名，并输出 `zhixing-ai-internal-dev-signed.apk`。这个包只适合临时设备验证；团队持续升级必须固定同一 applicationId 和同一团队签名。

## 首轮真机验收

- 与官方 Operit 同时安装，确认名称、图标、FileProvider authority、快捷方式和公共目录互不串用。
- 配置 DeepSeek 后先做纯对话，再按 `customization/team-assistant/HYPEROS_SETUP.md` 开启无障碍。
- 不授予 Root；需要 ADB 级能力时再单独验证 Shizuku。
- 验证外部聊天 / 工作流缺少或写错 Token 时被拒绝，隐式 action 广播不能触发入口。
- 验证 debug、clone 和 internal 同时运行时，工作区及外部 HTTP 端口不冲突。
- 验证上游更新检查保持关闭。

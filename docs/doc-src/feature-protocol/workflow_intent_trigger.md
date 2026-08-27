# Workflow Intent 触发（自定义 Action）

本文档说明如何通过 **Intent 广播**触发知行 AI 的工作流（Workflow）。

当前实现支持：

- **每个工作流的 Intent Trigger 可以配置自己的 `action`**
- 外部 App 通过发送带 `auth_token` 的**显式组件广播**触发匹配的工作流

对应代码：

- 接收入口：`app/src/main/java/com/ai/assistance/operit/integrations/tasker/WorkflowTaskerReceiver.kt`
- 匹配与触发：`WorkflowRepository.triggerWorkflowsByIntentEvent(intent)`

---

## 1. 核心概念

### 1.1 TriggerNode(intent)

当某个工作流的触发器节点满足：

- `triggerType == "intent"`
- `triggerConfig["action"] == intent.action`

则该工作流会被触发执行。

### 1.2 为什么必须使用“显式组件广播”

Android 对隐式广播有各种限制（尤其是后台、Android 8+ 等）。

为了确保广播稳定投递给知行 AI，并避免 Bearer Token 被注册同名 action 的其他 App 截获，必须使用：

- **显式广播**：指定 `component`（包名 + Receiver 类名）

这样即使 action 是自定义的，也能确保发给知行 AI 的 `WorkflowTaskerReceiver`。该 Receiver 不注册隐式 `intent-filter`，所以只写 action 的隐式广播不会触发工作流。

---

## 2. Receiver / Component 信息

- **Receiver 类**：`com.ai.assistance.operit.integrations.tasker.WorkflowTaskerReceiver`
- **默认 applicationId**：`com.zhixing.ai`
- **Component**：`com.zhixing.ai/com.ai.assistance.operit.integrations.tasker.WorkflowTaskerReceiver`

applicationId 可随构建变体调整，但 Component 右侧必须保留完整 Receiver 类名。以下命令统一使用：

```bash
export APP_PACKAGE="${OPERIT_APP_PACKAGE:-com.zhixing.ai}"
export WORKFLOW_RECEIVER="${APP_PACKAGE}/com.ai.assistance.operit.integrations.tasker.WorkflowTaskerReceiver"
export ZHIXING_AUTH_TOKEN="<外部 HTTP API 设置页显示的 Bearer Token>"
```

---

## 3. 配置工作流的 action

在工作流编辑器中，将触发器设置为：

- **类型**：Intent
- **action**：填写你希望外部触发的 action，例如：
  - `com.example.myapp.TRIGGER_OPERIT_WORKFLOW_A`

注意：

- action 只是一个字符串，用于匹配。
- 触发时 intent 的 extras 会作为 TriggerNode 的输出 JSON（字符串）提供给下游节点（可用 ExtractNode(JSON) 提取字段）。

---

## 4. adb 触发示例

### 4.1 自定义 action：显式组件广播

```bash
adb shell am broadcast \
  -n "$WORKFLOW_RECEIVER" \
  -a com.example.myapp.TRIGGER_OPERIT_WORKFLOW_A \
  --es auth_token "$ZHIXING_AUTH_TOKEN" \
  --es message "hello from adb" \
  --es request_id "req-1001"
```

- `-n` 指定 component，确保发给知行 AI。
- `-a` 为你在工作流 Trigger 里配置的 action。
- `--es auth_token` 是必填鉴权字段，值与外部 HTTP API 使用同一个 Bearer Token。
- `--es/--ez/--ei/...` 为 extras，会被工作流 TriggerNode 收集并输出。

### 4.2 使用内置默认 action

如果你的工作流 Trigger 里 `action` 配置的是默认值：

- `${APP_PACKAGE}.TRIGGER_WORKFLOW`（默认是 `com.zhixing.ai.TRIGGER_WORKFLOW`）

那么可以使用：

```bash
adb shell am broadcast \
  -n "$WORKFLOW_RECEIVER" \
  -a "${APP_PACKAGE}.TRIGGER_WORKFLOW" \
  --es auth_token "$ZHIXING_AUTH_TOKEN" \
  --es message "hello" \
  --es request_id "req-1002"
```

---

## 5. WORKFLOW_RESULT：工作流回传广播（示范模板默认值）

在内置的“Intent 触发 + 发送消息 + 回传广播”示范模板中，会使用工具节点 `send_broadcast` 回传结果：

- **action**：`${APP_PACKAGE}.WORKFLOW_RESULT`（默认是 `com.zhixing.ai.WORKFLOW_RESULT`）
- **extra_key**：`result`
- **extra_value**：来自 `send_message_to_ai` 节点的输出（字符串）

你也可以在工作流里自定义：

- 回传 action（例如回传给你自己的 App）
- extra 的 key/value（例如同时回传 `request_id`、`chat_id` 等）

---

## 6. 如何接收 WORKFLOW_RESULT

`adb` 本身无法直接作为“广播接收端”来打印收到的广播内容（它只能发送广播）。要接收回传广播，推荐两种方式：

### 6.1 用 Tasker 接收（最方便）

- 在 Tasker 创建 Profile：Event -> System -> Intent Received
- Action 填：`com.zhixing.ai.WORKFLOW_RESULT`（若构建使用了其他 applicationId，请同步替换前缀）
- 在 Task 中读取变量（通常可直接用 `%result` 或从 extras 映射中取）

### 6.2 写一个最小接收 App / Receiver（用于调试）

在你的测试 App 中注册一个 `BroadcastReceiver` 监听 `com.zhixing.ai.WORKFLOW_RESULT`，在 `onReceive()` 里读取：

- `intent.getStringExtra("result")`

然后你可以用 `adb logcat` 看接收端打印的内容。

---

## 7. 工作流内如何读取 extras（Trigger JSON + Extract(JSON)）

TriggerNode 会把收到的 extras 转为 JSON 字符串作为输出，例如：

- 收到 extras：
  - `message=hello`
  - `request_id=req-1001`

TriggerNode 输出（示意）：

```json
{"message":"hello","request_id":"req-1001"}
```

下游可以使用 `ExtractNode(mode=JSON)`：

- `source = NodeReference(triggerNodeId)`
- `expression = "message"`

从而得到 `hello`。

---

## 8. 注意事项

- 该 Receiver 为 `exported=true`，但每次请求都必须携带正确的 `auth_token`；缺失或错误的令牌会被拒绝。
- 请求必须通过 component 显式投递；不要使用只含 action 的隐式广播，也不要在隐式 Intent 中携带 Token。
- `auth_token` 与外部 HTTP API 的 Bearer Token 共用，请通过受控渠道分发，不要写入仓库、工作流模板或日志。
- Receiver 和定时 Worker 都可以在没有 Activity 的情况下冷启动应用进程。工作流运行时所需的全局偏好会在 `Application.onCreate()` 阶段就绪，因此无需先打开主界面。

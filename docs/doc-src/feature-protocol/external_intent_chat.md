# External Intent API: `EXTERNAL_CHAT`

本文档描述一个**独立于工作流系统**的外部交互接口：外部应用通过发送显式组件广播 Intent（`com.zhixing.ai.EXTERNAL_CHAT`）向知行 AI 发起一次“发送消息给 AI”的请求，并通过按包名定向的广播接收执行结果。请求必须指定知行 AI 的 Receiver 组件；不要把 Bearer Token 放进隐式广播。

如果你希望通过局域网 HTTP 调用，而不是广播 Intent，请查看：

- `docs/doc-src/feature-protocol/external_http_chat.md`

HTTP 接口与本文的 Intent 接口语义一致，仅入口不同。

该接口的实现位于：

- `app/src/main/java/com/ai/assistance/operit/integrations/intent/ExternalChatReceiver.kt`

Manifest 注册：

- `AndroidManifest.xml` -> `.integrations.intent.ExternalChatReceiver`

---

## 1. Action

- **请求 Action**：`com.zhixing.ai.EXTERNAL_CHAT`
- **默认回传 Action**：`com.zhixing.ai.EXTERNAL_CHAT_RESULT`

你也可以通过 `reply_action` 指定自定义回传 action。

---

## 2. 请求参数（Intent extras）

| extra key | 类型 | 必填 | 默认值 | 说明 |
|---|---:|:---:|---:|---|
| `auth_token` | `String` | 是 | - | 与“设置 → 外部 HTTP 聊天”页面相同的 Bearer Token；HTTP、Intent 聊天和 Intent 工作流共用此令牌 |
| `message` | `String` | 是 | - | 要发送给 AI 的文本 |
| `request_id` | `String` | 否 | - | 业务侧请求 ID（原样回传，便于关联请求/响应） |
| `group` | `String` | 否 | - | 当 `create_new_chat=true` 时，用于新对话分组 |
| `create_new_chat` | `Boolean` | 否 | `false` | 是否强制创建新对话再发送消息 |
| `chat_id` | `String` | 否 | - | 指定发送到某个对话（仅当 `create_new_chat=false` 时生效） |
| `create_if_none` | `Boolean` | 否 | `true` | 当未指定 `chat_id` 且当前没有对话时，是否允许自动创建对话（设为 `false` 则会报错返回） |
| `show_floating` | `Boolean` | 否 | `false` | 是否启动/显示悬浮窗服务（会触发绑定并启动 `FloatingChatService`） |
| `return_tool_status` | `Boolean` | 否 | `true` | 是否返回工具状态相关内容。设为 `false` 时，会从回传的 `ai_response` 中移除 `<tool*>`、`<tool_result*>`、`<status>` 这类辅助内容 |
| `initial_mode` | `String` | 否 | - | 当 `show_floating=true` 时指定浮窗初始模式，可用值：`WINDOW`、`BALL`、`VOICE_BALL`、`FULLSCREEN`、`RESULT_DISPLAY`、`SCREEN_OCR` |
| `auto_exit_after_ms` | `Long` | 否 | `-1` | 当 `show_floating=true` 时：自动退出/关闭悬浮窗的超时（毫秒） |
| `stop_after` | `Boolean` | 否 | `false` | 本次请求结束后是否停止对话服务（会 stop `FloatingChatService`） |
| `reply_action` | `String` | 否 | `com.zhixing.ai.EXTERNAL_CHAT_RESULT` | 指定回传广播 action |
| `reply_package` | `String` | 外部调用必填 | 知行 AI 自身 | 回传广播始终按包名显式投递；外部调用方必须写自己的包名，否则结果只会发回知行 AI 自身 |

---

## 3. 回传参数（Intent extras）

知行 AI 在处理完成后会发送一条广播（action 为 `reply_action` 或默认 action），携带如下 extras：

| extra key | 类型 | 说明 |
|---|---:|---|
| `request_id` | `String` | 若请求里携带，则原样回传 |
| `success` | `Boolean` | 是否成功 |
| `chat_id` | `String` | 发生交互的对话 ID（如果可用） |
| `ai_response` | `String` | AI 回复文本（如果可用） |
| `error` | `String` | 错误信息（失败时） |

---

## 4. 行为与优先级规则（简化版）

- 若 `message` 为空，直接失败回传。
- 若 `show_floating=true`：
  - 会尝试启动/连接 `FloatingChatService`。
  - 可通过 `initial_mode` 指定初始界面。
  - 若未传 `initial_mode`，则沿用当前/上次保存的浮窗模式；首次默认 `WINDOW`。
  - 可通过 `auto_exit_after_ms` 设置自动退出。
- 若 `return_tool_status=false`：
  - 回传中的 `ai_response` 会移除工具状态相关 XML，减少消息体积。
- 若 `create_new_chat=true`：
  - 会新建对话（可选 `group`）。
  - 发送消息时不会使用 `chat_id`（忽略 `chat_id`）。
- 若 `create_new_chat=false` 且 `chat_id` 不为空：
  - 会先切换到指定对话并发送。
- 若 `create_new_chat=false` 且 `chat_id` 为空：
  - 默认 `create_if_none=true`：允许在没有当前对话时自动创建。
  - `create_if_none=false`：如果当前没有对话则失败回传。
- 若 `stop_after=true`：处理完会停止 `FloatingChatService`。

---

## 5. adb 示例

先在“设置 → 外部 HTTP 聊天”中启用一次服务以生成 Token，再把它写入当前终端变量：

```bash
export APP_PACKAGE="${OPERIT_APP_PACKAGE:-com.zhixing.ai}"
export EXTERNAL_CHAT_RECEIVER="${APP_PACKAGE}/com.ai.assistance.operit.integrations.intent.ExternalChatReceiver"
export ZHIXING_AUTH_TOKEN='从知行 AI 设置页复制的 Token'
```

知行 AI 的 Receiver 没有隐式 `intent-filter`。ADB 必须使用下列示例中的 `-n`；Android 调用方必须使用 `Intent.setComponent(ComponentName(...))`。这既保证投递到正确变体，也避免同名 action 的其他 App 截获 Token。

### 5.1 创建新对话 + 分组 + 发送消息 + 显示悬浮窗

```bash
adb shell am broadcast \
  -n "$EXTERNAL_CHAT_RECEIVER" \
  -a com.zhixing.ai.EXTERNAL_CHAT \
  --es auth_token "$ZHIXING_AUTH_TOKEN" \
  --es request_id "req-001" \
  --es message "你好，帮我总结一下这段话" \
  --es group "workflow" \
  --ez create_new_chat true \
  --ez show_floating true \
  --ez return_tool_status false \
  --es initial_mode "WINDOW" \
  --el auto_exit_after_ms 10000
```

### 5.2 发到指定 chat_id（不新建）

```bash
adb shell am broadcast \
  -n "$EXTERNAL_CHAT_RECEIVER" \
  -a com.zhixing.ai.EXTERNAL_CHAT \
  --es auth_token "$ZHIXING_AUTH_TOKEN" \
  --es request_id "req-002" \
  --es chat_id "YOUR_CHAT_ID" \
  --es message "继续刚才的话题"
```

### 5.3 不允许自动创建对话（create_if_none=false）

如果当前没有对话且不允许创建，会返回失败：

```bash
adb shell am broadcast \
  -n "$EXTERNAL_CHAT_RECEIVER" \
  -a com.zhixing.ai.EXTERNAL_CHAT \
  --es auth_token "$ZHIXING_AUTH_TOKEN" \
  --es request_id "req-003" \
  --es message "测试" \
  --ez create_if_none false
```

---

## 6. 发送完毕后的回传如何接收

知行 AI 会在处理完成后发送广播回传：

- 默认 action：`com.zhixing.ai.EXTERNAL_CHAT_RESULT`
- 或者你在请求中指定的 `reply_action`

注意：

- `adb` 本身只能用来“发送广播”，不能直接作为“广播接收端”打印收到的广播。

如果你希望**只让自己的 App 收到回传**，请在请求里设置：

- `reply_package = 你的包名`

这样知行 AI 在回传时会对广播设置 `intent.setPackage(reply_package)`。

### 6.1 写一个最小接收 App / Receiver（用于调试/集成）

下面给出一个“最小可用”的接收端示例（Kotlin）。你只需要：

- 注册一个 `BroadcastReceiver`
- 监听 `EXTERNAL_CHAT_RESULT`（或你自定义的 `reply_action`）
- 在 `onReceive()` 中读取 extras

#### 6.1.1 Receiver 代码示例

```kotlin
class ExternalChatResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.zhixing.ai.EXTERNAL_CHAT_RESULT") return

        val requestId = intent.getStringExtra("request_id")
        val success = intent.getBooleanExtra("success", false)
        val chatId = intent.getStringExtra("chat_id")
        val aiResponse = intent.getStringExtra("ai_response")
        val error = intent.getStringExtra("error")

        Log.d(
            "ExternalChatResult",
            "request_id=$requestId success=$success chat_id=$chatId ai_response=$aiResponse error=$error"
        )
    }
}
```

#### 6.1.2 Manifest 注册示例

在你的 App 的 `AndroidManifest.xml` 中注册（Android 12+ 需要显式声明 exported）：

```xml
<receiver
    android:name=".ExternalChatResultReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="com.zhixing.ai.EXTERNAL_CHAT_RESULT" />
    </intent-filter>
</receiver>
```

#### 6.1.3 触发请求并验证回传（adb + logcat）

1) 先启动你的接收端 App（确保进程存在更容易观察 log）：

2) 发送请求（建议指定 `reply_package`，避免回传被其他 App 接收）：

```bash
adb shell am broadcast \
  -n "$EXTERNAL_CHAT_RECEIVER" \
  -a com.zhixing.ai.EXTERNAL_CHAT \
  --es auth_token "$ZHIXING_AUTH_TOKEN" \
  --es request_id "req-101" \
  --es message "hello" \
  --es reply_package "YOUR.APP.PACKAGE"
```

3) 观察你的 App 日志：

```bash
adb logcat | findstr ExternalChatResult
```

如果 `success=true`，则 `ai_response` 通常会包含 AI 回复文本；失败时可查看 `error`。

### 6.2 回传字段快速说明

回传广播 extras：

- `request_id: String?`
- `success: Boolean`
- `chat_id: String?`
- `ai_response: String?`
- `error: String?`

你可以用 `request_id` 在业务侧把请求与回传关联起来。

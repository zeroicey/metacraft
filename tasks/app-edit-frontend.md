# 任务：应用编辑 - 前端集成

**状态：** 待开始 📋
**创建日期：** 2026-03-10

## 背景

后端应用编辑流水线 (`AppEditPipelineService`) 已完成，前端需要集成显示编辑后的应用信息。

## 后端 SSE 事件

编辑流水线返回的事件：

| 事件 | 数据 | 说明 |
|------|------|------|
| `message` | JSON 字符串 | `chatBeforeEdit` 内容（流式） |
| `app_generated` | `{url, uuid, name, description}` | 编辑后的应用信息 |

## 需要修改的文件

`apps/huawei/entry/src/main/ets/components/chat/ChatPanel.ets`

## 前端实现要点

### 1. 消息模型

确认 `ChatMessage` 模型支持编辑场景：
- `isGenMessage: boolean` - 标记为应用生成/编辑消息
- `previewUrl: string` - 预览 URL
- `appName: string` - 应用名称
- `appDescription: string` - 应用描述
- `logoUrl: string` - 应用 logo（可选）

### 2. SSE 回调处理

确认 `app_generated` 事件回调能正确处理并显示应用预览卡片

### 3. 消息展示

确认 `MessageItem.ets` 组件能正确显示：
- 对话内容（`chatBeforeEdit`）
- 应用预览卡片（`AppPreviewCard` 组件）

### 4. 侧边栏会话列表

确认 `SessionList.ets` 能显示应用的 logo 和名称

## 任务清单

### 验证现有实现
- [ ] 验证 `ChatMessage` 模型是否支持编辑场景
- [ ] 验证 SSE `app_generated` 回调是否正确处理
- [ ] 验证 `MessageItem` 能显示对话 + 应用卡片
- [ ] 验证 `SessionList` 能显示应用 logo

### 缺失功能补充（如有）
- [ ] 补充缺失的消息字段支持
- [ ] 补充缺失的 UI 组件

### 测试
- [ ] 测试编辑功能完整流程
- [ ] 测试应用预览卡片显示
- [ ] 测试侧边栏会话列表更新

## 参考文件

- 后端流水线: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/pipeline/AppEditPipelineService.java`
- SSE 工具: `apps/huawei/entry/src/main/ets/utils/http/SSEClient.ets`
- 消息模型: `apps/huawei/entry/src/main/ets/components/chat/ChatMessageModels.ets`
- 消息组件: `apps/huawei/entry/src/main/ets/components/chat/MessageItem.ets`
- 预览卡片: `apps/huawei/entry/src/main/ets/components/AppPreviewCard.ets`
- 会话列表: `apps/huawei/entry/src/main/ets/components/sidebar/SessionList.ets`

## 预期效果

用户编辑应用后，聊天界面显示：
1. AI 的回复内容（`chatBeforeEdit`）
2. 应用预览卡片，包含：应用 logo、名称、描述、点击可跳转预览页面

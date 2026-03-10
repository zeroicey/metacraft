# 任务：智能体切换功能 - 元梦/元爪

**状态：** 待开始 📋
**创建日期：** 2026-03-10

## 背景

添加双智能体支持，用户可以在"元梦"（应用生成）和"元爪"（通用聊天）之间切换。

## 智能体说明

| 智能体 | 名称 | 用途 | 后端协议 | 会话管理 |
|--------|------|------|----------|----------|
| YuanMeng | 元梦 | 应用生成 | SSE (`/api/ai/agent/unified`) | 支持多会话 |
| YuanClaw | 元爪 | 通用聊天 | WebSocket (`/ws/yuanclaw/client`) | 单一聊天，无会话 |

## UI 设计

**顶部标签页：**
```
┌────────────────────────────────────────────┐
│  ☰              [元梦|元爪]         ⋯     │
└────────────────────────────────────────────┘
```

**视觉规范：**
- 选中状态：蓝色底色 + 白色文字 (`#0A59F7`)
- 未选中状态：透明底色 + 灰色文字 (`#999999`)
- 圆角：`8px`
- 高度：`32px`

**行为：**
- 元梦模式：显示会话列表侧边栏
- 元爪模式：隐藏侧边栏

## 架构设计

```
Index.ets (主页面)
    ├── TabBar (新增) - 元梦 | 元爪 切换
    ├── YuanMengChatPanel (现有) - SSE + 会话管理
    └── YuanClawChatPanel (新增) - WebSocket + 单一聊天
```

## 需要创建的文件

1. `apps/huawei/entry/src/main/ets/components/chat/YuanClawChatPanel.ets` - 元爪聊天面板
2. `apps/huawei/entry/src/main/ets/model/AgentType.ets` - 智能体类型枚举
3. `apps/huawei/entry/src/main/ets/api/yuanClaw.ets` - 元爪 WebSocket API 封装

## 需要修改的文件

1. `apps/huawei/entry/src/main/ets/pages/Index.ets` - 添加标签页切换逻辑
2. `apps/huawei/entry/src/main/ets/components/chat/ChatPanel.ets` - 重命名为 `YuanMengChatPanel.ets`

## 状态管理

**新增状态：**
- `currentAgent: AgentType` - 当前选中的智能体
- `yuanmengSessionId: string` - 元梦会话 ID

**持久化：** 使用 Preferences API 保存 `currentAgent`

## YuanClaw WebSocket 消息格式

| 方向 | 格式 |
|------|------|
| 发送 | `{ "type": "inbound", "content": "用户消息" }` |
| 接收 | `{ "type": "outbound", "content": "AI回复" }` |
| 接收 | `{ "type": "progress", "content": "处理进度..." }` |

## UI 切换逻辑

| 当前智能体 | 显示组件 | 侧边栏 |
|-----------|---------|--------|
| 元梦 | `YuanMengChatPanel` | 显示 |
| 元爪 | `YuanClawChatPanel` | 隐藏 |

## 任务清单

### 阶段 1：基础结构
- [ ] 创建 `AgentType.ets` 枚举
- [ ] 创建 `yuanClaw.ets` WebSocket API 封装
- [ ] 将 `ChatPanel.ets` 重命名为 `YuanMengChatPanel.ets`

### 阶段 2：元爪 ChatPanel
- [ ] 创建 `YuanClawChatPanel.ets` 基础结构
- [ ] 实现 WebSocket 连接管理
- [ ] 实现消息发送/接收
- [ ] 实现消息流式展示
- [ ] 实现错误处理

### 阶段 3：UI 集成
- [ ] 在 `Index.ets` 添加标签页组件
- [ ] 实现智能体切换逻辑
- [ ] 实现侧边栏显示/隐藏控制
- [ ] 实现状态持久化 (Preferences)

### 阶段 4：测试
- [ ] 测试元梦聊天功能
- [ ] 测试元爪聊天功能
- [ ] 测试智能体切换
- [ ] 测试状态持久化

## 参考文件

- WebSocket Handler: `apps/api/src/main/java/com/metacraft/api/modules/yuanclaw/ws/`
- Bridge Service: `apps/api/src/main/java/com/metacraft/api/modules/yuanclaw/service/YuanClawBridgeService.java`
- DTO: `apps/api/src/main/java/com/metacraft/api/modules/yuanclaw/dto/YuanClawWsMessage.java`

# YuanMeng 聊天页面设计

## 概述

实现前端的 yuanmeng 聊天页面，通过 WebSocket 与后端通信，无需登录。

## 功能

| 功能 | 实现 |
|------|------|
| 消息发送 | 通过 WebSocket 发送 JSON |
| 消息接收 | 监听 onMessage，AI 返回完整消息 |
| Markdown 渲染 | 用 Streamdown 渲染 Markdown |
| 自动重连 | react-use-websocket 配置 |
| 连接状态 | 显示在线/连接中/断开 |

## WebSocket 通信

### 端点
`ws://localhost:8080/ws/yuanmeng/client`

### 消息格式

**发送：**
```json
{
  "type": "user_message",
  "chatId": "shared",
  "senderId": "client-xxx",
  "content": "用户输入的内容",
  "metadata": { "source": "client" }
}
```

**接收：**
```json
{
  "type": "assistant_message",
  "chatId": "shared",
  "senderId": "nanobot",
  "content": "AI 回复内容"
}
```

**事件类型：**
- `connected` - 连接成功
- `user_message` - 用户自己发的消息（回显）
- `assistant_message` - AI 回复
- `progress` - 进度消息
- `status` - 状态更新
- `error` - 错误

## UI 设计

与 `yuanchuang.tsx` 保持一致风格，使用相同组件。

```
┌─────────────────────────────────────────┐
│  消息列表 (scrollable)                  │
│  - 用户消息 (右对齐，蓝色)              │
│  - AI 消息 (左对齐，用 Streamdown 渲染)  │
└─────────────────────────────────────────┘
┌─────────────────────────────────────────┐
│ [输入框...........................] [发送]│
│ 状态: ● 已连接                          │
└─────────────────────────────────────────┘
```

## 依赖

- react-use-websocket
- streamdown (已安装)
- lucide-react (已安装)
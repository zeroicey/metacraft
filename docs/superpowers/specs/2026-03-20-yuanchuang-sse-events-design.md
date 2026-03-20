# 元创页面 SSE 事件处理设计

## 概述

为 web 端的元创页面实现实时流式 SSE 事件处理，参照鸿蒙端实时展示生成过程。

## 背景

用户发送消息后，后端会发送多个 SSE 事件：
- `intent` - 意图分类
- `message` - 聊天内容
- `plan` - 应用生成计划
- `app_info` - 应用信息
- `logo_generated` - Logo 生成完成
- `app_generated` - 应用生成完成
- `done` - 流结束

当前 web 端只处理了 `message` 事件，其他事件未处理。

## 设计方案

### 架构

```
yuanchuang.tsx
├── useChatStream (改造)
│   └── 监听所有 SSE 事件并触发回调
├── 状态管理
│   ├── streamingContent
│   ├── planContent
│   ├── appInfo
│   ├── logoData
│   └── appGeneratedData
└── 渲染层
    ├── 消息列表
    ├── PlanCard
    ├── AppInfoCard
    └── AppPreviewCard
```

### 事件处理

| 事件 | 回调 | 数据格式 |
|------|------|----------|
| intent | onIntent | `chat\|gen\|edit` |
| message | onMessage | `{"content": "..."}` |
| plan | onPlan | `{"plan": "..."}` |
| app_info | onAppInfo | `{"name": "...", "description": "..."}` |
| logo_generated | onLogoGenerated | `{"uuid": "...", "ext": "..."}` |
| app_generated | onAppGenerated | `{"uuid": "...", "version": 1}` |
| error | onError | `{"error": "..."}` |
| done | onDone | - |

### 组件设计

#### PlanCard
- 显示生成计划
- 使用 Markdown 渲染
- 最大高度限制，可滚动

#### AppInfoCard
- 显示应用名称
- 显示应用描述
- 显示 Logo

#### AppPreviewCard
- 内嵌 iframe 预览
- 打开新窗口按钮

## 验收标准

- [ ] intent 事件正确识别并显示
- [ ] plan 事件触发 PlanCard 显示
- [ ] app_info 事件显示应用信息
- [ ] logo_generated 事件显示 Logo
- [ ] app_generated 事件显示预览
- [ ] 实时流式更新
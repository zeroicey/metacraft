# 前端 UI 美化实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 MetaCraft 前端应用美化 UI，元创使用蓝色系 #007AFF，元梦使用粉色系 #EC4899，元梦侧边栏新增知识库

**Architecture:** 基于 Tailwind CSS 的双主题系统，通过 CSS 变量和条件类实现主题切换

**Tech Stack:** React + TypeScript + Tailwind CSS + Lucide React

---

## Chunk 1: 核心样式定义与导航栏

### Task 1: 创建主题 CSS 变量文件

**Files:**
- Create: `apps/web/src/styles/themes.css`

- [ ] **Step 1: 创建主题 CSS 变量文件**

```css
/* apps/web/src/styles/themes.css */

/* 元创（YuanChuang）蓝色主题 */
.theme-yuanchuang {
  --color-primary: #007AFF;
  --color-primary-light: #E6F2FF;
  --color-primary-dark: #0056CC;
  --color-bg: #F5F7FA;
  --color-card: #FFFFFF;
  --color-text: #1F2937;
  --color-text-muted: #6B7280;
  --color-border: #E5E7EB;
}

/* 元梦（YuanMeng）粉色主题 */
.theme-yuanmeng {
  --color-primary: #EC4899;
  --color-primary-light: #FDF2F8;
  --color-primary-dark: #BE185D;
  --color-bg: #FDF4FF;
  --color-card: #FFFFFF;
  --color-text: #1F2937;
  --color-text-muted: #6B7280;
  --color-border: #FCE7F3;
}
```

- [ ] **Step 2: 在 main.tsx 中引入主题样式**

Run: 编辑 `apps/web/src/main.tsx`，在顶部添加：
```typescript
import "./styles/themes.css";
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/styles/themes.css apps/web/src/main.tsx
git commit -m "feat(web): add theme CSS variables for yuanchuang and yuanmeng"
```

---

### Task 2: 更新根布局应用主题类

**Files:**
- Modify: `apps/web/src/components/layouts/root-layout.tsx:1-35`

- [ ] **Step 1: 读取并修改 root-layout.tsx**

在 className 中添加主题类：
```tsx
// 原: <div className="flex h-screen w-screen overflow-hidden">
// 改为:
<div className={`flex h-screen w-screen overflow-hidden ${currentPage === 'yuanmeng' ? 'theme-yuanmeng' : 'theme-yuanchuang'}`}>
```

需要添加 useAppStore 获取 currentPage：
```tsx
const currentPage = useAppStore((state) => state.currentPage);
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/components/layouts/root-layout.tsx
git commit -m "feat(web): apply theme class based on current page"
```

---

## Chunk 2: 元创（YuanChuang）页面样式

### Task 3: 更新元创页面消息气泡样式

**Files:**
- Modify: `apps/web/src/pages/yuanchuang.tsx:93-108`

- [ ] **Step 1: 修改用户消息气泡颜色**

修改第 103 行的消息气泡：
```tsx
// 原: bg-blue-500
// 改为: bg-[#007AFF]
<div className="max-w-[80%] rounded-lg px-4 py-2 bg-[#007AFF] text-white text-sm whitespace-pre-wrap">
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/pages/yuanchuang.tsx
git commit -m "feat(web): apply blue theme to yuanchuang message bubbles"
```

---

### Task 4: 更新元创侧边栏样式

**Files:**
- Modify: `apps/web/src/components/sidebar/sidebar-content.tsx:84-124`

- [ ] **Step 1: 修改创建按钮样式**

```tsx
// 原: className="w-full h-10 bg-[#F2F2F2] hover:bg-[#E5E5E5] text-gray-800 rounded-lg"
// 改为:
<button
  className="w-full h-10 bg-[#007AFF] hover:bg-[#0056CC] text-white rounded-lg flex items-center justify-center gap-2"
  // ...
>
```

- [ ] **Step 2: 修改导航项图标颜色**

```tsx
// 原: <item.icon className="h-4 w-4 text-gray-500" />
// 改为:
<item.icon className="h-4 w-4 text-[#007AFF]" />
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/components/sidebar/sidebar-content.tsx
git commit -m "feat(web): apply blue theme to yuanchuang sidebar"
```

---

## Chunk 3: 元梦（YuanMeng）页面样式

### Task 5: 更新元梦页面消息气泡样式

**Files:**
- Modify: `apps/web/src/pages/yuanmeng.tsx:21-42`

- [ ] **Step 1: 修改用户消息气泡颜色**

修改第 33 行：
```tsx
// 原: bg-blue-500
// 改为: bg-[#EC4899]
<div className="rounded-lg px-4 py-2 bg-[#EC4899] text-white text-sm whitespace-pre-wrap">
```

- [ ] **Step 2: 修改 AI 消息边框颜色**

修改第 45 行：
```tsx
// 原: <div key={message.id} className="max-w-[80%]">
// 改为（添加粉色边框）:
<div key={message.id} className="max-w-[80%] border border-[#FCE7F3] rounded-lg p-3 bg-white">
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/pages/yuanmeng.tsx
git commit -m "feat(web): apply pink theme to yuanmeng message bubbles"
```

---

### Task 6: 更新元梦页面输入框和按钮样式

**Files:**
- Modify: `apps/web/src/pages/yuanmeng.tsx:119-142`

- [ ] **Step 1: 修改输入框样式**

```tsx
// 原: <Input className="p-3" ... />
// 改为:
<Input
  className="p-3 border-[#EC4899] focus:border-[#EC4899] focus:ring-[#EC4899]"
  ...
/>
```

- [ ] **Step 2: 修改发送按钮样式**

```tsx
// 原: <Button size="icon" ...>
// 改为（添加粉色背景）:
<Button
  size="icon"
  className="bg-[#EC4899] hover:bg-[#BE185D]"
  onClick={handleSendMessage}
  disabled={!isConnected || !inputText.trim()}
>
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/pages/yuanmeng.tsx
git commit -m "feat(web): apply pink theme to yuanmeng input and button"
```

---

## Chunk 4: 元梦侧边栏 - 知识库

### Task 7: 创建知识库侧边栏组件

**Files:**
- Create: `apps/web/src/components/sidebar/knowledge-base.tsx`

- [ ] **Step 1: 创建知识库组件**

```tsx
import { useState } from "react";
import { BookIcon, PlusIcon, ChevronRightIcon } from "lucide-react";

interface KnowledgeBase {
  id: string;
  name: string;
  articleCount: number;
}

const mockKnowledgeBases: KnowledgeBase[] = [
  { id: "1", name: "我的知识库", articleCount: 12 },
  { id: "2", name: "技术文档", articleCount: 8 },
  { id: "3", name: "产品FAQ", articleCount: 24 },
];

export function KnowledgeBaseSidebar() {
  const [selectedId, setSelectedId] = useState<string>("1");

  return (
    <div className="border-t border-[#FCE7F3]">
      <div className="px-3 py-2">
        <div className="text-xs font-medium text-gray-500 uppercase tracking-wider">
          知识库
        </div>
      </div>

      <div className="px-3 pb-2 space-y-1">
        {mockKnowledgeBases.map((kb) => (
          <button
            key={kb.id}
            onClick={() => setSelectedId(kb.id)}
            className={`w-full flex items-center gap-2 px-2 py-2 rounded-lg transition-colors ${
              selectedId === kb.id
                ? "bg-[#FDF2F8] border border-[#EC4899]"
                : "hover:bg-gray-50"
            }`}
          >
            <div
              className={`w-7 h-7 rounded-md flex items-center justify-center ${
                selectedId === kb.id
                  ? "bg-gradient-to-br from-[#EC4899] to-[#8B5CF6]"
                  : "bg-gray-200"
              }`}
            >
              <BookIcon
                className={`w-3.5 h-3.5 ${
                  selectedId === kb.id ? "text-white" : "text-gray-500"
                }`}
              />
            </div>
            <div className="flex-1 text-left">
              <div
                className={`text-xs ${
                  selectedId === kb.id ? "font-medium text-gray-900" : "text-gray-700"
                }`}
              >
                {kb.name}
              </div>
              <div className="text-[10px] text-gray-400">{kb.articleCount} 篇文章</div>
            </div>
            <ChevronRightIcon className="w-3 h-3 text-gray-400" />
          </button>
        ))}
      </div>

      <div className="px-3 pb-3">
        <button className="w-full py-2 bg-[#EC4899] hover:bg-[#BE185D] text-white text-xs font-medium rounded-lg flex items-center justify-center gap-1 transition-colors">
          <PlusIcon className="w-3.5 h-3.5" />
          新建知识库
        </button>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/components/sidebar/knowledge-base.tsx
git commit -m "feat(web): create knowledge base sidebar component"
```

---

### Task 8: 在元梦侧边栏中集成知识库

**Files:**
- Modify: `apps/web/src/components/sidebar/sandbox-monitor.tsx:141-210`

- [ ] **Step 1: 修改 sandbox-monitor.tsx 导出**

在文件顶部添加导入：
```tsx
import { KnowledgeBaseSidebar } from "./knowledge-base";
```

在 YuanMengSidebarContent 组件返回中添加知识库：
```tsx
// 在 </div> 前添加:
<KnowledgeBaseSidebar />
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/components/sidebar/sandbox-monitor.tsx
git commit -m "feat(web): integrate knowledge base into yuanmeng sidebar"
```

---

## Chunk 5: 其他页面统一主题

### Task 9: 更新 Store 页面主题

**Files:**
- Modify: `apps/web/src/pages/store.tsx:1-80`

- [ ] **Step 1: 修改 Store 页面背景色**

```tsx
// 原: <div className="flex h-full flex-col bg-[#F5F7FA]">
// 改为:
<div className="flex h-full flex-col bg-[var(--color-bg)]">
```

- [ ] **Step 2: 修改加载图标颜色**

```tsx
// 原: <Loader2Icon className="h-8 w-8 animate-spin text-[#007AFF]" />
// 改为:
<Loader2Icon className="h-8 w-8 animate-spin text-[var(--color-primary)]" />
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/pages/store.tsx
git commit -m "feat(web): apply theme to store page"
```

---

### Task 10: 更新 MyApps 页面主题

**Files:**
- Modify: `apps/web/src/pages/myapps.tsx`

- [ ] **Step 1: 查看并修改 myapps.tsx**

使用主题变量替换硬编码颜色。

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/pages/myapps.tsx
git commit -m "feat(web): apply theme to myapps page"
```

---

### Task 11: 更新 Navbar 导航栏主题

**Files:**
- Modify: `apps/web/src/components/navbar.tsx:1-95`

- [ ] **Step 1: 修改 Tab 切换背景色**

```tsx
// 原: <div className="relative flex items-center gap-1 bg-[#F2F2F2] rounded-lg p-1">
// 改为（根据当前页面使用主题色）:
<div className="relative flex items-center gap-1 bg-[var(--color-primary-light)] rounded-lg p-1">
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/components/navbar.tsx
git commit -m "feat(web): apply theme to navbar"
```

---

## Chunk 6: 组件样式优化

### Task 12: 更新 Button 组件主题

**Files:**
- Modify: `apps/web/src/components/ui/button.tsx`

- [ ] **Step 1: 为按钮添加主题色变量支持**

在 default Variants 中添加主题支持。

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/components/ui/button.tsx
git commit -m "feat(web): update button component theme support"
```

---

### Task 13: 更新 Input 组件主题

**Files:**
- Modify: `apps/web/src/components/ui/input.tsx`

- [ ] **Step 1: 修改输入框 focus 样式**

```tsx
// 添加 focus:ring-[var(--color-primary)] focus:border-[var(--color-primary)]
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/components/ui/input.tsx
git commit -m "feat(web): update input component theme support"
```

---

## 验收检查

- [ ] 所有修改已完成
- [ ] 元创页面使用蓝色系 #007AFF
- [ ] 元梦页面使用粉色系 #EC4899
- [ ] 元梦侧边栏包含沙盒监控 + 知识库
- [ ] 消息气泡样式符合主题
- [ ] 提交记录完整

---

**Plan complete and saved to `docs/superpowers/plans/2026-03-27-frontend-beautify.md`. Ready to execute?**
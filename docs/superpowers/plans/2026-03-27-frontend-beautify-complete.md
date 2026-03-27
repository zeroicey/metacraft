# 元创前端 UI 全面重建实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development to implement this plan.

**Goal:** 全面重建元创前端 UI，实现精致拟物化风格，源创蓝色主题，元梦粉色主题

**Architecture:** 基于 Tailwind CSS 的双主题系统，使用 CSS 变量管理主题色

**Tech Stack:** React + TypeScript + Tailwind CSS + Lucide React

---

## Chunk 1: 基础主题系统

### Task 1: 更新主题 CSS 变量

**Files:**
- Modify: `apps/web/src/styles/themes.css`

- [ ] **Step 1: 扩展主题 CSS 变量**

```css
/* 基础阴影变量 */
:root {
  /* 源创阴影 */
  --shadow-card-yuanchuang: 0 2px 8px rgba(0,122,255,0.08), 0 8px 24px rgba(0,0,0,0.06);
  --shadow-bubble-yuanchuang: 0 2px 6px rgba(0,122,255,0.3);
  --shadow-button-yuanchuang: 0 2px 4px rgba(0,122,255,0.3), inset 0 1px 0 rgba(255,255,255,0.2);

  /* 元梦阴影 */
  --shadow-card-yuanmeng: 0 2px 8px rgba(236,72,153,0.08), 0 8px 24px rgba(0,0,0,0.06);
  --shadow-bubble-yuanmeng: 0 2px 6px rgba(236,72,153,0.3);
  --shadow-button-yuanmeng: 0 2px 4px rgba(236,72,153,0.3), inset 0 1px 0 rgba(255,255,255,0.2);

  /* 圆角变量 */
  --radius-card: 16px;
  --radius-button: 12px;
  --radius-bubble-user: 14px 14px 4px 14px;
  --radius-bubble-ai: 14px 14px 14px 4px;
  --radius-input: 12px;
}
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/styles/themes.css
git commit -m "feat(web): add extended theme CSS variables"
```

---

## Chunk 2: 导航栏重建

### Task 2: 重构导航栏样式

**Files:**
- Modify: `apps/web/src/components/navbar.tsx`

- [ ] **Step 1: 修改 Tab 切换背景**

```tsx
// 修改第 47 行
<div className="relative flex items-center gap-1 bg-gradient-to-br from-[#E8F0FE] to-[#F0F4F8] rounded-xl p-1">
// 源创时
<div className="relative flex items-center gap-1 bg-gradient-to-br from-[#FDF2F8] to-[#FEF1F7] rounded-xl p-1">
// 元梦时
```

- [ ] **Step 2: 修改激活项样式**

```tsx
// 添加激活项阴影
className="relative z-10 px-4 py-1.5 text-sm font-medium rounded-lg transition-all bg-white shadow-sm"
```

- [ ] **Step 3: 修改激活文字颜色**

```tsx
// 源创激活
className="relative z-10 px-4 py-1.5 text-sm font-medium rounded-lg transition-all text-[#007AFF]"
// 元梦激活
className="relative z-10 px-4 py-1.5 text-sm font-medium rounded-lg transition-all text-[#EC4899]"
```

- [ ] **Step 4: 提交**

```bash
git add apps/web/src/components/navbar.tsx
git commit -m "feat(web): rebuild navbar with theme support"
```

---

## Chunk 3: 侧边栏重建

### Task 3: 源创侧边栏样式重构

**Files:**
- Modify: `apps/web/src/components/sidebar/index.tsx`
- Modify: `apps/web/src/components/sidebar/sidebar-content.tsx`
- Modify: `apps/web/src/components/layouts/root-layout.tsx`

- [ ] **Step 1: 修改侧边栏头部 Logo 区域**

```tsx
// root-layout.tsx - 添加主题类
<div className="flex h-6 w-6 items-center justify-center rounded-xl bg-gradient-to-br from-[#007AFF] to-[#0056CC]">
// 元梦时
<div className="flex h-6 w-6 items-center justify-center rounded-xl bg-gradient-to-br from-[#EC4899] to-[#BE185D]">
```

- [ ] **Step 2: 修改创建按钮样式**

```tsx
// sidebar-content.tsx - 第 96 行
className="w-full h-10 bg-gradient-to-br from-[#007AFF] to-[#0056CC] hover:from-[#0056CC] hover:to-[#0044AA] text-white rounded-xl flex items-center justify-center gap-2 shadow-md"
```

- [ ] **Step 3: 修改导航项样式**

```tsx
// sidebar-content.tsx
className="flex items-center gap-2 w-full px-3 py-2 rounded-lg hover:bg-[#F3F4F6] transition-all"
```

- [ ] **Step 4: 提交**

```bash
git add apps/web/src/components/sidebar/
git commit -m "feat(web): rebuild yuanchuang sidebar styles"
```

---

### Task 4: 元梦侧边栏重构

**Files:**
- Modify: `apps/web/src/components/sidebar/sandbox-monitor.tsx`
- Modify: `apps/web/src/components/sidebar/knowledge-base.tsx`

- [ ] **Step 1: 更新沙盒监控样式为粉色主题**

```tsx
// sandbox-monitor.tsx
// 状态指示
className={`w-2 h-2 rounded-full ${isRunning ? "bg-[#10B981]" : "bg-gray-400"}`}

// 资源条 - 使用粉色渐变
<div style={{ width: `${value}%` }} className="h-full bg-gradient-to-r from-[#EC4899] to-[#F472B6] rounded-full" />
```

- [ ] **Step 2: 移除知识库新建按钮**

```tsx
// knowledge-base.tsx - 移除新建按钮部分
// 删除最后的 <button> 新建知识库 </button>
```

- [ ] **Step 3: 修改知识库列表项样式**

```tsx
// knowledge-base.tsx
className={`w-full flex items-center gap-2 px-3 py-2.5 rounded-xl transition-all ${
  selectedId === kb.id
    ? "bg-gradient-to-r from-[#FDF2F8] to-[#FCE7F3] border border-[#EC4899] shadow-sm"
    : "hover:bg-[#F9FAFB] border border-transparent"
}`}
```

- [ ] **Step 4: 提交**

```bash
git add apps/web/src/components/sidebar/
git commit -m "feat(web): rebuild yuanmeng sidebar with pink theme"
```

---

## Chunk 4: 源创页面重建

### Task 5: 源创页面消息气泡和输入框

**Files:**
- Modify: `apps/web/src/pages/yuanchuang.tsx`

- [ ] **Step 1: 修改用户消息气泡**

```tsx
// 第 103 行
<div className="max-w-[80%] px-4 py-3 bg-gradient-to-br from-[#007AFF] to-[#0056CC] text-white text-sm whitespace-pre-wrap rounded-[14px_14px_4px_14px] shadow-md">
```

- [ ] **Step 2: 修改 AI 消息气泡**

```tsx
// 第 127-135 行
<div className="max-w-[80%] border border-[#E5E7EB] rounded-[14px_14px_14px_4px] p-4 bg-white shadow-sm">
```

- [ ] **Step 3: 修改输入框区域容器**

```tsx
// 第 156 行
<div className="px-4 py-4 bg-white rounded-2xl shadow-lg mx-4 mb-4">
```

- [ ] **Step 4: 修改输入框样式**

```tsx
// 第 158 行
<Input
  className="p-4 bg-[#F9FAFB] border border-[#E5E7EB] rounded-xl focus:border-[#007AFF] focus:ring-2 focus:ring-[#007AFF]/20 focus:bg-white transition-all"
  ...
/>
```

- [ ] **Step 5: 修改发送按钮样式**

```tsx
// 第 166 行
<Button
  size="icon"
  className="h-11 w-11 bg-gradient-to-br from-[#007AFF] to-[#0056CC] hover:from-[#0056CC] hover:to-[#0044AA] text-white rounded-xl shadow-md transition-all hover:scale-105 active:scale-95"
  ...
/>
```

- [ ] **Step 6: 提交**

```bash
git add apps/web/src/pages/yuanchuang.tsx
git commit -m "feat(web): rebuild yuanchuang page UI"
```

---

## Chunk 5: 元梦页面重建

### Task 6: 元梦页面消息气泡和输入框

**Files:**
- Modify: `apps/web/src/pages/yuanmeng.tsx`

- [ ] **Step 1: 修改用户消息气泡**

```tsx
// 第 33 行
<div className="rounded-[14px_14px_4px_14px] px-4 py-3 bg-gradient-to-br from-[#EC4899] to-[#F472B6] text-white text-sm whitespace-pre-wrap shadow-md">
```

- [ ] **Step 2: 修改 AI 消息气泡**

```tsx
// 第 45 行
<div key={message.id} className="max-w-[80%] border border-[#FCE7F3] rounded-[14px_14px_14px_4px] p-4 bg-white shadow-sm">
```

- [ ] **Step 3: 修改输入框区域**

```tsx
// 第 120 行
<div className="px-4 py-3 bg-white rounded-2xl shadow-lg mx-4 mb-3">
```

- [ ] **Step 4: 修改输入框样式**

```tsx
// 第 122 行
<Input
  className="p-3 bg-[#F9FAFB] border border-[#E5E7EB] rounded-xl focus:border-[#EC4899] focus:ring-2 focus:ring-[#EC4899]/20 focus:bg-white transition-all"
  ...
/>
```

- [ ] **Step 5: 修改发送按钮样式**

```tsx
// 第 130 行
<Button
  size="icon"
  className="h-10 w-10 bg-gradient-to-br from-[#EC4899] to-[#BE185D] hover:from-[#BE185D] hover:to-[#9D174D] text-white rounded-xl shadow-md transition-all hover:scale-105 active:scale-95"
  ...
/>
```

- [ ] **Step 6: 提交**

```bash
git add apps/web/src/pages/yuanmeng.tsx
git commit -m "feat(web): rebuild yuanmeng page UI"
```

---

## Chunk 6: 其他页面重建

### Task 7: 应用商店页面

**Files:**
- Modify: `apps/web/src/pages/store.tsx`

- [ ] **Step 1: 修改页面背景**

```tsx
// 第 12 行
<div className="flex h-full flex-col bg-gradient-to-b from-[#F0F4F8] to-white">
```

- [ ] **Step 2: 修改应用卡片样式**

```tsx
// StoreAppCard 组件
className="bg-white rounded-2xl border border-[#E5E7EB] shadow-md hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/pages/store.tsx
git commit -m "feat(web): rebuild store page UI"
```

---

### Task 8: 我的应用页面

**Files:**
- Modify: `apps/web/src/pages/myapps.tsx`

- [ ] **Step 1: 修改页面背景**

```tsx
<div className="flex h-full flex-col bg-gradient-to-b from-[#F0F4F8] to-white">
```

- [ ] **Step 2: 修改应用卡片样式**

```tsx
className="bg-white rounded-2xl border border-[#E5E7EB] p-4 shadow-md hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/pages/myapps.tsx
git commit -m "feat(web): rebuild myapps page UI"
```

---

### Task 9: 预览页面

**Files:**
- Modify: `apps/web/src/pages/preview.tsx`

- [ ] **Step 1: 修改页面容器样式**

```tsx
className="h-full bg-white rounded-2xl shadow-lg overflow-hidden"
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/pages/preview.tsx
git commit -m "feat(web): rebuild preview page UI"
```

---

### Task 10: 个人中心页面

**Files:**
- Modify: `apps/web/src/pages/profile.tsx`

- [ ] **Step 1: 修改头像样式**

```tsx
className="w-20 h-20 rounded-full border-4 border-white shadow-lg"
```

- [ ] **Step 2: 修改卡片样式**

```tsx
className="bg-white rounded-2xl shadow-md p-6"
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/pages/profile.tsx
git commit -m "feat(web): rebuild profile page UI"
```

---

### Task 11: 登录/注册页面

**Files:**
- Modify: `apps/web/src/pages/login.tsx`
- Modify: `apps/web/src/pages/register.tsx`

- [ ] **Step 1: 修改容器背景**

```tsx
className="min-h-screen bg-gradient-to-br from-[#F0F4F8] via-[#E8F0FE] to-[#F0F4F8]"
```

- [ ] **Step 2: 修改表单卡片**

```tsx
className="bg-white rounded-2xl shadow-xl p-8"
```

- [ ] **Step 3: 修改提交按钮**

```tsx
className="w-full bg-gradient-to-br from-[#007AFF] to-[#0056CC] text-white rounded-xl py-3 font-medium shadow-md hover:shadow-lg transition-all"
```

- [ ] **Step 4: 提交**

```bash
git add apps/web/src/pages/login.tsx apps/web/src/pages/register.tsx
git commit -m "feat(web): rebuild login/register pages UI"
```

---

## Chunk 7: UI 组件重构

### Task 12: Button 组件样式

**Files:**
- Modify: `apps/web/src/components/ui/button.tsx`

- [ ] **Step 1: 添加主题色支持**

```tsx
// 在 defaultVariants 中添加
defaultVariants: {
  variant: "default", // 源创蓝
}
```

- [ ] **Step 2: 添加粉色主题变体**

```tsx
// 在 cva 中添加
pink: "bg-gradient-to-br from-[#EC4899] to-[#BE185D] text-white shadow-md hover:shadow-lg",
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/components/ui/button.tsx
git commit -m "feat(web): add pink variant to button component"
```

---

### Task 13: Input 组件样式

**Files:**
- Modify: `apps/web/src/components/ui/input.tsx`

- [ ] **Step 1: 添加聚焦样式**

```tsx
className="flex h-10 w-full rounded-xl border border-[#E5E7EB] bg-[#F9FAFB] px-3 py-2 text-sm ring-offset-white file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-[#9CA3AF] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#007AFF] focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/components/ui/input.tsx
git commit -m "feat(web): enhance input component styles"
```

---

## 验收检查

- [ ] 导航栏 Tab 切换显示正确颜色
- [ ] 源创页面使用蓝色主题
- [ ] 元梦页面使用粉色主题
- [ ] 源创侧边栏内容完整
- [ ] 元梦侧边栏有沙盒监控 + 知识库（无新建按钮）
- [ ] 消息气泡样式符合规范
- [ ] 输入框聚焦效果正确
- [ ] 按钮阴影和圆角正确
- [ ] 卡片阴影和边框正确
- [ ] 所有页面主题一致

---

**Plan complete and saved to `docs/superpowers/plans/2026-03-27-frontend-beautify-complete.md`. Ready to execute?**
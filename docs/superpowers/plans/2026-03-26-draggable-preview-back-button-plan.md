# 可拖动预览页面返回按钮实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在预览页面添加可拖动的悬浮返回按钮，支持桌面端鼠标和移动端触摸拖动

**Architecture:** 使用 React Pointer Events API 实现统一的事件处理，无额外依赖

**Tech Stack:** React 19, TypeScript, Tailwind CSS 4

---

## 实现步骤

### 任务 1: 修改 preview.tsx 添加拖动功能

**文件:**
- Modify: `apps/web/src/pages/preview.tsx`

- [ ] **Step 1: 添加拖动相关状态**

在现有状态区域添加以下状态：

```typescript
const [position, setPosition] = useState({ x: 16, y: 16 });
const [isDragging, setIsDragging] = useState(false);
const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
const [buttonRect, setButtonRect] = useState({ width: 40, height: 40 });
```

- [ ] **Step 2: 添加 Pointer 事件处理函数**

在 `handleBack` 函数后添加：

```typescript
const handlePointerDown = (e: React.PointerEvent) => {
  const button = e.currentTarget.getBoundingClientRect();
  setButtonRect({ width: button.width, height: button.height });
  setDragOffset({
    x: e.clientX - position.x,
    y: e.clientY - position.y,
  });
  setIsDragging(true);
  (e.target as HTMLElement).setPointerCapture(e.pointerId);
};

const handlePointerMove = (e: React.PointerEvent) => {
  if (!isDragging) return;

  const newX = e.clientX - dragOffset.x;
  const newY = e.clientY - dragOffset.y;

  // 边界限制
  const maxX = window.innerWidth - buttonRect.width;
  const maxY = window.innerHeight - buttonRect.height;

  setPosition({
    x: Math.max(0, Math.min(newX, maxX)),
    y: Math.max(0, Math.min(newY, maxY)),
  });
};

const handlePointerUp = (e: React.PointerEvent) => {
  setIsDragging(false);
  (e.target as HTMLElement).releasePointerCapture(e.pointerId);
};
```

- [ ] **Step 3: 修改返回按钮样式和事件绑定**

将现有的 button 元素修改为：

```tsx
<button
  onClick={handleBack}
  onPointerDown={handlePointerDown}
  onPointerMove={handlePointerMove}
  onPointerUp={handlePointerUp}
  onPointerLeave={handlePointerUp}
  style={{
    position: 'fixed',
    left: position.x,
    top: position.y,
    touchAction: 'none',
    transition: isDragging ? 'none' : 'all 0.2s ease',
    opacity: isDragging ? 0.8 : 1,
    zIndex: 50,
  }}
  className={`flex items-center justify-center w-10 h-10 rounded-full bg-white/90 shadow-md hover:bg-gray-100 ${
    isDragging ? 'shadow-lg' : ''
  }`}
>
  <ArrowLeftIcon className="w-5 h-5" />
</button>
```

- [ ] **Step 4: 验证实现**

1. 桌面端：使用鼠标拖动按钮到任意位置，松开后应停留在新位置
2. 移动端：使用触摸拖动，测试边界限制
3. 点击测试：点击按钮应正常返回上一页
4. 视觉反馈：拖动时透明度降低，松手后恢复

- [ ] **Step 5: 提交代码**

```bash
git add apps/web/src/pages/preview.tsx
git commit -m "feat(web): add draggable back button to preview page

- Add pointer event handlers for mouse and touch drag support
- Implement boundary constraints to keep button within viewport
- Add visual feedback during dragging (opacity, shadow)

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验收标准检查

- [ ] 桌面端鼠标可以拖动按钮到任意位置
- [ ] 移动端手指可以拖动按钮到任意位置
- [ ] 按钮不能被拖出可视区域
- [ ] 点击按钮仍然可以正常返回上一页
- [ ] 拖动过程中有视觉反馈（透明度、阴影变化）
- [ ] 松开后按钮停留在最后位置
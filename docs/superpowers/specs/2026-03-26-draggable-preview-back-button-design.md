# 预览页面可拖动返回按钮设计

## 概述

在预览页面 (`preview.tsx`) 的左上角添加一个可拖动的悬浮返回按钮，用户可以将按钮拖动到屏幕任意位置松手后保留。

## 功能需求

1. **拖动功能**: 返回按钮可以在屏幕内自由拖动，松开后停留在新位置
2. **点击返回**: 点击按钮仍然执行原有的返回导航功能
3. **移动端适配**: 手指触摸拖动，与桌面端行为一致
4. **边界限制**: 按钮不能拖出可视区域

## 技术方案

### 使用 Pointer Events API

- 使用 `onPointerDown` / `onPointerMove` / `onPointerUp` 统一处理鼠标和触摸事件
- 无需额外依赖库，原生 React 即可实现

### 状态管理

```typescript
// 按钮位置状态（相对于视口左上角的偏移量）
const [position, setPosition] = useState({ x: 16, y: 16 });

// 是否正在拖动
const [isDragging, setIsDragging] = useState(false);

// 拖动起始点（用于计算偏移）
const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
```

### 事件处理流程

1. **PointerDown**: 记录拖动起始位置，设置拖动状态
2. **PointerMove**: 计算新位置，边界检查，更新状态
3. **PointerUp / PointerLeave**: 清除拖动状态

### 边界限制

- 最小 x: 0（不能超出左边界）
- 最小 y: 0（不能超出上边界）
- 最大 x: 视口宽度 - 按钮宽度
- 最大 y: 视口高度 - 按钮高度 - 底部安全距离（移动端）

## 实现细节

### 核心代码结构

```tsx
// 按钮样式
const buttonStyle = {
  position: 'fixed',
  left: position.x,
  top: position.y,
  // 拖动时透明度降低
  opacity: isDragging ? 0.8 : 1,
  // 拖动时添加阴影
  boxShadow: isDragging ? '0 8px 16px rgba(0,0,0,0.2)' : '0 2px 4px rgba(0,0,0,0.1)',
  // 禁用按钮区域的触摸动作，防止滚动干扰
  touchAction: 'none',
  transition: isDragging ? 'none' : 'all 0.2s ease',
};
```

### 防误触处理（移动端）

- 设置 `touch-action: none` 防止拖动时触发页面滚动
- 拖动时临时禁用 transition，松开后恢复

### 生命周期

- 位置状态使用 `useState`，每次页面加载重置为默认位置
- 如需持久化位置，可扩展使用 localStorage

## 验收标准

1. ✅ 桌面端鼠标可以拖动按钮到任意位置
2. ✅ 移动端手指可以拖动按钮到任意位置
3. ✅ 按钮不能被拖出可视区域
4. ✅ 点击按钮仍然可以正常返回上一页
5. ✅ 拖动过程中有视觉反馈（透明度、阴影变化）
6. ✅ 松开后按钮停留在最后位置
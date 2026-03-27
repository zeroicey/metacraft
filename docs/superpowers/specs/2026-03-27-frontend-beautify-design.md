# 元创前端 UI 全面重建设计方案

> **Design System:** 精致拟物化 - 柔和渐变 + 精致阴影 + 精品质感

---

## 1. 基础规范

### 1.1 颜色系统

| 用途 | 源创（蓝） | 元梦（粉） |
|------|-----------|-----------|
| 主色 | `#007AFF` | `#EC4899` |
| 主色浅 | `#E8F0FE` | `#FDF2F8` |
| 主色深 | `#0056CC` | `#BE185D` |
| 主色渐变起 | `#007AFF` | `#EC4899` |
| 主色渐变终 | `#0056CC` | `#BE185D` |
| 背景色 | `#F0F4F8` | `#FDF2F8` |
| 卡片背景 | `#FFFFFF` | `#FFFFFF` |
| 卡片边框 | `rgba(0,122,255,0.1)` | `rgba(236,72,153,0.15)` |
| 文字主 | `#1F2937` | `#1F2937` |
| 文字次 | `#6B7280` | `#6B7280` |
| 分割线 | `#E5E7EB` | `#FCE7F3` |

### 1.2 阴影系统

```css
/* 卡片阴影 - 源创 */
--shadow-card-yuanchuang: 0 2px 8px rgba(0,122,255,0.08), 0 8px 24px rgba(0,0,0,0.06);

/* 卡片阴影 - 元梦 */
--shadow-card-yuanmeng: 0 2px 8px rgba(236,72,153,0.08), 0 8px 24px rgba(0,0,0,0.06);

/* 按钮阴影 */
--shadow-button: 0 2px 4px rgba(0,122,255,0.3), inset 0 1px 0 rgba(255,255,255,0.2);

/* 消息气泡阴影 - 源创 */
--shadow-bubble-yuanchuang: 0 2px 6px rgba(0,122,255,0.3);

/* 消息气泡阴影 - 元梦 */
--shadow-bubble-yuanmeng: 0 2px 6px rgba(236,72,153,0.3);
```

### 1.3 圆角系统

| 元素 | 圆角 |
|------|------|
| 卡片 | 16px |
| 按钮 | 12px |
| 消息气泡（用户） | 14px 14px 4px 14px |
| 消息气泡（AI） | 14px 14px 14px 4px |
| 输入框 | 12px |
| 头像 | 50% (圆形) |
| 图标按钮 | 8px |

### 1.4 间距系统

- **xs:** 4px
- **sm:** 8px
- **md:** 12px
- **lg:** 16px
- **xl:** 24px
- **2xl:** 32px
- **3xl:** 48px

---

## 2. 页面设计

### 2.1 导航栏（Navbar）

**位置：** 顶部固定

**内容：**
- 左侧：Sidebar Trigger（汉堡菜单图标）
- 中间：Tab 切换（源创 | 元梦）
- 右侧：连接状态指示 + 设置图标

**Tab 切换样式：**
```css
/* 背景 */
background: linear-gradient(175deg, #E8F0FE 0%, #F0F4F8 100%); /* 源创 */
background: linear-gradient(175deg, #FDF2F8 0%, #FEF1F7 100%); /* 元梦 */

/* 激活项 */
background: white;
box-shadow: 0 1px 3px rgba(0,0,0,0.1);

/* 文字 */
color: #007AFF; /* 源创激活 */
color: #EC4899; /* 元梦激活 */
```

**连接状态：**
- 连接中：黄色圆点 + "连接中"
- 已连接：绿色圆点 + "已连接"
- 未连接：灰色圆点 + "未连接"

### 2.2 侧边栏（Sidebar）

#### 2.2.1 源创侧边栏

**布局：**
```
┌─────────────────────┐
│  [Logo] 元创      [>] │
├─────────────────────┤
│ [+ 创建新应用]       │
├─────────────────────┤
│ 我的元应用      [>]  │
│ 元应用商店      [>]  │
│ 元数据中心      [>]  │
├─────────────────────┤
│ 会话列表            │
│ ├ 会话1             │
│ ├ 会话2             │
│ └ ...               │
├─────────────────────┤
│ [头像] 用户名       │
│               [⚙️]  │
└─────────────────────┘
```

**创建按钮样式：**
```css
background: linear-gradient(135deg, #007AFF 0%, #0056CC 100%);
color: white;
border-radius: 12px;
box-shadow: 0 2px 4px rgba(0,122,255,0.3);
```

**导航项：**
```css
/* 默认 */
background: transparent;
color: #4B5563;

/* 悬停 */
background: #F3F4F6;

/* 激活 */
background: #E8F0FE;
color: #007AFF;
```

**会话列表：**
- 选中项：左边框 3px #007AFF，背景 #E8F0FE
- 悬停：背景 #F9FAFB

#### 2.2.2 元梦侧边栏

**布局：**
```
┌─────────────────────┐
│  沙盒监控            │
│ [●] 运行中  00:05:32 │
│ CPU ████░░░░░ 32%   │
│ 内存 ██████░░ 45%   │
│ 磁盘 2.5 / 10 GB    │
│ [网络图表]           │
├─────────────────────┤
│ 知识库               │
│ 我的知识库      [>]  │
│ 技术文档        [>]  │
│ 产品FAQ         [>]  │
└─────────────────────┘
```

**沙盒监控：**
- 状态指示：绿色圆点（运行中）/ 灰色（已停止）
- 资源条：浅灰背景 + 主色填充
- 网络图表：下载绿色，上传蓝色

**知识库列表：**
```css
/* 选中项 */
background: linear-gradient(135deg, #FDF2F8 0%, #FCE7F3 100%);
border: 1.5px solid #EC4899;

/* 未选中 */
background: #F9FAFB;
border: 1px solid transparent;
```

### 2.3 源创页面（YuanChuangPage）

**消息列表区域：**
```css
background: transparent;
padding: 16px;
gap: 12px;
```

**用户消息气泡：**
```css
background: linear-gradient(135deg, #007AFF 0%, #0056CC 100%);
color: white;
border-radius: 14px 14px 4px 14px;
padding: 10px 14px;
max-width: 70%;
box-shadow: 0 2px 6px rgba(0,122,255,0.3);
```

**AI 消息气泡：**
```css
background: white;
color: #1F2937;
border-radius: 14px 14px 14px 4px;
padding: 10px 14px;
max-width: 70%;
border: 1px solid #E5E7EB;
```

**输入框区域：**
```css
background: white;
border-radius: 16px;
padding: 16px;
box-shadow: 0 2px 8px rgba(0,122,255,0.08);
```

**输入框：**
```css
background: #F9FAFB;
border: 1px solid #E5E7EB;
border-radius: 12px;
padding: 12px 16px;

/* 聚焦 */
border-color: #007AFF;
box-shadow: 0 0 0 3px rgba(0,122,255,0.1);
```

**发送按钮：**
```css
background: linear-gradient(135deg, #007AFF 0%, #0056CC 100%);
color: white;
border-radius: 12px;
width: 44px;
height: 44px;
```

### 2.4 元梦页面（YuanMengPage）

**整体风格同源创，使用粉色主题：**

**用户消息气泡：**
```css
background: linear-gradient(135deg, #EC4899 0%, #F472B6 100%);
color: white;
border-radius: 14px 14px 4px 14px;
box-shadow: 0 2px 6px rgba(236,72,153,0.3);
```

**AI 消息气泡：**
```css
background: white;
color: #1F2937;
border-radius: 14px 14px 14px 4px;
border: 1px solid #FCE7F3;
```

**输入框：**
```css
/* 聚焦 */
border-color: #EC4899;
box-shadow: 0 0 0 3px rgba(236,72,153,0.1);
```

**发送按钮：**
```css
background: linear-gradient(135deg, #EC4899 0%, #BE185D 100%);
```

### 2.5 其他页面通用规范

#### 2.5.1 我的应用（MyApps）

**页面背景：**
```css
background: linear-gradient(180deg, #F0F4F8 0%, #FFFFFF 100%);
```

**应用卡片：**
```css
background: white;
border-radius: 16px;
border: 1px solid rgba(0,122,255,0.1);
box-shadow: 0 2px 8px rgba(0,122,255,0.08);
padding: 16px;
```

**悬停效果：**
```css
transform: translateY(-2px);
box-shadow: 0 8px 24px rgba(0,122,255,0.12);
```

#### 2.5.2 应用商店（Store）

**应用卡片：**
```css
/* 选中态 */
border-color: #007AFF;
background: linear-gradient(175deg, #FFFFFF 0%, #E8F0FE 100%);
```

#### 2.5.3 个人中心（Profile）

**头像：**
```css
width: 80px;
height: 80px;
border-radius: 50%;
border: 3px solid white;
box-shadow: 0 4px 12px rgba(0,0,0,0.1);
```

---

## 3. 组件规范

### 3.1 按钮（Button）

**主要按钮：**
```css
background: linear-gradient(135deg, #007AFF 0%, #0056CC 100%); /* 源创 */
background: linear-gradient(135deg, #EC4899 0%, #BE185D 100%); /* 元梦 */
color: white;
border-radius: 12px;
padding: 10px 20px;
font-weight: 500;
```

**次要按钮：**
```css
background: white;
border: 1px solid #E5E7EB;
color: #4B5563;
border-radius: 12px;
```

**禁用状态：**
```css
opacity: 0.5;
cursor: not-allowed;
```

### 3.2 输入框（Input）

```css
background: #F9FAFB;
border: 1px solid #E5E7EB;
border-radius: 12px;
padding: 12px 16px;
transition: all 0.2s;

/* 聚焦 */
border-color: #007AFF;
box-shadow: 0 0 0 3px rgba(0,122,255,0.1);
```

### 3.3 卡片（Card）

```css
background: white;
border-radius: 16px;
border: 1px solid rgba(0,122,255,0.1);
box-shadow: 0 2px 8px rgba(0,122,255,0.08);
padding: 16px;
```

### 3.4 加载状态

```css
/* 骨架屏 */
background: linear-gradient(90deg, #F3F4F6 25%, #E5E7EB 50%, #F3F4F6 75%);
background-size: 200% 100%;
animation: shimmer 1.5s infinite;
```

---

## 4. 动画规范

### 4.1 过渡动画

```css
/* 默认过渡 */
transition: all 0.2s ease;

/* 按钮悬停 */
transition: all 0.15s ease-out;

/* 页面切换 */
transition: all 0.3s ease-out;
```

### 4.2 动画效果

- **按钮点击：** scale(0.98)
- **卡片悬停：** translateY(-2px)
- **Tab 切换：** 滑动背景 + 颜色渐变
- **消息出现：** fadeIn + slideUp

---

## 5. 响应式断点

- **Mobile:** < 640px
- **Tablet:** 640px - 1024px
- **Desktop:** > 1024px

---

## 6. 验收标准

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
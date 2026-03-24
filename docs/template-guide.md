# MetaCraft 预生成模板规范

## 1. 概述

本文档定义了 MetaCraft 预生成模板的创建规范，用于模板匹配系统（Template Matching）。

## 2. 模板目录结构

```
{应用名}_{应用描述}/
├── index.html      # 必需：入口文件
├── app.js          # 必需：主 JS 文件
├── xxx.js          # 可选：其他 JS 文件
└── xxx.css         # 可选：CSS 文件
```

## 3. 命名规则

- **格式**：`应用名_应用描述`
- **分隔符**：使用单个下划线 `_`
- **示例**：
  - `待办工具_一个简易的待办工具`
  - `计算器_基础数学计算器`
  - `博客系统_个人博客主页`
  - `五子棋_经典双人棋盘游戏`

## 4. 技术栈要求

### 4.1 必需引入

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>应用标题</title>

  <!-- Bootstrap Icons 图标库 -->
  <link rel="stylesheet" href="/public/css/bootstrap-icons.css">

  <!-- Tailwind CSS 样式框架 -->
  <script src="/public/css/tailwind.js"></script>
</head>
<body>
  <div id="app"></div>

  <!-- Vue.js 运行时 -->
  <script src="/public/js/vue.js"></script>

  <!-- 应用 JS -->
  <script src="./app.js"></script>
</body>
</html>
```

### 4.2 可用资源

| 资源 | 路径 | 说明 |
|------|------|------|
| Vue.js | `/public/js/vue.js` | 全局运行时版本 |
| Tailwind CSS | `/public/css/tailwind.js` | 实时编译版本 |
| Bootstrap Icons | `/public/css/bootstrap-icons.css` | 字体图标库 |

### 4.3 Bootstrap Icons 常用图标

```html
<!-- 基础图标 -->
<i class="bi bi-house"></i>           <!-- 首页 -->
<i class="bi bi-search"></i>            <!-- 搜索 -->
<i class="bi bi-gear"></i>             <!-- 设置 -->
<i class="bi bi-person"></i>           <!-- 用户 -->
<i class="bi bi-cart"></i>            <!-- 购物车 -->
<i class="bi bi-heart"></i>            <!-- 收藏 -->
<i class="bi bi-chat"></i>            <!-- 聊天 -->
<i class="bi bi-image"></i>           <!-- 图片 -->
<i class="bi bi-upload"></i>           <!-- 上传 -->
<i class="bi bi-download"></i>         <!-- 下载 -->

<!-- 操作图标 -->
<i class="bi bi-trash"></i>            <!-- 删除 -->
<i class="bi bi-pencil"></i>           <!-- 编辑 -->
<i class="bi bi-plus-lg"></i>          <!-- 添加 -->
<i class="bi bi-check-lg"></i>         <!-- 完成 -->
<i class="bi bi-x-lg"></i>             <!-- 关闭 -->
<i class="bi bi-arrow-left"></i>       <!-- 返回 -->
<i class="bi bi-arrow-right"></i>      <!-- 前进 -->

<!-- 状态图标 -->
<i class="bi bi-star-fill"></i>        <!-- 星级 -->
<i class="bi bi-bell"></i>             <!-- 通知 -->
<i class="bi bi-envelope"></i>         <!-- 邮件 -->
<i class="bi bi-check2-square"></i>     <!-- 待办勾选框 -->
<i class="bi bi-stars"></i>            <!-- 特色 -->
```

完整图标列表：https://icons.getbootstrap.com/

### 4.4 Vue.js 使用规范

```javascript
const { createApp, ref, reactive, computed, onMounted, watch } = Vue;

createApp({
  setup() {
    // 响应式数据
    const message = ref('Hello World');
    const items = ref([]);
    const form = reactive({ name: '', email: '' });

    // 计算属性
    const filteredItems = computed(() =>
      items.value.filter(item => item.done)
    );

    // 方法
    const addItem = () => { /* ... */ };
    const removeItem = (index) => { /* ... */ };

    // 生命周期
    onMounted(() => {
      console.log('组件挂载完成');
    });

    return { message, items, form, filteredItems, addItem, removeItem };
  },
  template: `
    <div class="container">
      <h1>{{ message }}</h1>
      <ul>
        <li v-for="item in items" :key="item.id">
          {{ item.name }}
        </li>
      </ul>
    </div>
  `
}).mount('#app');
```

### 4.5 Tailwind CSS 常用类

```html
<!-- 布局 -->
<div class="container mx-auto p-4"></div>
<div class="flex items-center justify-between"></div>
<div class="grid grid-cols-3 gap-4"></div>
<div class="min-h-screen"></div>

<!-- 颜色 -->
<div class="bg-blue-500 text-white"></div>
<div class="text-red-600"></div>
<div class="bg-gray-100"></div>

<!-- 间距 -->
<div class="m-4 p-4 mt-2 mb-2"></div>
<div class="space-y-4"></div>

<!-- 圆角和阴影 -->
<div class="rounded-lg shadow-md"></div>
<div class="rounded-full"></div>
<div class="rounded-[32px]"></div>

<!-- 响应式 -->
<div class="md:flex hidden"></div>
<div class="w-full md:w-1/2"></div>
```

完整文档：https://tailwindcss.com/docs

## 5. 禁止事项

| 禁止项 | 说明 |
|--------|------|
| ❌ 创建子目录 | 模板目录必须是扁平结构 |
| ❌ 引用本地图片 | png, jpg, gif, svg 等不支持 |
| ❌ 引用本地字体 | woff2, ttf, eot 等不支持 |
| ❌ 使用 npm | 不允许安装外部依赖 |
| ❌ 使用打包工具 | webpack, vite 等不支持 |
| ❌ 危险文件类型 | .sh, .bat, .cmd, .exe, .ps1 |

## 6. 图片处理方案

如需图片，请使用以下方案之一：

- **Base64 内联**：`data:image/png;base64,xxxxx`
- **CDN 外链**：Cloudflare Images、ImgBB 等
- **SVG 内联**：直接写在 HTML 中

## 7. 允许的文件类型

```
.html, .js, .css, .json, .vue, .png, .jpg, .jpeg, .svg, .gif, .woff2
```

## 8. 模板示例

### 示例：待办工具_简易待办应用

**目录结构：**
```
待办工具_简易待办应用/
├── index.html
└── app.js
```

**index.html：**
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>待办工具</title>
  <link rel="stylesheet" href="/public/css/bootstrap-icons.css">
  <script src="/public/css/tailwind.js"></script>
</head>
<body class="bg-gray-100 min-h-screen">
  <div id="app"></div>
  <script src="/public/js/vue.js"></script>
  <script src="./app.js"></script>
</body>
</html>
```

**app.js：**
```javascript
const { createApp, ref, computed } = Vue;

createApp({
  setup() {
    const newTask = ref('');
    const tasks = ref([
      { id: 1, text: '欢迎使用待办工具', done: false },
      { id: 2, text: '点击添加新任务', done: true }
    ]);

    const addTask = () => {
      if (newTask.value.trim()) {
        tasks.value.push({
          id: Date.now(),
          text: newTask.value.trim(),
          done: false
        });
        newTask.value = '';
      }
    };

    const removeTask = (id) => {
      tasks.value = tasks.value.filter(t => t.id !== id);
    };

    const remaining = computed(() => tasks.value.filter(t => !t.done).length);

    return { newTask, tasks, addTask, removeTask, remaining };
  },
  template: `
    <main class="container mx-auto p-4 max-w-lg">
      <h1 class="text-3xl font-bold mb-6 flex items-center gap-2">
        <i class="bi bi-check2-square"></i> 待办工具
      </h1>

      <div class="flex gap-2 mb-4">
        <input v-model="newTask" @keyup.enter="addTask"
               class="flex-1 px-4 py-2 rounded border focus:outline-none focus:ring-2 focus:ring-blue-500"
               placeholder="添加新任务...">
        <button @click="addTask"
                class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">
          <i class="bi bi-plus-lg"></i>
        </button>
      </div>

      <ul class="space-y-2">
        <li v-for="task in tasks" :key="task.id"
            class="bg-white p-4 rounded shadow flex items-center gap-3">
          <input type="checkbox" v-model="task.done" class="w-5 h-5">
          <span :class="{ 'line-through text-gray-400': task.done }" class="flex-1">
            {{ task.text }}
          </span>
          <button @click="removeTask(task.id)" class="text-red-500 hover:text-red-700">
            <i class="bi bi-trash"></i>
          </button>
        </li>
      </ul>

      <p class="mt-4 text-gray-600">共 {{ remaining }} 项待完成</p>
    </main>
  `
}).mount('#app');
```

## 9. 模板匹配流程

```
用户请求 → Intent 分类 (GEN)
    ↓
TemplateMatcherService.matchTemplate(userMessage)
    ↓
扫描 templates/ 目录 → 获取模板列表
    ↓
调用 AI (TemplateMatcherAgent) 匹配
    ↓
返回模板名称 或 NONE
    ↓
匹配成功 → 复制模板文件到应用目录
匹配失败 → 回退到 OpenCode 生成
```

详细设计见：[2026-03-23-template-matching-design.md](./superpowers/specs/2026-03-23-template-matching-design.md)

## 10. 创建新模板 Checklist

- [ ] 创建目录 `{应用名}_{应用描述}/`
- [ ] 创建 `index.html`，引入必需的公共资源
- [ ] 创建 `app.js`，使用 Vue.js Composition API
- [ ] 确保没有引用本地图片/字体文件
- [ ] 确保没有创建子目录
- [ ] 验证模板可以正常运行
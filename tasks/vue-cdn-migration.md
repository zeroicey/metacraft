# 任务：迁移代码生成 - 从纯 JS 到 Vue CDN

**状态：** 进行中 🚧
**创建日期：** 2026-03-10

## 背景

当前 AI 生成的应用使用纯 JavaScript + Tailwind CSS，需要迁移到 Vue 3 CDN 模式。

## 涉及文件

- `apps/api/src/main/resources/prompts/gen-code.txt` - 生成代码的 prompt
- `apps/api/src/main/resources/prompts/edit-code.txt` - 编辑代码的 prompt

## 任务清单

- [x] 更新 `gen-code.txt` prompt，要求生成 Vue 3 CDN 格式的代码
- [x] 更新 `edit-code.txt` prompt，要求输出兼容 Vue 运行时的 HTML/JS patch
- [x] 确认 Vue 3 CDN 引入方式 (jsdelivr，固定版本下载到本地静态资源)
- [x] 调整代码结构要求：保持 HTML + JS 双文件，迁移为 Vue 3 CDN 运行时模式
- [ ] 测试生成流程是否正常工作
- [ ] 测试编辑流程是否正常工作

## 技术细节

```html
<!-- 本地 Vue 3 运行时引入示例 -->
<script src="/public/js/vue.js"></script>

<div id="app">{{ message }}</div>

<script src="app.js"></script>
```

```javascript
const { createApp, ref } = Vue;

createApp({
  setup() {
    const message = ref('Hello vue!');
    return {
      message,
    };
  },
}).mount('#app');
```

**备注：**
- 保持 Tailwind CSS 本地服务器引入方式不变
- 当前预览链路固定为 `index.html + app.js`，不适合迁移为 Vue SFC
- 已下载本地运行时：`/public/js/vue.js`

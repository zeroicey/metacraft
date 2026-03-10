# 静态资源目录

此目录包含可通过HTTP访问的静态资源文件。

## Tailwind CSS v4

文件位置: `css/tailwind.js`

版本: Tailwind CSS v4.2.1 (Browser Build)

## Vue 3

文件位置: `js/vue.js`

版本: Vue v3.5.30 (Global Production Build)

## 访问方式

启动服务器后，可以通过以下URL访问：

```
http://localhost:8080/public/css/tailwind.js
http://localhost:8080/public/js/vue.js
```

## 使用方法

在HTML中引用本地的 Tailwind CSS 和 Vue 3：

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <script src="/public/css/tailwind.js"></script>
    <script src="/public/js/vue.js"></script>
</head>
<body>
    <div id="app" class="p-6">
        <h1 class="text-3xl font-bold text-blue-600">{{ message }}</h1>
    </div>
    <script>
        const { createApp, ref } = Vue;

        createApp({
            setup() {
                const message = ref('Hello Tailwind CSS v4 & Vue 3!');
                return { message };
            }
        }).mount('#app');
    </script>
</body>
</html>
```

**重要说明**：
- 在 MetaCraft 生成的应用中，应使用相对路径 `/public/...` 而不是完整的 URL
- 生成应用时，推荐使用本地 Vue 运行时 `/public/js/vue.js`，避免依赖外网 CDN

## 配置说明

- **WebMvcConfig**: 配置了 `/public/**` 路径映射到 `classpath:/public/` 目录
- **SecurityConfig**: 允许匿名访问 `/public/**` 路径
- **文件来源**:
  - Tailwind CSS: https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4
    - Vue 3: https://cdn.jsdelivr.net/npm/vue@3.5.30/dist/vue.global.prod.js

## 添加更多静态资源

将文件放置在此目录或其子目录中，它们将自动可以通过 `/public/` 路径访问。

例如：
- `public/js/app.js` → `http://localhost:8080/public/js/app.js`
- `public/css/style.css` → `http://localhost:8080/public/css/style.css`
- `public/images/logo.png` → `http://localhost:8080/public/images/logo.png`

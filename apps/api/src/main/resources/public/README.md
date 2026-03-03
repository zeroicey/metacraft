# 静态资源目录

此目录包含可通过HTTP访问的静态资源文件。

## Tailwind CSS v4

文件位置: `css/tailwind.js`

版本: Tailwind CSS v4.2.1 (Browser Build)

## Alpine.js v3

文件位置: `js/alpine.js`

版本: Alpine.js v3.14.1

## 访问方式

启动服务器后，可以通过以下URL访问：

```
http://localhost:8080/public/css/tailwind.js
http://localhost:8080/public/js/alpine.js
```

## 使用方法

在HTML中引用本地的 Tailwind CSS 和 Alpine.js：

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <script src="/public/css/tailwind.js"></script>
    <script defer src="/public/js/alpine.js"></script>
</head>
<body>
    <div x-data="{ count: 0 }">
        <h1 class="text-3xl font-bold text-blue-600">
            Hello Tailwind CSS v4 & Alpine.js!
        </h1>
        <button @click="count++" class="px-4 py-2 bg-blue-500 text-white rounded">
            点击次数: <span x-text="count"></span>
        </button>
    </div>
</body>
</html>
```

**重要说明**：
- 在 MetaCraft 生成的应用中，应使用相对路径 `/public/...` 而不是完整的 URL
- Alpine.js 需要使用 `defer` 属性以确保在 DOM 加载后执行

## 配置说明

- **WebMvcConfig**: 配置了 `/public/**` 路径映射到 `classpath:/public/` 目录
- **SecurityConfig**: 允许匿名访问 `/public/**` 路径
- **文件来源**:
  - Tailwind CSS: https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4
  - Alpine.js: https://cdn.jsdelivr.net/npm/alpinejs@3.14.1/dist/cdn.min.js

## 添加更多静态资源

将文件放置在此目录或其子目录中，它们将自动可以通过 `/public/` 路径访问。

例如：
- `public/js/app.js` → `http://localhost:8080/public/js/app.js`
- `public/css/style.css` → `http://localhost:8080/public/css/style.css`
- `public/images/logo.png` → `http://localhost:8080/public/images/logo.png`

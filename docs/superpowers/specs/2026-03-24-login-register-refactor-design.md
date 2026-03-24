# 登录注册重构设计文档

## 1. 概述

将现有的硬编码弹窗登录改为独立的路由页面，实现完整的注册和登录流程。

## 2. 目标

- 移除硬编码账号密码
- 创建独立的 `/login` 和 `/register` 路由页面
- 401/403 时自动跳转到登录页
- 注册成功后自动登录并跳转首页
- 登录/注册页面可相互跳转

## 3. 设计系统

### 3.1 样式
- **风格**: Motion-Driven (动画驱动，微交互)
- **字号**: 基础 16px，行高 1.5
- **字体**: Plus Jakarta Sans (已在项目中使用)

### 3.2 颜色
| 用途 | 颜色 |
|------|------|
| Primary | #2563EB (Indigo) |
| Secondary | #3B82F6 |
| CTA | #F97316 (Orange) |
| Background | #F8FAFC |
| Text | #1E293B |
| Error | #EF4444 |

### 3.3 响应式断点
| 断点 | 宽度 |
|------|------|
| Mobile | < 640px |
| Tablet | 640-1024px |
| Desktop | > 1024px |

## 4. 页面设计

### 4.1 登录页 (/login)

**布局:**
- 居中卡片，宽度 max-w-sm (384px)
- 移动端全屏，边距 px-4

**字段:**
| 字段 | 类型 | 验证 |
|------|------|------|
| 邮箱 | email | 必填，邮箱格式 |
| 密码 | password | 必填 |

**交互:**
- 登录按钮：加载时显示 spinner，禁用输入
- 错误提示：显示在表单顶部
- 跳转链接：还没有账号？去注册

### 4.2 注册页 (/register)

**布局:** 同登录页

**字段:**
| 字段 | 类型 | 验证 |
|------|------|------|
| 邮箱 | email | 必填，邮箱格式 |
| 密码 | password | 必填，最少6位 |
| 确认密码 | password | 必填，需匹配 |
| 姓名 | text | 必填 |
| 简介 | textarea | 必填 |

**交互:**
- 密码显示/隐藏切换
- 实时密码匹配验证
- 提交后自动登录

### 4.3 响应式样式

```css
/* Mobile */
.px-4
.w-full max-w-sm

/* Tablet/Desktop */
.px-0
.mx-auto
```

## 5. 技术实现

### 5.1 文件改动

| 操作 | 文件 |
|------|------|
| 修改 | `apps/web/src/lib/http.ts` |
| 重构 | `apps/web/src/stores/auth-store.ts` |
| 删除 | `apps/web/src/components/auth/login-drawer.tsx` |
| 新增 | `apps/web/src/pages/login.tsx` |
| 新增 | `apps/web/src/pages/register.tsx` |
| 修改 | 路由配置文件 |

### 5.2 Auth Store 接口

```typescript
interface AuthStore {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  // Actions
  login: (email: string, password: string) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => void;
  initialize: () => void;
}
```

### 5.3 HTTP 拦截器

```typescript
// 401/403 时跳转登录页
if (response.status === 401 || response.status === 403) {
  window.location.href = '/login'
}
```

## 6. 验收标准

- [ ] 访问需要认证的接口时自动跳转到 /login
- [ ] 登录页面可正常登录
- [ ] 注册页面可正常注册
- [ ] 注册成功后自动登录并跳转首页
- [ ] 登录/注册页面可相互跳转
- [ ] 响应式布局在移动端和桌面端正常工作
- [ ] 登录成功后 token 存储到 localStorage
- [ ] 登出功能正常
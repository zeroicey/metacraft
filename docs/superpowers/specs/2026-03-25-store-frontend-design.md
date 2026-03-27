# 元应用商店前端设计

## 概述

为 MetaCraft 添加"元应用商店"功能的前端页面，支持用户浏览、评分、评论已发布应用。

## 页面结构

### 页面1: 商店首页 (`/store`)

**路由:** `/store`

**功能:**
- 展示所有已发布（isPublic=true）的应用
- 应用卡片显示：Logo、名称、评分、作者信息

**布局:**
- 使用 StandaloneLayout
- Header: 返回按钮 + 标题"元应用商店"
- 内容区: 2列网格布局的应用卡片

**应用卡片内容:**
- 应用 Logo (base64 或占位图)
- 应用名称 (最多2行截断)
- 评分: 星级 + 数字 (如 ★ 4.5 (10))
- 作者: 头像 + 名称

**空状态:**
- 显示"暂无发布的应用"

---

### 页面2: 应用详情页 (`/store/:id`)

**路由:** `/store/:id`

**功能:**
- 查看应用详情
- 预览应用 (iframe)
- 评分功能 (1-5星，选择后确认提交)
- 评论功能 (发表、查看、删除自己的评论)

**布局:**
- 使用 StandaloneLayout
- Header: 返回按钮 + 分享按钮

**内容区域:**

1. **应用信息区**
   - Logo + 名称
   - 评分展示 (★ 4.5 (10人评分))
   - 作者信息 (头像 + 名称)

2. **应用预览区**
   - iframe 嵌入应用预览
   - 可交互

3. **评分区 (需要登录)**
   - 点击星星选择评分 (1-5)
   - 选择后显示"确认提交"按钮
   - 提交后更新显示的平均分和评分数量

4. **评论输入区 (需要登录)**
   - 多行文本输入框
   - "发布"按钮

5. **评论列表区**
   - 显示所有评论 (按时间倒序)
   - 每条评论显示: 用户名、评分、内容、时间
   - 自己发布的评论显示"删除"按钮

---

## API 接口

### 现有后端接口 (已实现)

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | /api/store/apps | 获取商店应用列表 |
| GET | /api/store/apps/{id} | 获取应用详情（含评论） |
| POST | /api/store/apps/{id}/publish | 发布应用（需要认证） |
| DELETE | /api/store/apps/{id}/publish | 下架应用（需要认证） |
| POST | /api/store/apps/{id}/ratings | 提交评分（需要认证） |
| POST | /api/store/apps/{id}/comments | 提交评论（需要认证） |
| DELETE | /api/store/apps/{id}/comments/{commentId} | 删除评论（需要认证） |

---

## 前端文件结构

```
apps/web/src/
├── api/
│   └── store.ts              # 新增: 商店 API
├── hooks/
│   └── useStore.ts          # 新增: 商店 Hooks
├── stores/
│   └── (复用现有)           # 无需新增 store
├── pages/
│   ├── store.tsx            # 新增: 商店首页
│   └── store-detail.tsx     # 新增: 应用详情页
└── components/
    └── store/
        ├── StoreAppCard.tsx    # 应用卡片组件
        ├── StarRating.tsx      # 评分组件
        └── CommentList.tsx     # 评论列表组件
```

---

## 实现任务

1. 创建 `api/store.ts` - 商店 API 接口
2. 创建 `hooks/useStore.ts` - 商店数据 hooks
3. 创建 `components/store/StoreAppCard.tsx` - 应用卡片
4. 创建 `components/store/StarRating.tsx` - 评分组件
5. 创建 `components/store/CommentList.tsx` - 评论列表
6. 创建 `pages/store.tsx` - 商店首页
7. 创建 `pages/store-detail.tsx` - 应用详情页
8. 更新路由 `router.ts` - 添加商店路由
9. 更新侧边栏 `sidebar-content.tsx` - 修复"元应用商店"跳转

---

## 交互流程

### 商店首页 → 详情页
1. 用户点击应用卡片
2. 跳转到 `/store/{appId}`
3. 加载应用详情数据

### 评分流程
1. 用户点击星星选择评分 (1-5)
2. 显示"确认提交"按钮
3. 点击提交，调用 POST /api/store/apps/{id}/ratings
4. 成功后更新显示的平均分

### 评论流程
1. 用户输入评论内容
2. 点击"发布"按钮
3. 调用 POST /api/store/apps/{id}/comments
4. 成功后添加到评论列表

### 删除评论
1. 用户点击自己的评论上的"删除"
2. 调用 DELETE /api/store/apps/{id}/comments/{commentId}
3. 成功后从列表移除

---

## 设计规范

- 使用 shadcn/ui 组件
- 颜色: 主色 #007AFF (蓝色)
- 卡片圆角: 12px
- 间距: 8px 基准
- 响应式: 移动端优先
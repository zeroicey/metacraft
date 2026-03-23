# Web 端"我的元应用"页面设计

## 概述

为 Web 端创建"我的元应用"页面，功能参考华为 HarmonyOS 版本的 MyAppsScreen。

## 页面入口

- 独立页面，路由：`/myapps`
- 从侧边栏"我的元应用"菜单进入

## 功能需求

### 1. 头部
- 返回按钮（左箭头）
- 标题："我的元应用"
- 创建按钮（+ 图标，右上角）

### 2. 加载状态
- 显示 LoadingProgress 加载动画
- 显示文字"加载中..."

### 3. 空状态
- 显示 emoji 📱
- 标题："还没有应用"
- 副标题："通过对话创建你的第一个元应用吧"
- "去创建"按钮，点击返回上一页

### 4. 应用列表
- List 布局，间距 12px
- 每行一个应用卡片

### 5. 应用卡片 (AppItem)
- Logo：56x56，圆角 14px
  - 有 logo 图片时显示图片
  - 无 logo 时显示首字母 + 渐变背景（根据名称生成颜色）
- 应用名称：17px，中等粗细
- 版本号：如 "v1"，蓝色
- 描述：14px，灰色，最多1行
- 时间信息：
  - "创建于 X"
  - 如有更新显示 "· 更新 X"
  - 如有多个版本显示 "· X个版本"
- 操作菜单按钮（⋮）

### 6. 创建应用对话框
- 标题："新建应用"
- 应用名称输入框
- 应用描述输入框（可选）
- 取消 / 创建 按钮

## 技术方案

### API 接口
- `GET /api/apps` - 获取用户应用列表（已存在）
- `POST /api/apps` - 创建应用
- `DELETE /api/apps/{id}` - 删除应用

### 路由配置
```typescript
// router.ts
const MyAppsPage = lazy(() => import("@/pages/myapps"));
// 添加路由
{ path: "myapps", Component: MyAppsPage }
```

### 数据类型
```typescript
interface AppVersion {
  id: number;
  versionNumber: number;
  storagePath: string;
  changeLog: string;
  createdAt: string;
}

interface App {
  id: number;
  uuid: string;
  name: string;
  description: string;
  logo?: string;
  isPublic: boolean;
  currentVersionId: number | null;
  versions: AppVersion[];
  createdAt: string;
  updatedAt: string;
}
```

### 文件结构
```
apps/web/src/
├── api/
│   └── app.ts              # 应用 API 接口
├── types/
│   └── app.ts              # 应用类型定义
├── components/
│   └── app/
│       ├── AppItem.tsx     # 应用卡片组件
│       └── AppActionMenu.tsx  # 操作菜单
├── hooks/
│   └── useApps.ts          # 应用相关 hooks
└── pages/
    └── myapps.tsx          # 我的元应用页面
```

## UI 样式

- 背景色：`#F5F7FA`
- 卡片背景：白色，圆角 16px
- 主色调：`#007AFF`
- 文字颜色：
  - 主标题：$r('sys.color.ohos_id_color_text_primary') 等价 `#1f1f1f`
  - 副标题：`#8E8E93`
  - 时间：`#C7C7CC`
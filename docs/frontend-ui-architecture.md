# MetaCraft 前端 UI 页面与组件架构方案

## 1. 目标

这份方案只解决一件事：以后在 HarmonyOS 前端里新增页面、组件、接口、模型时，能够快速判断“应该放哪里”。

当前目录虽然已经做了基础分层，但页面、业务组件、通用组件、接口封装混在同一层级里，容易出现几个问题：

1. 新页面不知道应该挂在哪个模块下。
2. 组件复用边界不清楚，容易把一次性组件写进 components 根目录。
3. 页面逻辑容易变重，既管布局又管请求又管消息转换。
4. 同一个业务的代码分散在 pages、components、api、model 多处，维护时上下文切换成本高。

新的方案采用一种更适合当前项目的混合结构：页面入口单独保留，业务代码按领域收敛，共享能力统一沉到 shared 和 core。

## 2. 设计原则

1. pages 只放路由页面，不放可复用业务组件。
2. 一个功能域的组件、类型、接口尽量放在同一个 feature 下。
3. 只有跨多个业务域复用的组件，才进入 shared。
4. 网络、路由、鉴权、存储这类全局基础设施，统一放在 core。
5. 页面负责组装，不负责承载过多细节逻辑。

## 3. 推荐目录结构

建议将 apps/huawei/entry/src/main/ets 逐步调整为下面这套结构：

```text
ets/
|- app/
|  |- AppRoutes.ets
|  |- AppConstants.ets
|
|- pages/
|  |- workspace/
|  |  |- WorkspacePage.ets
|  |- apps/
|  |  |- MyAppsPage.ets
|  |  |- AppStorePage.ets
|  |  |- PreviewPage.ets
|  |- data/
|  |  |- DataCenterPage.ets
|  |- user/
|  |  |- ProfilePage.ets
|  |  |- SettingsPage.ets
|
|- features/
|  |- auth/
|  |  |- components/
|  |  |- service/
|  |  |- model/
|  |  |- types.ets
|  |
|  |- session/
|  |  |- components/
|  |  |  |- SessionSidebar.ets
|  |  |  |- SessionList.ets
|  |  |- service/
|  |  |  |- SessionService.ets
|  |  |- model/
|  |  |- types.ets
|  |
|  |- chat/
|  |  |- components/
|  |  |  |- ChatPanel.ets
|  |  |  |- MessageList.ets
|  |  |  |- MessageItem.ets
|  |  |  |- MessageComposer.ets
|  |  |  |- GenMessageCard.ets
|  |  |  |- AppPlanCard.ets
|  |  |  |- MessageMarkdownCard.ets
|  |  |- service/
|  |  |  |- ChatStreamService.ets
|  |  |- adapter/
|  |  |  |- ChatMessageAdapter.ets
|  |  |- model/
|  |  |- types.ets
|  |
|  |- app-center/
|  |  |- components/
|  |  |  |- AppItem.ets
|  |  |  |- AppInfoCard.ets
|  |  |  |- AppActionMenu.ets
|  |  |  |- AppWebPreviewCard.ets
|  |  |- service/
|  |  |  |- AppService.ets
|  |  |- model/
|  |  |- types.ets
|
|- shared/
|  |- ui/
|  |  |- BasePageHeader.ets
|  |  |- EmptyState.ets
|  |  |- LoadingState.ets
|  |- layout/
|  |  |- BottomSheetMenu.ets
|  |- feedback/
|  |- markdown/
|  |- utils/
|  |- types/
|
|- core/
|  |- http/
|  |  |- ApiConfig.ets
|  |  |- HttpManager.ets
|  |  |- Interceptors.ets
|  |  |- SSEClient.ets
|  |- auth/
|  |- router/
|  |- storage/
```

## 4. 分层职责

### 4.1 pages

pages 只负责页面级事情：

1. 接收路由参数。
2. 组织页面布局。
3. 挂载一个或多个 feature 组件。
4. 处理少量页面级生命周期。

pages 不应该承担这些职责：

1. 直接拼装复杂消息对象。
2. 承担大段业务状态机。
3. 放可被别的页面复用的业务组件。

一个页面文件最好能控制在“看起来就是页面壳子”的程度。

### 4.2 features

features 是这套方案的核心。每个 feature 对应一个业务域，内部自己管理：

1. 该业务自己的组件。
2. 该业务自己的 API/service。
3. 该业务自己的模型和类型。
4. 该业务自己的数据转换逻辑。

例如 chat 相关代码都应该尽量收敛进 features/chat，而不是一部分散在 components/chat，一部分散在 api，一部分散在 model。

### 4.3 shared

shared 放跨业务域复用的通用能力，例如：

1. 通用页面头部。
2. 通用空状态。
3. 通用加载态。
4. 通用底部菜单。
5. 与具体业务无关的 markdown 渲染封装。

判断标准很简单：
如果一个组件去掉业务文案后，仍然可以在两个以上 feature 中复用，它才适合进入 shared。

### 4.4 core

core 放全局基础设施：

1. HttpManager。
2. SSEClient。
3. Interceptors。
4. ApiConfig。
5. 全局鉴权、全局路由、全局常量。

core 不应该放业务组件。

## 5. 新页面、新组件到底放哪里

这是最重要的落地规则。

### 5.1 新增页面

如果它是一个能被 router.pushUrl 直接跳转的页面，就一定放 pages。

示例：

1. 我的收藏页，放 pages/apps/FavoritesPage.ets。
2. 应用详情页，放 pages/apps/AppDetailPage.ets。
3. 会话管理页，放 pages/workspace/SessionManagePage.ets。

页面内部如果有复杂内容，不要继续把所有 UI 都堆在这个 page 文件里，而是把主体区域拆给 feature 组件。

### 5.2 新增组件

先问自己两个问题。

问题一：这个组件是不是只服务于某一个业务域。

如果答案是“是”，放对应 feature/components。

示例：

1. 会话重命名弹窗，放 features/session/components/SessionRenameDialog.ets。
2. 生成结果消息卡片，放 features/chat/components/GenMessageCard.ets。
3. 应用版本切换面板，放 features/app-center/components/AppVersionSheet.ets。

问题二：这个组件是不是跨多个业务域复用。

如果答案是“是”，放 shared。

示例：

1. 通用顶部栏，放 shared/ui/BasePageHeader.ets。
2. 通用空页面提示，放 shared/ui/EmptyState.ets。
3. 通用 loading 区块，放 shared/ui/LoadingState.ets。

### 5.3 新增接口封装

业务接口不要继续统一堆在 api 根目录，而应优先跟随 feature。

示例：

1. 会话相关接口，放 features/session/service/SessionService.ets。
2. 聊天流式接口，放 features/chat/service/ChatStreamService.ets。
3. 应用管理接口，放 features/app-center/service/AppService.ets。

只有底层 http 请求能力才放 core/http。

### 5.4 新增类型与模型

类型同样优先跟业务走。

1. 只给 chat 用的类型，放 features/chat/types.ets。
2. 只给 session 用的模型，放 features/session/model。
3. 多个 feature 共用的简单类型，放 shared/types。

不要为了“整齐”把所有类型重新集中到一个 model 根目录，否则业务上下文还是会被打散。

## 6. 推荐依赖方向

依赖方向应该保持单向：

```text
pages -> features -> shared
pages -> features -> core
features -> shared
features -> core
shared -> core
```

不建议出现这些依赖：

1. shared 反向依赖 feature。
2. 一个 feature 直接深度依赖另一个 feature 的内部文件。
3. pages 直接依赖太多 core 细节。

如果确实需要跨 feature 复用，优先提炼到 shared，或者通过 feature 暴露统一出口文件。

## 7. 当前代码到新架构的映射

下面这张表是现有代码与目标结构的建议映射。

| 当前文件 | 建议目标位置 | 说明 |
| --- | --- | --- |
| apps/huawei/entry/src/main/ets/pages/Index.ets | pages/workspace/WorkspacePage.ets | 主工作台页面 |
| apps/huawei/entry/src/main/ets/components/sidebar/Index.ets | features/session/components/SessionSidebar.ets | 会话侧边栏 |
| apps/huawei/entry/src/main/ets/components/sidebar/SessionList.ets | features/session/components/SessionList.ets | 会话列表 |
| apps/huawei/entry/src/main/ets/components/chat/ChatPanel.ets | features/chat/components/ChatPanel.ets | 聊天业务主容器 |
| apps/huawei/entry/src/main/ets/components/chat/MessageItem.ets | features/chat/components/MessageItem.ets | 消息单元 |
| apps/huawei/entry/src/main/ets/components/chat/GenMessageCard.ets | features/chat/components/GenMessageCard.ets | 生成消息组合卡 |
| apps/huawei/entry/src/main/ets/components/chat/AppPlanCard.ets | features/chat/components/AppPlanCard.ets | 生成计划展示 |
| apps/huawei/entry/src/main/ets/components/chat/MessageMarkdownCard.ets | shared/markdown/MarkdownCard.ets 或 features/chat/components/MessageMarkdownCard.ets | 如果仅聊天使用就留在 chat |
| apps/huawei/entry/src/main/ets/components/app/AppItem.ets | features/app-center/components/AppItem.ets | 应用列表卡片 |
| apps/huawei/entry/src/main/ets/components/app/AppActionMenu.ets | features/app-center/components/AppActionMenu.ets | 应用操作菜单 |
| apps/huawei/entry/src/main/ets/components/app/AppInfoCard.ets | features/app-center/components/AppInfoCard.ets | 应用信息卡 |
| apps/huawei/entry/src/main/ets/components/app/AppWebPreviewCard.ets | features/app-center/components/AppWebPreviewCard.ets | 应用预览卡 |
| apps/huawei/entry/src/main/ets/components/BottomSheetMenu.ets | shared/layout/BottomSheetMenu.ets | 通用布局组件 |
| apps/huawei/entry/src/main/ets/api/aiChat.ets | features/chat/service/ChatStreamService.ets | 聊天流能力 |
| apps/huawei/entry/src/main/ets/api/chatSession.ets | features/session/service/SessionService.ets | 会话服务 |
| apps/huawei/entry/src/main/ets/api/appApi.ets | features/app-center/service/AppService.ets | 应用服务 |
| apps/huawei/entry/src/main/ets/model/chatSessionModel.ets | features/session/types.ets | 会话类型 |
| apps/huawei/entry/src/main/ets/model/appModel.ets | features/app-center/types.ets | 应用类型 |
| apps/huawei/entry/src/main/ets/utils/http/* | core/http/* | 全局基础设施 |

## 8. 具体到你当前项目的页面组织方案

建议把页面分成 4 组：

### 8.1 workspace 工作台

承载主聊天工作流。

1. WorkspacePage
2. SessionManagePage
3. ChatHistoryPage

### 8.2 apps 应用中心

承载应用列表、应用详情、应用预览。

1. MyAppsPage
2. AppStorePage
3. AppDetailPage
4. PreviewPage

### 8.3 data 数据中心

承载统计、分析、版本与运行数据。

1. DataCenterPage
2. AppAnalyticsPage
3. VersionHistoryPage

### 8.4 user 用户中心

承载个人资料、设置、账号能力。

1. ProfilePage
2. SettingsPage
3. AccountSecurityPage

## 9. 页面内部推荐写法

一个 page 文件建议遵循这个结构：

1. 路由参数读取。
2. 页面级状态。
3. aboutToAppear 等生命周期。
4. 页面布局。
5. 将主要内容交给 feature 组件。

例如 WorkspacePage 应该是这种感觉：

```text
WorkspacePage
|- BasePageHeader
|- SessionSidebar
|- ChatPanel
```

而不是把登录、会话初始化、消息转换、消息列表、输入框、弹窗逻辑全部继续堆在一个文件里。

## 10. 命名规范

建议统一以下命名。

1. 路由页面统一以 Page 结尾。
2. 业务组件使用具体业务名，不用 Index 这种泛名。
3. 服务统一以 Service 结尾。
4. 类型文件优先使用 types.ets，复杂模型再拆 model 目录。

示例：

1. SessionSidebar.ets 比 sidebar/Index.ets 更清晰。
2. WorkspacePage.ets 比 pages/Index.ets 更清晰。
3. AppService.ets 比 appApi.ets 更像领域服务。

## 11. 建议的渐进式重构顺序

不建议一次性大搬家，应该分 4 步做。

1. 先新增 features、shared、core 目录，但不立即删旧目录。
2. 优先迁移 chat、session、app-center 三个核心业务域。
3. 再把 BottomSheetMenu、通用 header、空态、加载态抽到 shared。
4. 最后把 pages 改名和收口，让页面只负责组装。

这样做的好处是风险低，不会一次性把所有 import 全打乱。

## 12. 一套简单判断口诀

以后新增代码时，按下面这套顺序判断就够了：

1. 能直接路由进入吗。能，就放 pages。
2. 只属于某个业务域吗。是，就放对应 feature。
3. 跨多个业务复用吗。是，就放 shared。
4. 是全局网络、鉴权、路由、存储能力吗。是，就放 core。

## 13. 最终建议

对你这个项目来说，最合适的不是“纯 pages/components/api/model 四层分法”，也不是完全激进的前端大一统架构，而是：

页面保留入口，业务按 feature 收拢，通用能力下沉 shared，全局基础设施沉到 core。

这样之后你新增一个功能时，判断路径会非常稳定：

1. 新页面先去 pages。
2. 页面里的业务块去 feature。
3. 多处复用的小组件去 shared。
4. 网络和鉴权去 core。

这套结构最适合你现在这个项目的原因是，它既能保留 HarmonyOS 页面入口的直观性，又能把聊天、会话、应用中心这些核心业务真正收成块，降低你继续迭代时的混乱感。
# ETS Frontend Structure

## 30 秒速查

1. 能被路由直接打开的文件，只放 pages。
2. 顶层 @Entry 页只做路由壳，真实页面实现放 feature/pages 或 feature/components。
3. 单一业务组件放 feature/components。
4. 跨业务复用组件放 shared。
5. 业务接口放 feature/service，底层请求放 core/http。
6. 业务核心对象放 feature/domain。
7. 请求 DTO、参数映射放 feature/request。
8. 过程态、草稿态、分组态放 feature/state。
9. 不再往 api、model、components、utils 旧目录新增代码。
10. 通用结果类型放 shared/types，feature 内不要重复定义 Result 壳。

这份文档描述当前 HarmonyOS 前端代码的落地结构。目标只有一个：以后新增页面、组件、服务、类型时，能立刻判断应该放哪里。

## 当前目录原则

当前代码按 4 层组织：

1. pages：只放路由入口页。
2. features：按业务域组织真实实现。
3. shared：跨业务复用的 UI 和布局。
4. core：全局基础设施。

不要再往以下旧目录新增代码：

1. api
2. model
3. components
4. utils

这些目录已经不再是推荐入口，后续只会继续清理，不会回到过去的集中堆放方式。

## 当前推荐结构

```text
ets/
|- pages/
|  |- Index.ets
|  |- MyApps.ets
|  |- AppStore.ets
|  |- Preview.ets
|  |- Explore.ets
|  |- DataCenter.ets
|  |- Profile.ets
|  |- Setting.ets
|
|- features/
|  |- workspace/
|  |  |- pages/
|  |  |- state/
|  |
|  |- app-center/
|  |  |- domain/
|  |  |- request/
|  |  |- state/
|  |  |- pages/
|  |  |- components/
|  |  |- service/
|  |
|  |- chat/
|  |  |- request/
|  |  |- domain/
|  |  |- state/
|  |  |- adapter/
|  |  |- components/
|  |  |- service/
|  |  |- types.ets
|  |
|  |- session/
|  |  |- domain/
|  |  |- request/
|  |  |- state/
|  |  |- components/
|  |  |- service/
|  |
|  |- auth/
|  |  |- model/
|  |  |- service/
|  |
|  |- user-center/
|  |  |- pages/
|  |
|  |- data-center/
|  |  |- pages/
|
|- shared/
|  |- ui/
|  |- layout/
|  |- types/
|
|- core/
|  |- http/
```

## 每层放什么

### pages

pages 只负责路由入口。

页面文件应该尽量薄，只做两件事：

1. 作为 Entry 页面被路由加载。
2. 挂载 feature 里的真实页面实现。

例如：

1. pages/Preview.ets 直接挂载 features/app-center/pages/PreviewScreen.ets。
2. pages/Index.ets 直接挂载 features/workspace/pages/WorkspaceScreen.ets。

不要把复杂业务状态、网络请求、消息转换继续写回 pages。

### features

features 是主战场。每个业务域自己管理自己的页面实现、组件、服务、状态和类型。

简单理解：

1. app-center 管应用展示、预览、应用列表。
2. chat 管消息模型、流式请求、消息渲染。
3. session 管会话列表、会话切换、会话服务。
4. auth 管登录与 token。
5. workspace 管工作台壳层和页面级组合。
6. user-center 管资料和设置相关页面实现。
7. data-center 管数据中心页面实现。

### shared

shared 放跨业务复用的内容。

常见例子：

1. 通用页头。
2. 通用占位页。
3. 通用底部菜单。

判断标准很简单：如果一个组件去掉业务文案后，还能在两个以上 feature 中复用，它才应该进 shared。

### core

core 放全局基础设施。

目前重点是 core/http，下层包括：

1. ApiConfig
2. HttpManager
3. Interceptors
4. SSEClient

core 不放业务 UI，不放业务页面。

## feature 内部怎么放

不是每个 feature 都必须拥有完全相同的子目录，但语义要一致。

### pages

放业务域里的真实页面实现。

命名建议使用 Screen 后缀，例如：

1. MyAppsScreen
2. PreviewScreen
3. ProfileScreen

规则：

1. 这是完整页面主体，不是路由入口。
2. 允许持有页面自己的状态和生命周期。
3. 被顶层 pages/*.ets 直接挂载。

### components

放业务域内可复用的 UI 组件。

例如：

1. ChatPanel
2. MessageItem
3. AppItem
4. SessionSidebar

规则：

1. 组件只服务当前 feature，默认放这里。
2. 不要因为“以后可能复用”就提前放到 shared。

### service

放对后端接口或业务流程的封装。

例如：

1. ChatStreamService
2. AppService
3. SessionService
4. AuthService

规则：

1. service 不关心页面布局。
2. service 只暴露业务能力。

### types

放当前 feature 对外可读的接口、DTO、返回结构等轻量类型。

适合放在 types.ets 的内容：

1. request/response interface
2. 简单 VO
3. service 返回结果类型

### request

放请求校验、请求 DTO 映射、接口参数转换。

chat 里当前的例子是：

1. AiChatRequestMapper

适用场景：

1. 页面输入需要先校验。
2. 前端请求对象需要转成后端 DTO。

### domain

放业务域核心模型。

chat 里当前的例子是：

1. ChatMessage
2. GenMessageContent

适用场景：

1. 这个模型会在多个组件之间流转。
2. 这个模型表达的是业务对象，不只是接口字段。

### state

放过程性状态对象、草稿对象、临时聚合状态。

chat 里当前的例子是：

1. ChatStreamDraft

适用场景：

1. 只服务于某个交互过程。
2. 不是最终持久模型。
3. 更像状态机的一部分。

### adapter

放数据转换和格式拆解逻辑。

chat 里当前的例子是：

1. GenMessageParser

适用场景：

1. 后端格式需要前端拆解。
2. 页面显示前要做转换。

## 新代码落点速查

### 新增页面

如果它是 router.pushUrl 直接跳转的页面：

1. 顶层路由文件放 pages。
2. 真实实现放对应 feature/pages。

示例：新增应用详情页。

1. pages/AppDetail.ets
2. features/app-center/pages/AppDetailScreen.ets

### 新增业务组件

如果只给单一业务域使用，放对应 feature/components。

示例：

1. 会话重命名弹窗放 features/session/components
2. 应用版本面板放 features/app-center/components

### 新增通用组件

如果跨多个业务域复用，放 shared。

示例：

1. 通用空状态卡片放 shared/ui
2. 通用页头放 shared/ui

### 新增接口封装

业务接口放 feature/service，底层 HTTP 放 core/http。

不要新建 api 根目录文件。

### 新增模型或类型

优先按语义落到 feature 内部：

1. 业务核心对象放 domain
2. 临时交互状态放 state
3. 请求映射放 request
4. 纯接口类型放 types.ets

## 依赖方向

依赖只允许单向流动：

```text
pages -> features
features -> shared
features -> core
shared -> core
```

不应该出现：

1. shared 依赖 feature
2. core 依赖 feature
3. pages 直接依赖一堆底层细节

## 命名约定

推荐约定：

1. 路由入口页用 Page 后缀或保持当前页面名。
2. feature 下的完整页面实现用 Screen 后缀。
3. 普通 UI 组件用业务语义名，不强制 Card、Panel、Item，但要自解释。
4. service 用 Service 后缀。
5. request 映射器用 Mapper 后缀。
6. 状态草稿类用 Draft 或 State 后缀。

## 提交前自检

每次新增文件前先问自己 4 个问题：

1. 这是路由入口，还是业务实现？
2. 这是单业务复用，还是跨业务复用？
3. 这是业务对象，还是临时状态？
4. 这段逻辑是页面职责，还是 service 或 adapter 职责？

如果这 4 个问题答清楚，文件位置基本就不会放错。
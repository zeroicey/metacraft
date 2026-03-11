# MetaCraft 后端架构整理与重构方案

## 目标

这份文档用于梳理当前后端架构、判断当前分层是否合理，并给出一份可落地的重构方案。重点关注以下问题：

1. app、image、preview 的职责边界是否清晰。
2. DTO 层是否存在语义混乱、跨模块漂移的问题。
3. AI 编排、会话、应用、版本、预览之间的依赖关系是否合理。
4. 如何在不一次性推翻现有系统的前提下，逐步重构到更稳定的结构。

## 一、当前后端架构梳理

### 1. 模块划分

当前后端主要按 modules 组织，核心包括：

1. ai
2. app
3. user
4. storage
5. yuanclaw

其中本次重点分析的是 ai、app、storage 三个模块。

### 2. 当前职责分布

#### ai 模块

主要承担：

1. 智能体统一入口与 SSE 输出。
2. 意图分析、会话标题生成、聊天能力。
3. 聊天会话与消息持久化。
4. 代码差异计算。
5. 部分与应用代码、应用信息、logo 生成相关的 DTO。

当前目录中可以看到 ai 模块下同时存在：

1. controller
2. service
3. pipeline
4. agent
5. dto
6. entity
7. repository
8. vo
9. util

这说明 ai 模块已经不是单纯的 AI 能力模块，而是混入了对话、应用代码、生成过程状态和部分应用领域模型。

#### app 模块

主要承担：

1. 应用 CRUD。
2. 应用版本管理。
3. 应用代码快照读取。
4. 预览内容读取与静态资源拼接。
5. logo 相关服务。

当前 app 模块下的 AppService 同时负责：

1. 应用实体创建。
2. 版本号计算。
3. HTML/JS 格式化。
4. 文件路径拼接。
5. StorageService 文件写入。
6. 当前版本指针维护。
7. 版本读取与删除。
8. 权限校验。
9. 当前代码快照读取。
10. logo 元数据更新。

这已经明显超过单一应用服务的职责范围。

#### storage 模块

主要承担底层文件存储能力，职责相对清晰。

### 3. 当前主调用链

#### 普通 AI 请求链路

UnifiedController -> UnifiedOrchestrator -> ChatPipelineService 或占位的 Gen/Edit Pipeline -> ChatMessageService / ChatSessionService / AI Agent

#### 应用管理链路

AppController -> AppService -> AppRepository / AppVersionRepository / StorageService

#### 应用预览链路

PreviewController -> PreviewService -> AppRepository / AppVersionRepository / StorageService

### 4. 当前结构的显著特征

当前项目的主要问题不是“模块数量不够”，而是“模块内部职责聚合过重、跨模块语义边界模糊”。

表现最明显的地方有三个：

1. AppService 过胖。
2. ai.dto 中混入大量应用领域对象。
3. preview 和 image 虽然被抽出来了，但抽取方式仍然停留在技术动作层，不是稳定的领域边界层。

## 二、当前架构是否合理

结论：部分合理，但整体上不够稳定，尤其在语义边界和依赖方向上存在明显问题。

### 1. 合理的部分

#### 统一入口 + 编排器模式是合理的

UnifiedController 与 UnifiedOrchestrator 的方向本身没有问题。对 AI 请求使用统一入口，再根据意图走不同 pipeline，这是一个可扩展的思路。

#### 预览能力独立成 PreviewService 的方向是对的

预览属于读模型能力，把它从 AppService 中抽出来是正确方向。

#### 底层文件操作放到 StorageService 是对的

文件 IO 不应该散落在业务服务中，集中在 storage 模块是合理的。

### 2. 不合理的部分

#### app、image、preview 的抽法语义不完整

你提到“抽出去的 app image preview 等有点语义上不太合理”，这个判断是对的。

问题不在于它们不该拆，而在于现在拆分依据更像“技术动作”而不是“领域职责”。

例如：

1. AppService 仍然保留了大量版本与代码存储逻辑。
2. PreviewService 依赖 AppRepository 和 AppVersionRepository 直接拼装预览资源，本质上是在做读模型查询，但名字只是 Preview，没有表达这是“预览内容读取应用服务”还是“预览发布服务”。
3. ImageService 当前仅保留 logo 生成占位，这个名字过大，但实际只服务 app logo，一个通用 ImageService 反而语义过宽。

换句话说，抽出来了，但没有形成清晰的应用边界。

#### AppService 已经变成杂糅型 Facade

AppService 同时处理：

1. 应用元数据。
2. 版本生命周期。
3. 代码文件保存。
4. 格式化逻辑。
5. 当前代码快照读取。
6. logo 元数据写入。
7. 权限校验。

这会导致两个问题：

1. 服务名叫 AppService，但已经不是单纯应用服务。
2. 后续任何和应用相关的需求都会继续堆到这里，最终成为不可维护的核心大类。

#### ChatMessageService 反向依赖 AppRepository，不合理

ChatMessageService 在转换响应对象时读取 AppRepository，给消息回填 related app 信息。这意味着对话域服务知道应用域仓储细节。

这是典型的跨聚合读写耦合。

短期上看方便，长期会导致：

1. 消息表示层耦合应用实体。
2. AI 模块难以独立演进。
3. 后续要做消息查询优化或独立读模型时很难拆。

#### DTO 层确实比较乱

当前 DTO 的主要问题不是数量多，而是分类依据不统一。

当前 ai.dto 中同时存在：

1. 请求对象，例如 AgentRequestDTO、ChatRequestDTO。
2. 会话对象，例如 ChatSessionCreateDTO、ChatSessionUpdateDTO。
3. 应用代码对象，例如 AppCodeDTO、AppCodeDiffDTO、AppCodeSnapshotDTO。
4. 应用信息对象，例如 AppInfoDTO、AppMetadataDTO。
5. 图片请求对象，例如 GenerateLogoRequestDTO。
6. 意图对象，例如 IntentDTO。

这些对象不是一个维度：

1. 有些是 HTTP 入参。
2. 有些是 AI 结构化输出。
3. 有些是服务层内部数据载体。
4. 有些是应用领域模型的轻量表达。

它们都叫 DTO，但用途完全不同，导致 dto 包变成“杂物箱”。

### 3. 依赖方向问题

当前依赖方向有以下风险：

1. ai 模块知道过多 app 细节。
2. app 模块内部又混入代码格式化、存储路径和版本装配。
3. preview 直接摸 repository，而不是通过明确的查询服务。
4. 响应对象构造分散在 service 内部，缺少独立 mapper 或 assembler 层。

这意味着当前架构可以继续跑，但会越来越难演进。

## 三、核心问题归纳

### 1. 领域边界没有彻底拉开

建议至少区分以下几个能力域：

1. 对话与 AI 编排域。
2. 应用元数据域。
3. 应用版本与代码资产域。
4. 应用预览发布域。
5. 媒体资产域，例如 logo。

当前这些边界还混在一起。

### 2. DTO 没有按用途分层

建议把 DTO 至少拆成四类：

1. api request
2. api response
3. application command/query
4. ai structured result

现在它们都塞进 dto，导致语义混乱。

### 3. 读模型和写模型没有分开

例如：

1. AppService 既负责写入，也负责查询，也负责组装返回。
2. PreviewService 是读能力，但仍直接依赖 repository 细节。
3. ChatMessageService 既负责持久化，也负责读取，还负责拼装带应用信息的展示对象。

这会让后续优化查询、缓存、联表、分页时越来越痛苦。

## 四、建议的目标架构

建议不是一下子改成复杂 DDD，而是做中等强度分层重构。目标是把职责收干净，让后续继续演化有空间。

### 1. 模块层次建议

建议后端核心按下面方式组织：

1. ai
2. conversation
3. app
4. asset
5. preview
6. storage
7. user

其中：

#### ai

只保留：

1. AI controller
2. orchestrator
3. pipeline
4. agent capability
5. AI 结构化输出模型
6. SSE event mapper

不再承载会话实体和应用代码对象。

#### conversation

拆出当前 ai 中的聊天会话与消息相关内容：

1. ChatSessionEntity
2. ChatMessageEntity
3. ChatSessionRepository
4. ChatMessageRepository
5. ChatSessionService
6. ChatMessageService
7. conversation 的 request/response model

这样 conversation 才是独立业务域，而不是挂在 ai 名下。

#### app

只保留应用聚合本身：

1. AppEntity
2. AppRepository
3. AppApplicationService
4. AppQueryService
5. app request/response model

负责应用元数据与对外管理接口。

#### asset

把代码版本、logo、未来截图等统一视为资产能力：

1. AppVersionEntity
2. AppVersionRepository
3. AppCodeAssetService
4. AppLogoService
5. AssetPathPolicy

这样 image 就不再是一个语义过大的概念，而是资产的子能力。

#### preview

独立为明确的预览发布与读取域：

1. PreviewController
2. PreviewQueryService
3. PreviewContentResolver
4. PreviewResourceAssembler

预览的职责是读取某个应用版本并发布可访问内容，而不是普通 app service 的附属功能。

### 2. 服务拆分建议

#### 现有 AppService 建议拆成

1. AppApplicationService
   负责创建应用、更新应用元数据、删除应用。
2. AppVersionService
   负责创建版本、删除版本、切换当前版本。
3. AppCodeAssetService
   负责 HTML/JS 文件读写、路径规则、格式化。
4. AppQueryService
   负责查询应用详情、版本详情、当前代码快照。
5. AppLogoService
   负责 logo 元数据绑定与 logo 资源定位。

拆完后，AppService 可以删除，或者退化为极薄 Facade，不再承载主要逻辑。

#### 现有 PreviewService 建议演化为

1. PreviewQueryService
   负责按 uuid/version 解析当前预览目标。
2. PreviewAssetResolver
   负责把 html 路径转换成 js/css 资源路径。
3. PreviewContentService
   负责读取内容并补齐 media type。

如果当前规模不大，也可以先只拆成 PreviewQueryService + PreviewAssetResolver 两层。

#### 现有 ImageService 建议重命名

建议不要继续叫 ImageService，语义太宽。

更合适的命名：

1. AppLogoService
2. AppLogoGenerationService
3. AppMediaAssetService

如果当前只处理 logo，就用 AppLogoService。

### 3. DTO / Response / 内部模型分层建议

这是本次最值得下手的一块。

建议不要再用一个 dto 包承载所有数据对象，而是按用途拆分。

#### HTTP 入参层

按 controller 归属放到 request 包，例如：

1. ai/api/request/AgentRequest
2. conversation/api/request/CreateSessionRequest
3. app/api/request/CreateAppRequest
4. app/api/request/UpdateAppRequest

#### HTTP 出参层

统一用 response 包，不再混用 VO 和 DTO：

1. app/api/response/AppResponse
2. app/api/response/AppVersionResponse
3. conversation/api/response/ChatMessageResponse
4. conversation/api/response/ChatSessionResponse

当前项目里 VO 本质上承担的是 response model，建议直接统一命名为 Response，更直观。

#### 应用层命令/查询对象

例如：

1. app/application/command/CreateAppCommand
2. app/application/command/CreateVersionCommand
3. app/application/query/GetAppDetailQuery

这些对象是给 service 用的，不直接暴露给 controller。

#### AI 结构化输出对象

单独放在 ai/result 或 ai/model 下，例如：

1. ai/result/IntentResult
2. ai/result/AppSpecResult
3. ai/result/PlanResult
4. ai/result/CodeEditResult

其中 AppInfoDTO、IntentDTO 这类对象更适合放到这里，而不是继续叫 DTO。

#### 内部读模型

例如预览和消息列表需要拼装额外字段时，可以定义 query model：

1. preview/query/PreviewResourceView
2. conversation/query/ChatMessageDetailView

### 4. 组装层建议

当前 convertToVO 分散在 service 内部，建议抽出 assembler 或 mapper：

1. AppResponseAssembler
2. ChatMessageResponseAssembler
3. PreviewResourceAssembler

这样 service 只做业务，不负责响应结构拼装。

### 5. 依赖方向建议

目标依赖方向建议如下：

1. controller -> application service
2. application service -> domain service / repository
3. query service -> repository / storage
4. assembler -> domain/query model -> response
5. ai orchestrator -> conversation service + app application service + ai capability

需要避免：

1. conversation service 直接依赖 app repository 去补响应字段。
2. app service 同时负责 preview。
3. ai dto 承担 app 内部模型。

## 五、分阶段重构方案

### Phase 1：先收边界，不改业务行为

目标：不改变对外 API，先把内部职责拆开。

任务：

1. 把 AppService 拆成 AppApplicationService、AppVersionService、AppQueryService、AppCodeAssetService。
2. 把 ImageService 重命名为 AppLogoService。
3. 把 PreviewService 改造成更明确的查询服务结构，至少拆出 asset path resolver。
4. 把 service 内 convertToVO 抽到 assembler。
5. 保持 controller 层接口不变，避免前端一起改。

产出：

1. 现有接口无行为变化。
2. 服务职责明显变薄。
3. 后续对象整理时不会继续受 AppService 大类拖累。

### Phase 2：整理 DTO / VO 命名与目录

目标：把当前 dto 杂糅问题彻底清掉。

任务：

1. 建立 request / response / command / result 四类对象目录。
2. 把 AgentRequestDTO、AppCreateDTO、AppUpdateDTO 迁移为 request 模型。
3. 把 AppVO、AppVersionVO、ChatMessageVO、ChatSessionVO 迁移为 response 模型。
4. 把 AppInfoDTO、IntentDTO 等迁移为 ai/result。
5. 把 AppCodeSnapshotDTO 迁移为 app/query/model 或 asset/model。
6. 删除跨模块含义不清的 DTO 命名。

产出：

1. DTO 只保留真正的 API transport 语义，或者直接不再使用 DTO 命名。
2. 对象位置一眼可知用途。
3. controller / service / ai 之间的数据边界更清晰。

### Phase 3：拆 conversation 出 ai 模块

目标：把对话系统从 AI 能力中解耦。

任务：

1. 将 ChatSession 和 ChatMessage 相关 entity、repository、service、controller 迁到 conversation 模块。
2. 让 UnifiedOrchestrator 依赖 conversation application service，而不是 ai 内部 service。
3. 把消息详情中的 related app 展示逻辑迁到 conversation query service 或专门 assembler。

产出：

1. ai 模块更纯粹，只保留智能体与编排。
2. 对话能力后续可独立扩展，不被 AI 技术实现绑死。

### Phase 4：梳理 preview 和 asset 语义

目标：让 preview、logo、代码文件这些能力进入更合理的语义边界。

任务：

1. 将 app version 与 code file 视为 asset 体系的一部分。
2. 预览读取改为基于 query service，不再由单个 PreviewService 直接承担所有解析与装配。
3. logo 绑定改为 AppLogoService 负责，不再作为宽泛的 ImageService。
4. 路径规则、资源扩展名、media type 解析集中到 asset/policy。

产出：

1. preview 成为面向外部读取的能力。
2. asset 成为内部资源管理能力。
3. app 聚合只维护元数据和关联关系。

## 六、建议的目标目录草案

apps/api/src/main/java/com/metacraft/api/modules/
  ai/
    controller/
    orchestrator/
    pipeline/
    capability/
    result/
    event/
  conversation/
    controller/
    service/
    repository/
    entity/
    api/request/
    api/response/
    assembler/
  app/
    controller/
    service/
    repository/
    entity/
    api/request/
    api/response/
    assembler/
  asset/
    service/
    repository/
    model/
    policy/
  preview/
    controller/
    service/
    model/
  storage/
    service/

## 七、重点结论

### 1. 当前架构不是完全不合理，但已经到需要重构的阶段

问题核心不是能不能跑，而是职责混合已经开始影响语义清晰度和后续扩展成本。

### 2. 你对 app / image / preview 语义不合理的判断是准确的

目前的拆分更像把几段代码挪走，还不是稳定的领域边界。尤其 ImageService 这个命名与职责不匹配，AppService 也过度膨胀。

### 3. DTO 层确实混乱，且这是当前最应该先治理的点之一

当前 DTO 同时承担 API 入参、AI 输出、内部数据载体和领域视图，建议优先按用途拆分，而不是继续追加对象。

### 4. 最合适的路线不是激进重写，而是四阶段渐进重构

建议顺序：

1. 先拆服务职责。
2. 再整理 request / response / result。
3. 再把 conversation 从 ai 中独立出来。
4. 最后梳理 preview 与 asset 的最终边界。

## 八、建议优先落地的最小重构包

如果只做一轮最有价值、风险又可控的重构，建议先做下面这些：

1. 拆 AppService。
2. ImageService 重命名为 AppLogoService。
3. 对象分层为 request / response / result。
4. 提取 assembler，移除 service 中的 convertToVO。
5. 将 ChatMessageService 中对 AppRepository 的展示性拼装迁出。

这五步做完，整体结构会立刻清楚很多，而且不会要求前端立刻配合大改。

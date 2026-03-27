# 应用商店与发布功能设计

## 概述

为 MetaCraft 添加应用商店功能，支持用户发布自己生成的应用，其他用户可以查看、评分和评论。

## 功能需求

1. **发布/下架应用**：用户可以将已创建的应用发布到商店，也可以下架
2. **应用商店列表**：展示所有已发布应用，包含名称、描述、logo、评分、作者信息
3. **应用详情**：展示应用的详细信息，包括评分和评论
4. **评分功能**：用户可以对已发布应用进行 1-5 星评分，每人只能评一次
5. **评论功能**：用户可以对已发布应用发表评论

## 数据库设计

### 新增表

#### app_ratings（应用评分表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| app_id | BIGINT | NOT NULL, FK → apps(id), INDEX | 关联应用 |
| user_id | BIGINT | NOT NULL, FK | 关联 users 表 |
| rating | SMALLINT | NOT NULL, CHECK (1-5) | 评分 1-5 星 |
| created_at | TIMESTAMP | NOT NULL | 评分时间 |

唯一约束：UNIQUE(app_id, user_id) - 防止重复评分

#### app_comments（应用评论表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| app_id | BIGINT | NOT NULL, FK → apps(id), INDEX | 关联应用 |
| user_id | BIGINT | NOT NULL, FK | 关联 users 表 |
| content | TEXT | NOT NULL, @Size(max=1000) | 评论内容（最大1000字） |
| created_at | TIMESTAMP | NOT NULL | 评论时间 |

### 现有表修改

#### apps 表新增字段

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| average_rating | DOUBLE PRECISION | NULL | 平均评分（缓存） |
| rating_count | INTEGER | 0 | 评分数量 |

## API 设计

### 应用商店

| 方法 | 端点 | 认证 | 说明 |
|------|------|------|------|
| GET | /api/store/apps | 否 | 获取应用商店列表 |
| GET | /api/store/apps/{id} | 否 | 获取应用详情 |

### 发布管理（需要认证）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | /api/store/apps/{id}/publish | 发布应用到商店 |
| DELETE | /api/store/apps/{id}/publish | 下架应用 |

### 评分（需要认证）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | /api/store/apps/{id}/ratings | 提交/更新评分 |

### 评论（需要认证）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | /api/store/apps/{id}/comments | 提交评论 |
| DELETE | /api/store/apps/{id}/comments/{commentId} | 删除自己的评论 |

## 接口详情

### GET /api/store/apps

返回所有已发布（is_public = true）的应用列表。

**响应（ApiResponse 格式）：**
```json
{
  "message": "success",
  "data": {
    "apps": [
      {
        "id": 1,
        "uuid": "xxx-uuid",
        "name": "计算器",
        "description": "简洁的计算器应用",
        "logo": "base64字符串或URL",
        "averageRating": 4.5,
        "ratingCount": 10,
        "author": {
          "id": 1,
          "name": "用户名",
          "avatarBase64": "base64字符串"
        },
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

### GET /api/store/apps/{id}

返回应用详情，包含评分和评论。

**响应（ApiResponse 格式）：**
```json
{
  "message": "success",
  "data": {
    "id": 1,
    "uuid": "xxx-uuid",
    "name": "计算器",
    "description": "简洁的计算器应用",
    "logo": "base64字符串或URL",
    "averageRating": 4.5,
    "ratingCount": 10,
    "author": {
      "id": 1,
      "name": "用户名",
      "avatarBase64": "base64字符串"
    },
    "createdAt": "2024-01-01T00:00:00Z",
    "comments": [
      {
        "id": 1,
        "userId": 2,
        "userName": "评论者",
        "userAvatar": "base64字符串",
        "content": "很好用的应用",
        "createdAt": "2024-01-02T00:00:00Z"
      }
    ]
  }
}
```

### POST /api/store/apps/{id}/publish

发布应用。只能由应用所有者操作。

**请求体：** 无

**响应（ApiResponse 格式）：**
```json
{
  "message": "应用已发布",
  "data": null
}
```

### POST /api/store/apps/{id}/ratings

提交评分。

**请求体：**
```json
{
  "rating": 5
}
```

**规则：**
- rating 必须是 1-5 的整数
- 用户已发布过则更新评分
- 评分后更新 apps 表的 average_rating 和 rating_count

**响应（ApiResponse 格式）：**
```json
{
  "message": "评分成功",
  "data": {
    "averageRating": 4.5,
    "ratingCount": 10
  }
}
```

### POST /api/store/apps/{id}/comments

提交评论。

**请求体：**
```json
{
  "content": "评论内容"
}
```

**响应（ApiResponse 格式）：**
```json
{
  "message": "评论成功",
  "data": {
    "id": 1,
    "userId": 2,
    "userName": "用户名",
    "userAvatar": "base64字符串",
    "content": "评论内容",
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

### DELETE /api/store/apps/{id}/comments/{commentId}

删除评论，只能删除自己的评论。

**响应（ApiResponse 格式）：**
```json
{
  "message": "评论已删除",
  "data": null
}
```

## 业务规则

1. **发布权限**：只有应用所有者可以发布/下架自己的应用
2. **评分限制**：每个用户对每个应用只能评分一次，可以更新自己的评分
3. **评分条件**：应用必须是已发布状态（is_public = true）才能评分
4. **评论条件**：应用必须是已发布状态才能评论
5. **下架后规则**：应用下架后，禁止新增评论和评分（历史数据保留）
6. **删除评论**：用户只能删除自己发表的评论
7. **评分更新**：用户更新评分时，重新计算 average_rating 和 rating_count

## 模块结构

```
modules/app/
├── controller/
│   └── StoreController.java      # 应用商店接口
├── entity/
│   ├── AppRatingEntity.java      # 评分实体
│   └── AppCommentEntity.java     # 评论实体
├── repository/
│   ├── AppRatingRepository.java
│   └── AppCommentRepository.java
├── service/
│   ├── StoreAppService.java     # 商店应用服务
│   ├── StoreRatingService.java  # 评分服务
│   └── StoreCommentService.java # 评论服务
├── dto/
│   ├── RatingRequestDTO.java
│   ├── CommentRequestDTO.java
│   └── PublishRequestDTO.java
└── vo/
    ├── StoreAppVO.java
    └── StoreAppDetailVO.java
```

## 现有代码复用

- 复用 AppEntity.isPublic 字段表示是否发布
- 复用现有的 User 查询逻辑获取作者信息
- 复用现有的 App 查询接口
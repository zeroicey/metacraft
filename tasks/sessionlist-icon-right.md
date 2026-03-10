# 任务：SessionList 图标居右

**状态：** 待开始 📋
**创建日期：** 2026-03-10

## 背景

优化会话列表的视觉布局，将应用图标移到右侧，仅在有应用关联时显示。

## 当前布局

```
┌────────────────────────────┐
│ [图标] 会话标题              │
└────────────────────────────┘
```

## 目标布局

```
┌────────────────────────────┐
│ 会话标题            [图标]  │  ← 有应用
│ 普通聊天                    │  ← 无图标
└────────────────────────────┘
```

## 修改内容

### 文件

`apps/huawei/entry/src/main/ets/components/sidebar/SessionList.ets`

### 具体改动

**当前代码（第 111-143 行）：**
```typescript
Row({ space: 8 }) {
  Stack() {
    if (this.hasLogo(item)) {
      Image(this.getLogoUrl(item))
        .width(28)
        .height(28)
        ...
    } else {
      Row() {
        SymbolGlyph($r('sys.symbol.ellipsis_message'))
          ...
      }
      .width(28)
      .height(28)
    }
  }
  .width(28)
  .height(28)

  Text(item.title)
    ...
    .layoutWeight(1)
}
```

**修改后：**
```typescript
Row({ space: 8 }) {
  Text(item.title)
    .fontSize(14)
    .fontWeight(this.selectedSessionId === item.sessionId ? FontWeight.Bold : FontWeight.Medium)
    .fontColor(this.selectedSessionId === item.sessionId ? '#000000' : '#333333')
    .layoutWeight(1)
    .maxLines(1)
    .textOverflow({ overflow: TextOverflow.Ellipsis })

  // 仅在有 relatedAppId 时显示图标
  if (this.hasLogo(item)) {
    Image(this.getLogoUrl(item))
      .width(28)
      .height(28)
      .borderRadius(6)
      .objectFit(ImageFit.Cover)
      .onError(() => {
        this.markLogoLoadFailed(item.sessionId);
      });
  }
}
```

### 关键改动点

| 改动 | 说明 |
|------|------|
| **图标位置** | 从 Row 开头移到结尾 |
| **无图标时** | 不显示占位符（移除 `else` 分支的 `SymbolGlyph`） |
| **布局** | `Text` 使用 `layoutWeight(1)` 占据剩余空间 |

## 预览效果

| 场景 | 效果 |
|------|------|
| **有应用关联** | `会话标题        [🖼️]` |
| **普通聊天** | `会话标题`（无图标） |

## 任务清单

- [ ] 修改 `SessionList.ets` 的 `SessionGroup` builder
- [ ] 移除 `else` 分支的占位图标
- [ ] 将图标移到 Row 末尾
- [ ] 测试有应用的会话
- [ ] 测试普通聊天会话

## 参考文件

- `apps/huawei/entry/src/main/ets/components/sidebar/SessionList.ets`

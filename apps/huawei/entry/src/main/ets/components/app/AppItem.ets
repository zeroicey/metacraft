/**
 * 应用列表项组件
 * 显示应用卡片，点击直接跳转到预览页面
 */
import { router } from '@kit.ArkUI';
import { App } from '../../model/appModel';
import { AppActionMenu, AppActionMenuOptions } from './AppActionMenu';
import { toApiUrl } from '../../utils/http/ApiConfig';

@ComponentV2
export struct AppItem {
  @Require @Param app: App;
  @Param onRefresh: (() => void) | null = null;
  @Local logoLoadFailed: boolean = false;

  // 生成应用Logo颜色（基于应用名称的简单哈希）
  private getLogoColor(name: string): string {
    const colors: string[] = [
      '#007AFF', // 蓝
      '#5856D6', // 紫
      '#AF52DE', // 紫红
      '#FF2D55', // 粉红
      '#FF3B30', // 红
      '#FF9500', // 橙
      '#FFCC00', // 黄
      '#34C759', // 绿
      '#30B0C7', // 青
      '#8E8E93'  // 灰
    ];

    let hash: number = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  // 获取应用名称首字母作为Logo
  private getLogoInitial(name: string): string {
    return name && name.length > 0 ? name.charAt(0).toUpperCase() : 'A';
  }

  // 格式化时间显示
  private formatTime(isoString: string): string {
    const date: Date = new Date(isoString);
    const now: Date = new Date();
    const diffMs: number = now.getTime() - date.getTime();
    const diffDays: number = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      const diffHours: number = Math.floor(diffMs / (1000 * 60 * 60));
      if (diffHours === 0) {
        const diffMinutes: number = Math.floor(diffMs / (1000 * 60));
        return diffMinutes <= 1 ? '刚刚' : `${diffMinutes}分钟前`;
      }
      return `${diffHours}小时前`;
    } else if (diffDays === 1) {
      return '昨天';
    } else if (diffDays < 7) {
      return `${diffDays}天前`;
    } else if (diffDays < 30) {
      return `${Math.floor(diffDays / 7)}周前`;
    } else if (diffDays < 365) {
      return `${Math.floor(diffDays / 30)}月前`;
    } else {
      return `${Math.floor(diffDays / 365)}年前`;
    }
  }

  // 格式化完整日期
  private formatFullDate(isoString: string): string {
    const date: Date = new Date(isoString);
    const year: number = date.getFullYear();
    const month: number = date.getMonth() + 1;
    const day: number = date.getDate();
    return `${year}/${month}/${day}`;
  }

  // 解析 logo URL，后端存储的是 uuid.ext
  private getLogoUrl(): string {
    if (!this.app.logo || this.app.logo.length === 0) {
      return '';
    }

    if (this.app.logo.startsWith('http://') || this.app.logo.startsWith('https://')) {
      return this.app.logo;
    }

    const dotIndex: number = this.app.logo.lastIndexOf('.');
    const logoUuid: string = dotIndex > 0 ? this.app.logo.substring(0, dotIndex) : this.app.logo;
    return toApiUrl(`/api/logo/${logoUuid}`);
  }

  build() {
    Row() {
      // Logo区域
      Stack() {
        if (!this.logoLoadFailed && this.getLogoUrl().length > 0) {
          Image(this.getLogoUrl())
            .width(56)
            .height(56)
            .borderRadius(14)
            .objectFit(ImageFit.Cover)
            .onError(() => {
              this.logoLoadFailed = true;
            })
        } else {
          Rect()
            .width(56)
            .height(56)
            .fill(this.getLogoColor(this.app.name))
            .radius(14)
          Text(this.getLogoInitial(this.app.name))
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor(Color.White)
        }
      }
      .margin({ right: 16 })

      // 应用信息区域
      Column() {
        // 第一行：名称和版本标签
        Row() {
          Text(this.app.name)
            .fontSize(17)
            .fontWeight(FontWeight.Medium)
            .fontColor($r('sys.color.ohos_id_color_text_primary'))
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })

          if (this.app.versions && this.app.versions.length > 0) {
            Text(` v${this.app.versions[0].versionNumber}`)
              .fontSize(13)
              .fontColor('#007AFF')
              .margin({ left: 6 })
          }

          Blank()
        }
        .width('100%')

        // 第二行：描述
        Text(this.app.description || '暂无描述')
          .fontSize(14)
          .fontColor('#8E8E93')
          .margin({ top: 3 })
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })

        // 第三行：时间信息
        Row() {
          Text(`创建于 ${this.formatTime(this.app.createdAt)}`)
            .fontSize(12)
            .fontColor('#C7C7CC')

          if (this.app.updatedAt !== this.app.createdAt) {
            Text(' · ')
              .fontSize(12)
              .fontColor('#C7C7CC')

            Text(`更新 ${this.formatTime(this.app.updatedAt)}`)
              .fontSize(12)
              .fontColor('#C7C7CC')
          }

          if (this.app.versions && this.app.versions.length > 1) {
            Text(' · ')
              .fontSize(12)
              .fontColor('#C7C7CC')

            Text(`${this.app.versions.length}个版本`)
              .fontSize(12)
              .fontColor('#C7C7CC')
          }
        }
        .margin({ top: 6 })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      // 更多操作菜单
      AppActionMenu({
        options: {
          app: this.app,
          onRefresh: () => {
            if (this.onRefresh) {
              this.onRefresh();
            }
          }
        }
      })
    }
    .width('100%')
    .height(88)
    .padding({ left: 16, right: 12, top: 12, bottom: 12 })
    .backgroundColor(Color.White)
    .borderRadius(16)
    .onClick(() => {
      this.handlePreview();
    })
  }

  private handlePreview(): void {
    const previewUrl: string = toApiUrl(`/api/preview/${this.app.uuid}`);

    router.pushUrl({
      url: 'pages/Preview',
      params: {
        url: previewUrl,
        appName: this.app.name,
        appUuid: this.app.uuid,
        logoUrl: this.getLogoUrl()
      }
    }).catch((err: Error) => {
      console.error('Failed to navigate to preview:', err.message);
    });
  }
}

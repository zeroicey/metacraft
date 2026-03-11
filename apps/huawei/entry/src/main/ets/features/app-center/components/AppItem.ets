import { router } from '@kit.ArkUI';
import { App } from '../domain/App';
import { AppActionMenu } from './AppActionMenu';
import { toApiUrl } from '../../../core/http/ApiConfig';

@ComponentV2
export struct AppItem {
  @Require @Param app: App;
  @Param onRefresh: (() => void) | null = null;
  @Local logoLoadFailed: boolean = false;

  private getLogoColor(name: string): string {
    const colors: string[] = [
      '#007AFF',
      '#5856D6',
      '#AF52DE',
      '#FF2D55',
      '#FF3B30',
      '#FF9500',
      '#FFCC00',
      '#34C759',
      '#30B0C7',
      '#8E8E93'
    ];

    let hash: number = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  private getLogoInitial(name: string): string {
    return name && name.length > 0 ? name.charAt(0).toUpperCase() : 'A';
  }

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

      Column() {
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

        Text(this.app.description || '暂无描述')
          .fontSize(14)
          .fontColor('#8E8E93')
          .margin({ top: 3 })
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })

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
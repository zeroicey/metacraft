/**
 * 应用列表项组件
 * 显示应用卡片，包含图标、名称、描述和操作按钮
 */
import { router } from '@kit.ArkUI';
import { App } from '../model/appModel';
import { AppActionMenu, AppActionMenuOptions } from './AppActionMenu';

@ComponentV2
export struct AppItem {
  @Require @Param app: App;
  @Param onRefresh: (() => void) | null = null;

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

  build() {
    Row() {
      // Logo区域
      Stack() {
        Rect()
          .width(52)
          .height(52)
          .fill(this.getLogoColor(this.app.name))
          .radius(12)
        Text(this.getLogoInitial(this.app.name))
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor(Color.White)
      }
      .margin({ right: 14 })

      // 应用信息区域
      Column() {
        Text(this.app.name)
          .fontSize(17)
          .fontWeight(FontWeight.Medium)
          .fontColor($r('sys.color.ohos_id_color_text_primary'))
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })

        Text(this.app.description || '暂无描述')
          .fontSize(13)
          .fontColor('#999999')
          .margin({ top: 2 })
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })

        // 版本信息
        if (this.app.versions && this.app.versions.length > 0) {
          Row() {
            Text(`v${this.app.versions[0].versionNumber}`)
              .fontSize(11)
              .fontColor('#007AFF')
              .padding({ left: 6, right: 6, top: 2, bottom: 2 })
              .backgroundColor('#E5F1FF')
              .borderRadius(4)

            if (this.app.versions.length > 1) {
              Text(`${this.app.versions.length} 个版本`)
                .fontSize(11)
                .fontColor('#999999')
                .margin({ left: 6 })
            }
          }
          .margin({ top: 6 })
        }
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      // 操作按钮区域
      Row({ space: 8 }) {
        // 预览按钮
        Button() {
          Text('▶')
            .fontSize(14)
            .fontColor(Color.White)
        }
        .width(36)
        .height(36)
        .backgroundColor('#007AFF')
        .borderRadius(10)
        .onClick(() => {
          this.handlePreview();
        })

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
    }
    .width('100%')
    .height(76)
    .padding({ left: 4, right: 4, top: 8, bottom: 8 })
    .backgroundColor(Color.Transparent)
    .onClick(() => {
      // 点击整个卡片也可以预览
      this.handlePreview();
    })
  }

  private handlePreview(): void {
    const baseUrl: string = 'http://192.168.5.4:8080';
    const previewUrl: string = `${baseUrl}/api/preview/${this.app.uuid}`;

    router.pushUrl({
      url: 'pages/Preview',
      params: {
        url: previewUrl,
        appName: this.app.name,
        appUuid: this.app.uuid
      }
    }).catch((err: Error) => {
      console.error('Failed to navigate to preview:', err.message);
    });
  }
}

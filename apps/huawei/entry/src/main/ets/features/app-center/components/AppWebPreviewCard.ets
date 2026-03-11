import { router } from '@kit.ArkUI';
import { webview } from '@kit.ArkWeb';
import { toApiUrl } from '../../../core/http/ApiConfig';

@Component
export struct AppWebPreviewCard {
  @Prop @Watch('onPreviewUrlChange') previewUrl: string;
  @Prop appName: string = 'Application Generated';
  @Prop logoUrl: string = '';
  @State private resolvedPreviewUrl: string = '';
  private previewController: webview.WebviewController = new webview.WebviewController();

  aboutToAppear() {
    this.syncResolvedPreviewUrl();
  }

  onPreviewUrlChange() {
    this.syncResolvedPreviewUrl();
  }

  private syncResolvedPreviewUrl() {
    if (!this.previewUrl || this.previewUrl.length === 0) {
      this.resolvedPreviewUrl = '';
      return;
    }
    this.resolvedPreviewUrl = this.previewUrl.startsWith('/')
      ? toApiUrl(this.previewUrl)
      : this.previewUrl;
  }

  build() {
    Column() {
      Row() {
        Blank().layoutWeight(1)

        Button() {
          SymbolGlyph($r('sys.symbol.arrow_up_left_and_arrow_down_right'))
            .fontWeight(FontWeight.Medium)
            .fontSize(16)
            .fontColor([Color.White])
        }
        .width(28)
        .height(28)
        .backgroundColor('#007AFF')
        .borderRadius(14)
        .onClick(() => {
          router.pushUrl({
            url: 'pages/Preview',
            params: {
              url: this.resolvedPreviewUrl,
              appName: this.appName,
              logoUrl: this.logoUrl,
            },
          });
        })
      }
      .width('100%')
      .margin({ bottom: 10 })

      if (this.resolvedPreviewUrl.length > 0) {
        Web({ src: this.resolvedPreviewUrl, controller: this.previewController })
          .width('100%')
          .layoutWeight(1)
          .domStorageAccess(true)
          .javaScriptAccess(true)
          .borderRadius(8)
          .backgroundColor('#FFFFFF');
      } else {
        Column() {
          Text('预览加载中...')
            .fontSize(12)
            .fontColor('#999999')
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#FFFFFF')
        .borderRadius(8);
      }
    }
    .width('100%')
    .height(210)
    .padding(10)
    .backgroundColor('#F5F7FA')
    .borderRadius(12)
    .border({ width: 1, color: '#EEEEEE' })
    .margin({ top: 8 });
  }
}
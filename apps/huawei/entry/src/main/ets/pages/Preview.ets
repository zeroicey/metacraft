import { webview } from '@kit.ArkWeb';
import { toApiUrl } from '../utils/http/ApiConfig';

@Entry
@Component
struct PreviewPage {
  topRectHeight: number = 0;

  @State url: string = '';
  @State appName: string = 'App Preview';
  @State logoUrl: string = '';
  @State logoLoadFailed: boolean = false;
  controller: webview.WebviewController = new webview.WebviewController();

  aboutToAppear() {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
    const params = this.getUIContext().getRouter().getParams() as Record<string, string>;
    if (params && params['url']) {
      this.url = params['url'];
      if (this.url.startsWith('/')) {
        this.url = toApiUrl(this.url);
      }
    }
    if (params && params['appName']) {
      this.appName = params['appName'];
    }
    if (params && params['logoUrl']) {
      this.logoUrl = params['logoUrl'];
      this.logoLoadFailed = false;
    }
  }

  build() {
    Column() {
      // Header
      Row() {
        Image($r('app.media.chevron_backward')) // Use system back icon if available, or text
          .width(24)
          .height(24)
          .onClick(() => {
            this.getUIContext().getRouter().back();
          })

        if (this.logoUrl.length > 0 && !this.logoLoadFailed) {
          Image(this.logoUrl)
            .width(26)
            .height(26)
            .borderRadius(6)
            .objectFit(ImageFit.Cover)
            .margin({ left: 12 })
            .onError(() => {
              this.logoLoadFailed = true;
            })
        }

        Text(this.appName)
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .margin({ left: 12 })
      }
      .width('100%')
      .padding({
        top: this.getUIContext().px2vp(this.topRectHeight),
        left: 16,
        right: 16,
        bottom: 8
      })
      .backgroundColor('#FFFFFF')
      .border({ width: { bottom: 1 }, color: '#EEEEEE' })

      // Webview
      if (this.url) {
        Web({ src: this.url, controller: this.controller })
          .width('100%')
          .layoutWeight(1)
          .domStorageAccess(true)
          .javaScriptAccess(true)
      } else {
        Column() {
          Text('No URL provided')
            .fontColor('#999999')
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}

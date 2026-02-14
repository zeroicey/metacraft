import { webview } from '@kit.ArkWeb';

@Entry
@Component
struct PreviewPage {
  topRectHeight: number = 0;

  @State url: string = '';
  controller: webview.WebviewController = new webview.WebviewController();

  aboutToAppear() {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
    const params = this.getUIContext().getRouter().getParams() as Record<string, string>;
    if (params && params['url']) {
      // Assuming the URL from backend is relative "/api/preview/..." or absolute
      // If relative, prepend host. 
      // For now, let's assume we need to prepend the host as used in aiChat.ets (192.168.2.133:8080)
      // Or better, let the caller handle the full URL.
      this.url = params['url'];
      if (this.url.startsWith('/')) {
        this.url = 'http://192.168.2.133:8080' + this.url;
      }
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
        Text('App Preview')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .margin({ left: 16 })
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

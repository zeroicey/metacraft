import { webview } from '@kit.ArkWeb';

const WEB_APP_URL: string = 'http://36.151.146.122/';

@Entry
@Component
struct Index {
  private controller: webview.WebviewController = new webview.WebviewController();

  build() {
    Column() {
      Web({ src: WEB_APP_URL, controller: this.controller })
        .javaScriptAccess(true)
        .domStorageAccess(true)
        .onPageBegin((event) => {
          console.info(`[ArkWeb] onPageBegin url=${event?.url}`);
        })
        .onProgressChange((event) => {
          console.info(`[ArkWeb] onProgressChange progress=${event?.newProgress}`);
        })
        .onPageEnd((event) => {
          console.info(`[ArkWeb] onPageEnd url=${event?.url}`);
        })
        .onConsole((event) => {
          console.info(`[ArkWeb][console] ${event?.message?.getMessage?.() ?? ''}`);
          return false;
        })
        .onHttpErrorReceive((event) => {
          const statusCode = event?.response?.getResponseCode?.();
          const url = event?.request?.getRequestUrl?.();
          console.error(`[ArkWeb] onHttpErrorReceive status=${statusCode} url=${url}`);
        })
        .onErrorReceive((event) => {
          const code = event?.error?.getErrorCode?.();
          const url = event?.request?.getRequestUrl?.();
          console.error(`[ArkWeb] onErrorReceive code=${code} url=${url}`);
        })
        .width('100%')
        .height('100%')
    }
    .width('100%')
    .height('100%')
  }
}
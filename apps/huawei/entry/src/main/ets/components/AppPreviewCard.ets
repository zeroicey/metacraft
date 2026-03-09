import { router } from "@kit.ArkUI";
import { webview } from "@kit.ArkWeb";
import { toApiUrl } from "../utils/http/ApiConfig";
@Component
export struct AppPreviewCard {
  @Prop @Watch("onPreviewUrlChange") previewUrl: string;
  @Prop appName: string = "Application Generated";
  @Prop appDescription: string = "Click to preview the app";
  @Prop @Watch("onLogoUrlChange") logoUrl: string = "";
  @State private logoLoadFailed: boolean = false;
  @State private resolvedLogoUrl: string = "";
  @State private resolvedPreviewUrl: string = "";
  private previewController: webview.WebviewController = new webview.WebviewController();

  aboutToAppear() {
    this.syncResolvedPreviewUrl();
    this.syncResolvedLogoUrl();
  }

  onPreviewUrlChange() {
    this.syncResolvedPreviewUrl();
  }

  onLogoUrlChange() {
    this.syncResolvedLogoUrl();
  }

  private syncResolvedPreviewUrl() {
    if (!this.previewUrl || this.previewUrl.length === 0) {
      this.resolvedPreviewUrl = "";
      return;
    }
    this.resolvedPreviewUrl = this.previewUrl.startsWith("/")
      ? toApiUrl(this.previewUrl)
      : this.previewUrl;
  }

  private syncResolvedLogoUrl() {
    this.logoLoadFailed = false;

    if (!this.logoUrl || this.logoUrl.length === 0) {
      this.resolvedLogoUrl = "";
      return;
    }

    this.resolvedLogoUrl = this.withCacheBust(this.logoUrl);
  }

  private withCacheBust(url: string): string {
    const separator = url.includes("?") ? "&" : "?";
    return `${url}${separator}t=${Date.now()}`;
  }

  build() {
    Column() {
      Row() {
        // Icon Placeholder
        Stack() {
          if (this.logoUrl.length > 0 && this.resolvedLogoUrl.length > 0 && !this.logoLoadFailed) {
            Image(this.resolvedLogoUrl)
              .width(40)
              .height(40)
              .borderRadius(8)
              .objectFit(ImageFit.Cover)
              .onError(() => {
                this.logoLoadFailed = true;
              });
          } else {
            Rect().width(40).height(40).fill("#007AFF").radius(8);
            Text("App").fontSize(12).fontColor(Color.White);
          }
        }
        .margin({ right: 12 })
        Column() {
          Text(this.appName)
            .fontSize(14)
            .fontWeight(FontWeight.Bold)
            .fontColor("#333333")
            .margin({ bottom: 2 })
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
          Text(this.appDescription)
            .fontSize(11)
            .fontColor("#999999")
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        Button() {
          SymbolGlyph($r("sys.symbol.arrow_up_left_and_arrow_down_right"))
            .fontWeight(FontWeight.Medium)
            .fontSize(16)
            .fontColor([Color.White])
        }
        .width(28)
        .height(28)
        .backgroundColor("#007AFF")
        .borderRadius(14)
        .onClick(() => {
          router.pushUrl({
            url: "pages/Preview",
            params: {
              url: this.resolvedPreviewUrl,
              appName: this.appName,
              logoUrl: this.logoUrl,
            },
          });
        })
      }
      .width("100%")
      .margin({ bottom: 10 })

      if (this.resolvedPreviewUrl.length > 0) {
        Web({ src: this.resolvedPreviewUrl, controller: this.previewController })
          .width("100%")
          .layoutWeight(1)
          .domStorageAccess(true)
          .javaScriptAccess(true)
          .borderRadius(8)
          .backgroundColor("#FFFFFF");
      } else {
        Column() {
          Text("预览加载中...")
            .fontSize(12)
            .fontColor("#999999")
        }
        .width("100%")
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
        .backgroundColor("#FFFFFF")
        .borderRadius(8);
      }
    }
    .width("100%")
    .height(248)
    .padding(10)
    .backgroundColor("#F5F7FA")
    .borderRadius(12)
    .border({ width: 1, color: "#EEEEEE" })
    .margin({ top: 8 });
  }
}

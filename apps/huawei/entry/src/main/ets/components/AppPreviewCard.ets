import { router } from '@kit.ArkUI';

@Component
export struct AppPreviewCard {
  @Prop previewUrl: string;
  @Prop appName: string = 'Application Generated';
  @Prop appDescription: string = 'Click to preview the app';
  @Prop @Watch('onLogoUrlChange') logoUrl: string = '';
  @State private logoLoadFailed: boolean = false;

  onLogoUrlChange() {
    this.logoLoadFailed = false;
  }

  build() {
    Column() {
      Row() {
        // Icon Placeholder
        Stack() {
          if (this.logoUrl.length > 0 && !this.logoLoadFailed) {
            Image(this.logoUrl)
              .width(40)
              .height(40)
              .borderRadius(8)
              .objectFit(ImageFit.Cover)
              .onError(() => {
                this.logoLoadFailed = true;
              })
          } else {
            Rect()
              .width(40)
              .height(40)
              .fill('#007AFF')
              .radius(8)
            Text('App')
              .fontSize(12)
              .fontColor(Color.White)
          }
        }
        .margin({ right: 12 })

        Column() {
          Text(this.appName)
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor('#333333')
            .margin({ bottom: 4 })
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
          Text(this.appDescription)
            .fontSize(12)
            .fontColor('#999999')
            .maxLines(2)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)

        Button('Preview')
          .fontSize(12)
          .height(28)
          .backgroundColor('#007AFF')
          .onClick(() => {
             router.pushUrl({
               url: 'pages/Preview',
               params: {
                 url: this.previewUrl,
                 appName: this.appName,
                 logoUrl: this.logoUrl
               }
             });
          })
      }
      .width('100%')
      .padding(12)
      .backgroundColor('#F5F7FA')
      .borderRadius(12)
      .border({ width: 1, color: '#EEEEEE' })
    }
    .width('100%')
    .margin({ top: 8 })
  }
}

import { router } from '@kit.ArkUI';

@Component
export struct AppPreviewCard {
  @Prop previewUrl: string;

  build() {
    Column() {
      Row() {
        // Icon Placeholder
        Stack() {
          Rect()
            .width(40)
            .height(40)
            .fill('#007AFF')
            .radius(8)
          Text('App')
            .fontSize(12)
            .fontColor(Color.White)
        }
        .margin({ right: 12 })
          
        Column() {
          Text('Application Generated')
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor('#333333')
            .margin({ bottom: 4 })
          Text('Click to preview the app')
            .fontSize(12)
            .fontColor('#999999')
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
                 url: this.previewUrl
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

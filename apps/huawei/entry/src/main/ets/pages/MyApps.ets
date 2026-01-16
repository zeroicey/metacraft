@Entry
@ComponentV2
struct MyApps {
  topRectHeight: number = 0;

  aboutToAppear(): void {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
  }

  build() {
    Column() {
      // 顶部导航栏
      Row() {
        Button() {
          SymbolGlyph($r('sys.symbol.chevron_left'))
            .fontWeight(FontWeight.Normal)
            .fontSize(22)
            .fontColor([$r('sys.color.ohos_id_color_titlebar_icon')])
        }
        .backgroundColor(Color.Transparent)
        .height(40)
        .width(40)
        .onClick(() => {
          this.getUIContext().getRouter().back();
        })

        Text('我的元应用')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ left: 8 })

        Blank().layoutWeight(1)
      }
      .width('100%')
      .padding({
        top: this.getUIContext().px2vp(this.topRectHeight),
        left: 16,
        right: 16,
        bottom: 8
      })

      // 占位内容
      Column() {
        Text('我的元应用')
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .margin({ bottom: 16 })

        Text('这里将展示你创建的所有元应用')
          .fontSize(14)
          .fontColor('#888888')
      }
      .layoutWeight(1)
      .justifyContent(FlexAlign.Center)
    }
    .width('100%')
    .height('100%')
    .gesture(
      PanGesture({ direction: PanDirection.Right })
        .onActionEnd((event: GestureEvent) => {
          if (event.offsetX > 50) {
            this.getUIContext().getRouter().back();
          }
        })
    )
  }
}

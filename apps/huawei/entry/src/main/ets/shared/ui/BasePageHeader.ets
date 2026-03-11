@ComponentV2
export struct BasePageHeader {
  @Param title: string = '';
  @Param topRectHeight: number = 0;
  @Param showBackButton: boolean = true;
  @Param onBack: () => void = () => {};

  build() {
    Row() {
      if (this.showBackButton) {
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
          this.onBack();
        })
      }

      Text(this.title)
        .fontSize(18)
        .fontWeight(FontWeight.Medium)
        .margin({ left: this.showBackButton ? 8 : 0 })

      Blank().layoutWeight(1)
    }
    .width('100%')
    .padding({
      top: this.topRectHeight,
      left: 16,
      right: 16,
      bottom: 8
    })
  }
}
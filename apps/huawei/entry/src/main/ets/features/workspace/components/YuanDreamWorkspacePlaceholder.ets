@ComponentV2
export struct YuanDreamWorkspacePlaceholder {
  @Param bottomPadding: number = 0;

  build() {
    Column({ space: 12 }) {
      Text('元梦')
        .fontSize(24)
        .fontWeight(FontWeight.Bold)

      Text('元梦页面占位组件')
        .fontSize(14)
        .fontColor('#999999')
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .padding({ left: 24, right: 24, bottom: this.bottomPadding + 5 })
  }
}

@ComponentV2
export struct PagePlaceholder {
  @Param title: string = '';
  @Param description: string = '';

  build() {
    Column() {
      Text(this.title)
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
        .margin({ bottom: 16 })

      Text(this.description)
        .fontSize(14)
        .fontColor('#888888')
    }
    .layoutWeight(1)
    .justifyContent(FlexAlign.Center)
  }
}
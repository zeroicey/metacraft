@Entry
@Component
struct Index {
  @State message: string = 'Hello World';

  build() {
    Column() {
      // 顶部导航栏
      Row() {
        Button('...')
          .width(40)
          .height(40)
          .backgroundColor(Color.Transparent)
          .fontSize(24)
          .fontColor('#333333')

        Text('元创')
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Button()
          .width(40)
          .height(40)
          .backgroundColor('#87CEEB')
      }
      .height(60)
      .width('100%')
      .justifyContent(FlexAlign.SpaceBetween)
      .padding({ left: 12, right: 12 })

      // 中间内容区域
      Row() {
        Text(this.message)
          .id('HelloWorld')
          .fontSize($r('app.float.page_text_font_size'))
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            this.message = 'Welcome';
          })
      }
      .flexGrow(1)
      .width('100%')
      .justifyContent(FlexAlign.Center)

      // 浮动输入框
      Row() {
        TextInput({ placeholder: '请输入内容' })
          .layoutWeight(1)
          .height(48)
          .backgroundColor(Color.White)
          .border({ width: 1, color: '#e0e0e0' })
          .borderRadius(24)
          .padding({ left: 16, right: 16 })

        Button('发送')
          .width(70)
          .height(48)
          .backgroundColor('#87CEEB')
          .borderRadius(24)
          .fontSize(16)
          .fontColor(Color.White)
          .margin({ left: 8 })
      }
      .width('90%')
      .position({ x: '5%', y: '90%' })
      .zIndex(999)
    }
    .height('100%')
    .width('100%')
    .backgroundColor('#f5f5f5')
  }
}

@ComponentV2
export struct WorkspaceStage {
  @Param appName: string = '';
  @Event onRunPreview: () => void;

  build() {
    Column({ space: 16 }) {
      // 开场白
      Column({ space: 8 }) {
        Text('基于你的蓝图，元应用已构建完成！')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)

        // 预览卡片
        Column({ space: 8 }) {
          Text(this.appName || '未命名应用')
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
          Text('点击运行，在全屏容器中预览应用。')
            .fontSize(14)
            .fontColor('#888888')

          Button('▶️ 运行')
            .width('100%')
            .height(44)
            .onClick(() => {
              this.onRunPreview();
            })
        }
        .padding(12)
        .borderRadius(12)
        .backgroundColor('#F5F5F5')
      }

      // TODO: 后续可在这里扩展对话区、聊天输入框等
    }
  }
}

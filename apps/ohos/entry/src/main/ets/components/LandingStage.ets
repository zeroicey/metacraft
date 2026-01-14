@ComponentV2
export struct LandingStage {
  @Event onStart: () => void;
  @Event onSelectInspiration: (name: string, desc: string) => void;

  build() {
    Column({ space: 16 }) {
      // 欢迎区
      Column({ space: 8 }) {
        Text('Hi，创作者')
          .fontSize(28)
          .fontWeight(FontWeight.Bold)

        Text('今天想把什么想法变成现实？')
          .fontSize(16)
          .fontColor('#888888')
      }
      .margin({ top: '10%' })

      // 灵感胶囊
      Column({ space: 8 }) {
        Text('灵感胶囊')
          .fontSize(14)
          .fontColor('#AAAAAA')

        Row({ space: 8 }) {
          this.buildInspirationChip('🎲 掷骰子游戏', '一个简单的掷骰子小游戏，支持计分和规则自定义');
          this.buildInspirationChip('📝 极简备忘录', '一个用于快速记录灵感和待办的极简备忘录');
        }
        Row({ space: 8 }) {
          this.buildInspirationChip('⏲️ 番茄专注钟', '一个支持番茄工作法的专注计时器');
        }
      }

      Blank().layoutWeight(1)

      // 启动入口按钮
      Button('+ 开始创建一个新元应用')
        .width('100%')
        .height(48)
        .onClick(() => {
          this.onStart();
        })
    }
  }

  @Builder
  private buildInspirationChip(label: string, desc: string) {
    Button(label)
      .height(36)
      .onClick(() => {
        this.onSelectInspiration(label, desc);
      })
  }
}

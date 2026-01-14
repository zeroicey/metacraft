@ComponentV2
export struct BlueprintStage {
  @Param appName: string = '';
  @Param planText: string = '';
  @Event onPlanTextChange: (value: string) => void;
  @Event onBack: () => void;
  @Event onBuild: () => void;

  build() {
    Column({ space: 16 }) {
      // 概览头
      Column({ space: 4 }) {
        Text(this.appName || '未命名应用')
          .fontSize(22)
          .fontWeight(FontWeight.Bold)
        Text('即将为你生成以下内容，是否确认？')
          .fontSize(14)
          .fontColor('#888888')
      }

      // 蓝图卡片
      Column() {
        Text('蓝图规划')
          .fontSize(14)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 8 })
        Scroll() {
          TextArea({ text: this.planText })
            .height(220)
            .onChange(value => {
              this.onPlanTextChange(value);
            })
        }
        .height(220)
      }

      Blank().layoutWeight(1)

      // 底部操作栏
      Row({ space: 12 }) {
        Button('上一步')
          .layoutWeight(1)
          .height(44)
          .onClick(() => {
            this.onBack();
          })

        Button('🚀 立即生成 (Build Now)')
          .layoutWeight(1)
          .height(44)
          .onClick(() => {
            this.onBuild();
          })
      }
    }
  }
}

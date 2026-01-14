@ComponentV2
export struct InceptionStage {
  @Param appName: string = '';
  @Param appDesc: string = '';
  @Event onAppNameChange: (value: string) => void;
  @Event onAppDescChange: (value: string) => void;
  @Event onNext: () => void;

  build() {
    Column({ space: 16 }) {
      // 引导文案
      Column({ space: 4 }) {
        Text('好的，先给你的元应用起个名字。')
          .fontSize(22)
          .fontWeight(FontWeight.Medium)
        Text('描述写得越详细，AI 就越懂你。')
          .fontSize(14)
          .fontColor('#888888')
      }

      // 表单区
      Column({ space: 12 }) {
        Text('应用名称')
          .fontSize(14)
        TextInput({
          placeholder: '例如：我的记账本',
          text: this.appName
        })
          .onChange(value => {
            this.onAppNameChange(value);
          })

        Text('应用描述')
          .fontSize(14)
        TextArea({
          placeholder: '描述你的应用功能，例如：我要一个记录日常开销的应用，要有图表统计...',
          text: this.appDesc
        })
          .height(160)
          .onChange(value => {
            this.onAppDescChange(value);
          })
      }

      Blank().layoutWeight(1)

      // 底部操作栏
      Button('下一步 (Next)')
        .width('100%')
        .height(48)
        .enabled(this.canNext())
        .onClick(() => {
          if (!this.canNext()) {
            return;
          }
          this.onNext();
        })
    }
  }

  private canNext(): boolean {
    return this.appName.trim().length > 0 && this.appDesc.trim().length > 0;
  }
}

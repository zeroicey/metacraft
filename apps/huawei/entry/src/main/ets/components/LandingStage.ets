/**
 * Stage组件使用方法备份（暂不使用，仅供参考）
 *
 * 1. LandingStage 落地页
 *    LandingStage({
 *      onStart: () => {
 *        // 点击开始创建按钮
 *      },
 *      onSelectInspiration: (name: string, desc: string) => {
 *        // 选择灵感胶囊
 *      }
 *    })
 *
 * 2. InceptionStage 初始阶段（命名和描述）
 *    InceptionStage({
 *      appName: this.appName,
 *      appDesc: this.appDesc,
 *      onAppNameChange: (value: string) => {
 *        this.appName = value;
 *      },
 *      onAppDescChange: (value: string) => {
 *        this.appDesc = value;
 *      },
 *      onNext: () => {
 *        // 下一步
 *      }
 *    })
 *
 * 3. BlueprintStage 蓝图阶段（规划确认）
 *    BlueprintStage({
 *      appName: this.appName,
 *      planText: this.planText,
 *      onPlanTextChange: (value: string) => {
 *        this.planText = value;
 *      },
 *      onBack: () => {
 *        // 返回上一步
 *      },
 *      onBuild: () => {
 *        // 立即生成
 *      }
 *    })
 *
 * 4. WorkspaceStage 工作区阶段（预览和运行）
 *    WorkspaceStage({
 *      appName: this.appName,
 *      onRunPreview: () => {
 *        // 运行预览
 *      }
 *    })
 */

@ComponentV2
export struct LandingStage {
  @Event onStart: () => void;
  @Event onSelectInspiration: (name: string, desc: string) => void;
  @Local displayText: string = '';
  @Local currentTextIndex: number = 0;
  private textOptions: string[] = [
    '今天想把什么想法变成现实？',
    '准备好创造一些令人惊叹的东西了吗？',
    '让我们一起将想象力转化为现实！',
    '有什么灵感想要实现吗？',
    '开始你的创作之旅吧！'
  ];
  private typewriterTimer: number = -1;

  aboutToAppear(): void {
    this.startTypewriter();
  }

  aboutToDisappear(): void {
    if (this.typewriterTimer !== -1) {
      clearInterval(this.typewriterTimer);
    }
  }

  private startTypewriter(): void {
    const text = this.textOptions[this.currentTextIndex];
    let charIndex = 0;
    this.displayText = '';

    this.typewriterTimer = setInterval(() => {
      if (charIndex < text.length) {
        this.displayText += text[charIndex];
        charIndex++;
      } else {
        clearInterval(this.typewriterTimer);
        // 3秒后切换到下一句
        setTimeout(() => {
          this.currentTextIndex = (this.currentTextIndex + 1) % this.textOptions.length;
          this.startTypewriter();
        }, 3000);
      }
    }, 80);
  }

  build() {
    Column({ space: 16 }) {
      Blank().layoutWeight(1)
  
      // 欢迎区
      Column({ space: 8 }) {
        Text('Hi，创作者')
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .width('100%')
          .textAlign(TextAlign.Start)
      
        Text(this.displayText)
          .fontSize(16)
          .fontColor('#888888')
          .height(24)
          .width('100%')
          .textAlign(TextAlign.Start)
      }
      .width('90%')
      .alignItems(HorizontalAlign.Start)
      
      // 灵感胶囊 - 流式错落布局
      Flex({ wrap: FlexWrap.Wrap, justifyContent: FlexAlign.Start }) {
        this.buildInspirationChip('⏲️ 定制番茄钟', '一个支持番茄工作法的专注计时器');
        this.buildInspirationChip('🎲 玩游戏', '一个简单的投骰子小游戏，支持计分和规则自定义');
        this.buildInspirationChip('🎨 制作调色板', '一个用于配色方案设计的调色板应用');
        this.buildInspirationChip('📝 快速记事本', '一个用于快速记录灵感和待办的极简备忘录');
        this.buildInspirationChip('🎵 噪音机', '提供多种环境音帮助专注和放松');
        this.buildInspirationChip('📊 习惯追踪器', '帮助你培养好习惯的追踪记录工具');
      }
      .width('100%')
      .margin({ top: 20 })
  
      Blank().layoutWeight(1)
  
      // 启动入口按钮
      Button('+ 开始创建一个新元应用')
        .width('100%')
        .height(48)
        .backgroundColor($r('sys.color.ohos_id_color_emphasize'))
        .margin({ bottom: 24 })
        .onClick(() => {
          this.onStart();
        })
    }
  }

  @Builder
  private buildInspirationChip(label: string, desc: string) {
    Button(label)
      .height(40)
      .padding({ left: 16, right: 16 })
      .margin({ right: 8, bottom: 15 })
      .backgroundColor('#F5F5F5')
      .fontColor('#333333')
      .borderRadius(20)
      .onClick(() => {
        this.onSelectInspiration(label, desc);
      })
  }
}

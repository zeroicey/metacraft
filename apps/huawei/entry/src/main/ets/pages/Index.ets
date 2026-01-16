import { Sidebar } from '../components/Sidebar';
import { BottomSheetMenu } from '../components/BottomSheetMenu';

interface Message {
  id: number;
  text: string;
  isUser: boolean;
}

@Entry
@ComponentV2
struct Index {
  topRectHeight: number = 0;
  bottomRectHeight: number = 0;
  @Local messages: Message[] = [
    { id: 1, text: '你好！我是元创AI助手，很高兴为你服务。我可以帮你创建各种元应用，你有什么想法吗？', isUser: false },
    { id: 2, text: '我想做一个番茄钟应用', isUser: true },
    { id: 3, text: '很好的想法！番茄钟是一个很实用的生产力工具。让我来帮你规划一下：\n\n1. 核心功能：25分钟工作计时和5分钟休息提醒\n2. 可以添加任务列表，记录每个任务完成的番茄钟数量\n3. 统计功能，展示每天/每周的专注时长\n4. 自定义时长设置\n\n你觉得这个方案怎么样？需要调整什么吗？', isUser: false },
    { id: 4, text: '听起来不错，能加上白噪音功能吗？', isUser: true },
    { id: 5, text: '当然可以！我会在应用中集成白噪音功能，提供多种环境音供你选择，比如雨声、咖啡厅、森林等，帮助你更好地专注。现在开始为你生成这个应用吧！', isUser: false }
  ];
  @Local inputText: string = '';

  @Local showSideBar: boolean = false;

  aboutToAppear(): void {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
    this.bottomRectHeight = AppStorage.get<number>('bottomRectHeight') ?? 0;
  }

  @Builder
  buildTopBar() {
    Row() {
      Button() {
        SymbolGlyph($r('sys.symbol.open_sidebar'))
          .fontWeight(FontWeight.Normal)
          .fontSize(22)
          .fontColor([$r('sys.color.ohos_id_color_titlebar_icon')])
          .hitTestBehavior(HitTestMode.None)
      }
      .backgroundColor(Color.Transparent)
      .height(40)
      .width(40)
      .onClick(() => {
        this.showSideBar = true;
      })

      Blank().layoutWeight(1)

      Text('元创')
        .fontSize(18)
        .fontWeight(FontWeight.Medium)
        .fontStyle(FontStyle.Italic)

      Blank().layoutWeight(1)

      BottomSheetMenu({
        onMenuItemClick: (menuId: string) => {
          this.handleMenuClick(menuId);
        }
      })
    }
    .width('100%')
    .padding({
      top: this.getUIContext().px2vp(this.topRectHeight),
      left: 16,
      right: 16,
      bottom: 8
    })
  }
  
  @Builder
  buildMessageItem(message: Message) {
    if (message.isUser) {
      // 用户消息 - 右侧气泡
      Row() {
        Column() {
          Text(message.text)
            .fontSize(15)
            .fontColor('#FFFFFF')
            .padding({ top: 10, bottom: 10, left: 14, right: 14 })
            .backgroundColor('#007AFF')
            .borderRadius(16)
            .maxLines(999)
        }
        .alignItems(HorizontalAlign.End)
      }
      .width('100%')
      .justifyContent(FlexAlign.End)
      .margin({ bottom: 16 })
    } else {
      // AI消息 - 左侧无气泡
      Row() {
        Text(message.text)
          .fontSize(15)
          .fontColor('#333333')
          .lineHeight(22)
          .maxLines(999)
      }
      .width('100%')
      .justifyContent(FlexAlign.Start)
      .margin({ bottom: 16 })
    }
  }
  
  @Builder
  buildContent() {
    Column() {
      this.buildTopBar();

      // 消息列表区域
      Scroll() {
        Column() {
          ForEach(this.messages, (message: Message) => {
            this.buildMessageItem(message);
          })
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16, bottom: 16 })
      }
      .layoutWeight(1)
      .edgeEffect(EdgeEffect.Spring)
      .align(Alignment.Top)

      // 底部输入区域
      Column() {
        // 输入框和发送按钮
        Row() {
          TextInput({ placeholder: '输入你的想法...', text: this.inputText })
            .layoutWeight(1)
            .height(40)
            .backgroundColor('#F5F5F5')
            .borderRadius(10)
            .padding({ left: 16, right: 16 })
            .onChange((value: string) => {
              this.inputText = value;
            })

          Button('发送')
            .height(40)
            .backgroundColor($r('sys.color.ohos_id_color_emphasize'))
            .borderRadius(10)
            .margin({ left: 8 })
            .enabled(this.inputText.trim().length > 0)
            .onClick(() => {
              if (this.inputText.trim().length > 0) {
                // 添加用户消息
                this.messages.push({
                  id: this.messages.length + 1,
                  text: this.inputText,
                  isUser: true
                });
                this.inputText = '';
                // TODO: 后续接入真实AI响应
              }
            })
        }
        .width('100%')

        // AI生成提示文字
        Text('内容由AI生成')
          .fontSize(12)
          .fontColor('#999999')
          .width('100%')
          .textAlign(TextAlign.Center)
          .margin({ top: 8 })
      }
      .width('100%')
      .padding({ 
        left: 16, 
        right: 16, 
        bottom: this.getUIContext().px2vp(this.bottomRectHeight) + 5,
        top: 8 
      })
      .backgroundColor(Color.White)
    }
    .width('100%')
    .height('100%')
  }

  build() {
    Stack() {
      Sidebar({
        isShowSidebar: this.showSideBar,
        topRectHeight: this.getUIContext().px2vp(this.topRectHeight),
        onSideBarChange: (isShow: boolean) => {
          this.showSideBar = isShow;
        },
        contentBuilder: () => {
          this.buildContent();
        }
      })
    }
    .width('100%')
    .height('100%')
  }

  // 菜单项点击处理
  handleMenuClick(menuId: string) {
    console.info('点击了菜单项:', menuId);
    switch (menuId) {
      case 'profile':
        // 跳转到个人中心
        break;
      case 'settings':
        // 跳转到设置页面
        break;
      case 'about':
        // 显示关于信息
        break;
    }
  }
}
import {
  AboutModel,
  SettingItem,
  SettingGroup,
  SettingType,
  SettingView,
  SupportedSetting,
  WebModel,
} from 'app_setting'

@Entry
@ComponentV2
struct SettingsPage {
  topRectHeight: number = 0;
  @Local navPathStack: NavPathStack = new NavPathStack();
  
  @Local group1: SettingGroup = new SettingGroup()
    .setList([
      new SettingItem()
        .setType(SettingType.TEXT)
        .setLabel('个人信息')
        .setClickEvent(() => {
          this.getUIContext().getRouter().pushUrl({ url: 'pages/Profile' });
        }),
    ])
  @Local group2: SettingGroup = new SettingGroup()
    .setList([
      new SettingItem()
        .setSupportedType(SupportedSetting.LIGHT_DARK_MODE)
        .setSelectIndex(0)
        .setOnChange((colorMode) => {
          // 获取当前应用深浅色模式：colorMode as ConfigurationConstant.ColorMode
        }),
    ])
  @Local group3: SettingGroup = new SettingGroup()
    .setList([
      new SettingItem()
        .setSupportedType(SupportedSetting.PUSH_SWITCH)
        .setOnChange((isOn) => {
          // 获取推送开关情况：isOn as boolean
        }),
      new SettingItem()
        .setSupportedType(SupportedSetting.CLEAR_CACHE),
    ])
  @Local recommendItem: SettingItem = new SettingItem()
    .setType(SettingType.SWITCH)
    .setLabel('个性化推荐')
    .setSwitchValue(true)
    .setClickEvent((isOn) => {
      this.recommendItem.setSwitchValue(isOn as boolean);
    })
  @Local group4: SettingGroup = new SettingGroup()
    .setTitle('数据和隐私')
    .setList([
      this.recommendItem,
    ])
  @Local group5: SettingGroup = new SettingGroup()
    .setTitle('了解我们如何使用您的数据')
    .setList([
      new SettingItem()
        .setSupportedType(SupportedSetting.WEB)
        .setLabel('业务与隐私的声明')
        .setExtraParams({ src: 'https://www.example.com' } as WebModel),
      new SettingItem()
        .setSupportedType(SupportedSetting.WEB)
        .setLabel('已收集个人信息清单')
        .setExtraParams({ src: 'https://www.example.com' } as WebModel),
      new SettingItem()
        .setSupportedType(SupportedSetting.WEB)
        .setLabel('隐私声明摘要')
        .setExtraParams({ src: 'https://www.example.com' } as WebModel),
    ])
  @Local group6: SettingGroup = new SettingGroup()
    .setTitle('更多')
    .setList([
      new SettingItem()
        .setSupportedType(SupportedSetting.CHECK_VERSION),
    ])

  aboutToAppear(): void {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
  }

  build() {
    Navigation(this.navPathStack) {
      Column() {
        // 顶部导航栏
        Row() {
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
            this.getUIContext().getRouter().back();
          })

          Text('设置')
            .fontSize(18)
            .fontWeight(FontWeight.Medium)
            .margin({ left: 8 })

          Blank().layoutWeight(1)
        }
        .width('100%')
        .padding({
          top: this.getUIContext().px2vp(this.topRectHeight),
          left: 16,
          right: 16,
          bottom: 8
        })

        // 设置组件
        SettingView({
          pathStack: this.navPathStack,
          settingList: [this.group1, this.group2, this.group3, this.group4, this.group5, this.group6],
          pagePadding: { left: 16, right: 16 }
        })
      }
      .width('100%')
      .height('100%')
    }
    .hideTitleBar(true)
    .backgroundColor($r('sys.color.background_secondary'))
    .mode(NavigationMode.Stack)
    .gesture(
      PanGesture({ direction: PanDirection.Right })
        .onActionEnd((event: GestureEvent) => {
          if (event.offsetX > 50) {
            this.getUIContext().getRouter().back();
          }
        })
    )
  }
}

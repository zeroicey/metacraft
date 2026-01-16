import { PersonalInfo, PersonalInfoView, DataType, SwitchInfo, ApiHandler, GenderType } from 'collect_personal_info';

@Entry
@ComponentV2
struct ProfilePage {
  topRectHeight: number = 0;
  
  @Local switchInfo: SwitchInfo = {
    showAvatar: true,
    showNickName: true,
    showFullName: true,
    showGender: true,
    showBirthDate: true,
    showPhoneNumber: true,
    showIntroDesc: true,
  };
  @Local personalInfo: PersonalInfo = {
    avatar: 'https://agc-storage-drcn.platform.dbankcloud.cn/v0/news-hnp2d/avatar%2Favatar_12.jpg',
    nickName: '华为用户',
    gender: GenderType.MALE,
    birthDate: new Date('1990-10-01'),
    fullName: '张三',
    phoneNumber: '10000001111',
    introDesc: '在自己的小世界里,做一个快乐的普通人,把节奏放慢,享受生活!十六岁的青春张扬 十八岁的未来可期,携零零碎碎的美好奔赴未知的将来!',
  };
  @Local apiInfo: ApiHandler = {
    updateAvatar: (param: Object) => new Promise((resolve) => resolve(undefined)),
    updateNickName: (param: Object) => new Promise((resolve) => resolve(undefined)),
    updateGender: (param: Object) => new Promise((resolve) => resolve(undefined)),
    updateBirth: (param: Object) => new Promise((resolve) => resolve(undefined)),
    updateFullName: (param: Object) => new Promise((resolve) => resolve(undefined)),
    updateIntro: (param: Object) => new Promise((resolve) => resolve(undefined)),
    updatePhoneInfo: {
      codeLength: 6,
      sendCode: (phone: string) => {
        return new Promise((resolve, reject) => {
          setTimeout(() => {
            resolve(undefined)
          }, 1000)
        })
      },
      verifyCode: (phone: string, code: string) => {
        return new Promise((resolve, reject) => {
          setTimeout(() => {
            resolve(undefined)
          }, 1000)
        })
      },
    },
  };

  aboutToAppear(): void {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
  }

  build() {
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

        Text('个人资料')
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

      // 个人信息组件
      Column() {
        PersonalInfoView({
          switchInfo: this.switchInfo,
          personalInfo: this.personalInfo,
          apiInfo: this.apiInfo,
          onChange: (type: DataType, data: PersonalInfo) => {
            console.log('personal info change, all data: ' + JSON.stringify(data));
          },
        })
      }
      .width('100%')
      .layoutWeight(1)
      .padding(16)
    }
    .width('100%')
    .height('100%')
    .backgroundColor($r('sys.color.background_secondary'))
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

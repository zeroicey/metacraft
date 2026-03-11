import {
  PersonalInfo,
  PersonalInfoView,
  DataType,
  SwitchInfo,
  ApiHandler,
  GenderType
} from 'collect_personal_info';
import { BasePageHeader } from '../../../shared/ui/BasePageHeader';
import {
  createProfileSwitchInfo,
  createProfilePersonalInfo,
  createProfileApiHandler,
} from '../state/ProfileState';

@ComponentV2
export struct ProfileScreen {
  topRectHeight: number = 0;

  @Local switchInfo: SwitchInfo = createProfileSwitchInfo();
  @Local personalInfo: PersonalInfo = createProfilePersonalInfo();
  @Local apiInfo: ApiHandler = createProfileApiHandler();

  aboutToAppear(): void {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
  }

  build() {
    Column() {
      BasePageHeader({
        title: '个人资料',
        topRectHeight: this.getUIContext().px2vp(this.topRectHeight),
        onBack: () => {
          this.getUIContext().getRouter().back();
        }
      })

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
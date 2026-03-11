import {
  SettingItem,
  SettingGroup,
  SettingType,
  SettingView,
  SupportedSetting,
  WebModel,
} from 'app_setting';
import { BasePageHeader } from '../../../shared/ui/BasePageHeader';
import { createSettingsGroups } from '../state/SettingsState';

@ComponentV2
export struct SettingsScreen {
  topRectHeight: number = 0;
  @Local navPathStack: NavPathStack = new NavPathStack();
  @Local settingGroups: SettingGroup[] = [];

  aboutToAppear(): void {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
    this.settingGroups = createSettingsGroups(() => {
      this.getUIContext().getRouter().pushUrl({ url: 'pages/Profile' });
    });
  }

  build() {
    Navigation(this.navPathStack) {
      Column() {
        BasePageHeader({
          title: '设置',
          topRectHeight: this.getUIContext().px2vp(this.topRectHeight),
          onBack: () => {
            this.getUIContext().getRouter().back();
          }
        })

        SettingView({
          pathStack: this.navPathStack,
          settingList: this.settingGroups,
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
import {
  SettingItem,
  SettingGroup,
  SettingType,
  SupportedSetting,
  WebModel,
} from 'app_setting';

export function createSettingsGroups(openProfile: () => void): SettingGroup[] {
  const recommendItem = new SettingItem()
    .setType(SettingType.SWITCH)
    .setLabel('个性化推荐')
    .setSwitchValue(true)
    .setClickEvent((isOn) => {
      recommendItem.setSwitchValue(isOn as boolean);
    });

  return [
    new SettingGroup().setList([
      new SettingItem()
        .setType(SettingType.TEXT)
        .setLabel('个人信息')
        .setClickEvent(() => {
          openProfile();
        }),
    ]),
    new SettingGroup().setList([
      new SettingItem()
        .setSupportedType(SupportedSetting.LIGHT_DARK_MODE)
        .setSelectIndex(0)
        .setOnChange((_colorMode) => {
        }),
    ]),
    new SettingGroup().setList([
      new SettingItem()
        .setSupportedType(SupportedSetting.PUSH_SWITCH)
        .setOnChange((_isOn) => {
        }),
      new SettingItem()
        .setSupportedType(SupportedSetting.CLEAR_CACHE),
    ]),
    new SettingGroup()
      .setTitle('数据和隐私')
      .setList([
        recommendItem,
      ]),
    new SettingGroup()
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
      ]),
    new SettingGroup()
      .setTitle('更多')
      .setList([
        new SettingItem()
          .setSupportedType(SupportedSetting.CHECK_VERSION),
      ]),
  ];
}
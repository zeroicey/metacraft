@ComponentV2
export struct BottomSheetMenu {
  @Require @Param onMenuItemClick: (menuId: string) => void = () => {};

  @Local showSheet: boolean = false;

  build() {
    Button() {
      SymbolGlyph($r('sys.symbol.dot_grid_1x2'))
        .fontWeight(FontWeight.Normal)
        .fontSize(22)
        .fontColor([$r('sys.color.ohos_id_color_titlebar_icon')])
    }
    .backgroundColor(Color.Transparent)
    .height(40)
    .width(40)
    .onClick(() => {
      this.showSheet = true;
    })
    .bindSheet(this.showSheet, this.buildSheetContent(), {
      detents: [SheetSize.MEDIUM, SheetSize.LARGE],
      preferType: SheetType.BOTTOM,
      showClose: true,
      title: { title: '菜单' },
      dragBar: true,
      onDisappear: () => {
        this.showSheet = false;
      }
    })
  }

  @Builder
  buildSheetContent() {
    Column() {
      List() {
        ListItem() {
          Row() {
            Text('👤')
              .fontSize(24)
              .margin({ right: 12 })
            Text('个人中心')
              .fontSize(16)
              .fontColor($r('sys.color.ohos_id_color_text_primary'))
          }
          .width('100%')
          .height(56)
          .padding({ left: 16, right: 16 })
          .onClick(() => {
            this.showSheet = false;
            this.onMenuItemClick('profile');
          })
        }

        ListItem() {
          Row() {
            Text('⚙️')
              .fontSize(24)
              .margin({ right: 12 })
            Text('设置')
              .fontSize(16)
              .fontColor($r('sys.color.ohos_id_color_text_primary'))
          }
          .width('100%')
          .height(56)
          .padding({ left: 16, right: 16 })
          .onClick(() => {
            this.showSheet = false;
            this.onMenuItemClick('settings');
          })
        }

        ListItem() {
          Row() {
            Text('ℹ️')
              .fontSize(24)
              .margin({ right: 12 })
            Text('关于')
              .fontSize(16)
              .fontColor($r('sys.color.ohos_id_color_text_primary'))
          }
          .width('100%')
          .height(56)
          .padding({ left: 16, right: 16 })
          .onClick(() => {
            this.showSheet = false;
            this.onMenuItemClick('about');
          })
        }
      }
      .width('100%')
      .divider({ strokeWidth: 1, color: $r('sys.color.ohos_id_color_list_separator') })
    }
    .width('100%')
    .padding({ top: 8, bottom: 8 })
  }
}
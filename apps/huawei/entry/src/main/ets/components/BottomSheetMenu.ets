/**
 * 底部菜单弹窗组件
 * 提供半模态底部弹窗菜单功能
 * 
 * @使用示例:
 * ```typescript
 * import { BottomSheetMenu } from '../components/BottomSheetMenu';
 * 
 * // 在你的页面中使用:
 * BottomSheetMenu({
 *   onMenuItemClick: (menuId: string) => {
 *     console.info('点击了菜单项:', menuId);
 *     // 根据 menuId 处理不同的菜单项点击
 *     if (menuId === 'profile') {
 *       // 跳转到个人中心
 *     } else if (menuId === 'settings') {
 *       // 跳转到设置
 *     }
 *   }
 * })
 * ```
 * 
 * @组件功能:
 * 1. 点击组件(默认为按钮)触发底部弹窗
 * 2. 弹窗内显示菜单列表
 * 3. 点击菜单项后自动关闭弹窗并回调
 * 4. 支持拖拽调整高度
 * 5. 支持下拉关闭
 */
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

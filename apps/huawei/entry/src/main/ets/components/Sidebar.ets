// components/Sidebar.ets
import { HdsSideBar } from '@kit.UIDesignKit';

@ComponentV2
export struct Sidebar {
  @Param @Require isShowSidebar: boolean;
  @Param onSideBarChange: (isShow: boolean) => void = () => {};
  @Param topRectHeight: number = 0;

  @Local isSideBarContainerMask: boolean = false;
  @Local isAutoHide: boolean = false;
  @Local selectedApp: string = '云养猫互动应用开发'; // 当前选中的应用

  @Builder
  SideBarPanelBuilder() {
    Column() {
      // Header: Logo + MetaCraft 品牌字 + 关闭按钮
      Row({ space: 8 }) {
        Row({ space: 8 }) {
          Circle()
            .width(24)
            .height(24)
            .backgroundColor('#222222')

          Text('元创')
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
        }
        .layoutWeight(1)

        Button() {
          SymbolGlyph($r('sys.symbol.chevron_right'))
            .fontWeight(FontWeight.Normal)
            .fontSize(18)
            .fontColor([$r('sys.color.ohos_id_color_titlebar_icon')])
        }
        .backgroundColor(Color.Transparent)
        .height(32)
        .width(32)
        .onClick(() => {
          this.onSideBarChange(false);
        })
      }
      .width('100%')
      .padding({ left: 16, right: 12, top: this.topRectHeight + 16, bottom: 12 })

      // Primary Action: 创建新应用
      Button() {
        Row({ space: 8 }) {
          SymbolGlyph($r('sys.symbol.ellipsis_message'))
            .fontSize(18)
            .fontColor(['#333333'])

          Text('创建新应用')
            .fontSize(14)
            .fontColor('#333333')
        }
      }
        .width('90%')
        .height(40)
        .margin({ left: 16, right: 16, bottom: 16 })
        .backgroundColor('#F2F2F2')
        .borderRadius(10)
        .onClick(() => {
          // TODO: 在主工程中，这里可以通过事件或回调重置首页 Stage 为 Landing
          this.onSideBarChange(false);
        })

      // Navigation 区域
      Column({ space: 4 }) {
        this.buildNavItem('我的元应用', 'more');
        this.buildNavItem('我的收藏', 'star');
        this.buildNavItem('元数据中心', 'folder');
      }
      .padding({ left: 16, right: 16, bottom: 12 })

      // Divider 分隔线
      Divider()
        .margin({ left: 16, right: 16 })

      // History 最近运行
      Column({ space: 16 }) {
        // 今天
        Column({ space: 4 }) {
          Text('今天')
            .fontSize(12)
            .fontColor('#999999')
            .margin({ bottom: 4 })
            .width('100%')
            .textAlign(TextAlign.Start)

          Column({ space: 4 }) {
            ForEach(['云养猫互动应用开发', '像素画生成器开发'], (item: string) => {
              Row({ space: 12 }) {
                Text(item)
                  .fontSize(14)
                  .fontWeight(this.selectedApp === item ? FontWeight.Bold : FontWeight.Medium)
                  .fontColor(this.selectedApp === item ? '#000000' : '#333333')
                  .layoutWeight(1)
                  .maxLines(1)
              }
              .width('100%')
              .padding({ top: 8, bottom: 8, left: 12, right: 12 })
              .backgroundColor(this.selectedApp === item ? '#F2F2F2' : Color.Transparent)
              .borderRadius(10)
              .onClick(() => {
                this.selectedApp = item;
                // TODO: 点击打开对应应用的对话
                this.onSideBarChange(false);
              })
            }, (item: string) => item)
          }
          .width('100%')
        }

        // 昨天
        Column({ space: 4 }) {
          Text('昨天')
            .fontSize(12)
            .fontColor('#999999')
            .margin({ bottom: 4 })
            .width('100%')
            .textAlign(TextAlign.Start)

          Column({ space: 4 }) {
            ForEach(['制作浪漫烟花互动应用', '智能日程助手开发'], (item: string) => {
              Row({ space: 12 }) {
                Text(item)
                  .fontSize(14)
                  .fontWeight(this.selectedApp === item ? FontWeight.Bold : FontWeight.Medium)
                  .fontColor(this.selectedApp === item ? '#000000' : '#333333')
                  .layoutWeight(1)
                  .maxLines(1)
              }
              .width('100%')
              .padding({ top: 8, bottom: 8, left: 12, right: 12 })
              .backgroundColor(this.selectedApp === item ? '#F2F2F2' : Color.Transparent)
              .borderRadius(10)
              .onClick(() => {
                this.selectedApp = item;
                // TODO: 点击打开对应应用的对话
                this.onSideBarChange(false);
              })
            }, (item: string) => item)
          }
          .width('100%')
        }

        // 30天内
        Column({ space: 4 }) {
          Text('30天内')
            .fontSize(12)
            .fontColor('#999999')
            .margin({ bottom: 4 })
            .width('100%')
            .textAlign(TextAlign.Start)

          Column({ space: 4 }) {
            ForEach(['待办事项管理器开发', '天气预报小组件', '阅读笔记应用'], (item: string) => {
              Row({ space: 12 }) {
                Text(item)
                  .fontSize(14)
                  .fontWeight(this.selectedApp === item ? FontWeight.Bold : FontWeight.Medium)
                  .fontColor(this.selectedApp === item ? '#000000' : '#333333')
                  .layoutWeight(1)
                  .maxLines(1)
              }
              .width('100%')
              .padding({ top: 8, bottom: 8, left: 12, right: 12 })
              .backgroundColor(this.selectedApp === item ? '#F2F2F2' : Color.Transparent)
              .borderRadius(10)
              .onClick(() => {
                this.selectedApp = item;
                // TODO: 点击打开对应应用的对话
                this.onSideBarChange(false);
              })
            }, (item: string) => item)
          }
          .width('100%')
        }
      }
      .padding({ left: 16, right: 16, top: 16, bottom: 16 })

      Blank().layoutWeight(1)

      // Footer: 用户头像 + 昵称 + 设置入口
      Row({ space: 12 }) {
        Row({ space: 8 }) {
          Circle()
            .width(32)
            .height(32)
            .backgroundColor('#444444')

          Column() {
            Text('雪宁韵')
              .fontSize(14)
              .fontWeight(FontWeight.Medium)
          }
        }
        .layoutWeight(1)
        .onClick(() => {
          this.onSideBarChange(false);
          setTimeout(() => {
            this.getUIContext().getRouter().pushUrl({ url: 'pages/Profile' });
          }, 200);
        })

        Button() {
          SymbolGlyph($r('sys.symbol.gearshape'))
            .fontWeight(FontWeight.Normal)
            .fontSize(20)
            .fontColor([$r('sys.color.ohos_id_color_titlebar_icon')])
        }
        .backgroundColor(Color.Transparent)
        .height(32)
        .width(32)
        .onClick(() => {
          this.onSideBarChange(false);
          setTimeout(() => {
            this.getUIContext().getRouter().pushUrl({ url: 'pages/Setting' });
          }, 200);
        })
      }
      .padding({ left: 16, right: 16, bottom: 24 })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
    .gesture(
      PanGesture({ direction: PanDirection.Left })
        .onActionEnd((event: GestureEvent) => {
          if (event.offsetX < -50) {
            this.onSideBarChange(false);
          }
        })
    )
  }

  @Builder
  private buildNavItem(label: string, iconName: string) {
    Row({ space: 8 }) {
      SymbolGlyph($r(`sys.symbol.${iconName}`))
        .fontSize(16)
        .fontColor(['#666666'])

      Text(label)
        .fontSize(14)
        .layoutWeight(1)

      SymbolGlyph($r('sys.symbol.chevron_right'))
        .fontSize(14)
        .fontColor(['#999999'])
    }
    .width('100%')
    .padding({ top: 8, bottom: 8 })
    .onClick(() => {
      this.onSideBarChange(false);
      
      // 根据标签跳转到对应页面
      let targetUrl = '';
      if (label === '我的元应用') {
        targetUrl = 'pages/MyApps';
      } else if (label === '我的收藏') {
        targetUrl = 'pages/MyFavorites';
      } else if (label === '元数据中心') {
        targetUrl = 'pages/DataCenter';
      }
      
      if (targetUrl) {
        // 延迟跳转，确保侧边栏动画完成
        setTimeout(() => {
          this.getUIContext().getRouter().pushUrl({ url: targetUrl });
        }, 200);
      }
    })
  }

  @BuilderParam contentBuilder: () => void;

  @Builder
  ContentWithMask() {
    Stack() {
      Column() {
        this.contentBuilder()
      }
      .width('100%')
      .height('100%')
      .gesture(
        PanGesture({ direction: PanDirection.Right })
          .onActionEnd((event: GestureEvent) => {
            if (!this.isShowSidebar && event.offsetX > 50) {
              this.onSideBarChange(true);
            }
          })
      )

      if (this.isShowSidebar) {
        Column()
          .width('100%')
          .height('100%')
          .backdropBlur(20)
          .backgroundColor('rgba(0, 0, 0, 0.4)')
          .onClick(() => {
            this.onSideBarChange(false);
          })
          .transition(
            TransitionEffect.OPACITY
              .animation({ duration: 300, curve: Curve.EaseInOut })
          )
      }
    }
  }

  @BuilderParam sideBarBuilder: () => void = this.SideBarPanelBuilder;

  @Builder
  HDSSideBarBuilder() {
    HdsSideBar({
      sideBarPanelBuilder: () => {
        this.sideBarBuilder()
      },
      contentPanelBuilder: () => {
        this.ContentWithMask()
      },
      autoHide: this.isAutoHide,
      contentAreaMask: this.isSideBarContainerMask,
      sideBarContainerType: SideBarContainerType.Embed,
      isShowSideBar: this.isShowSidebar,
      $isShowSideBar: (isShowSidebar: boolean) => {
        this.onSideBarChange(!isShowSidebar);
      },
      minSideBarWidth: '75%',
      maxSideBarWidth: '75%',
      sideBarWidth: '75%',
    })
  }

  build() {
    Stack() {
      this.HDSSideBarBuilder()
    }
  }
}

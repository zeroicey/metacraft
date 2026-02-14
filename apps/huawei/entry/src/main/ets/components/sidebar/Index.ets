// components/Sidebar.ets
import { HdsSideBar } from '@kit.UIDesignKit';
import { SessionList } from './SessionList';
import { chatSessionApi } from '../../api/chatSession';

@ComponentV2
export struct Sidebar {
  @Param @Require isShowSidebar: boolean;
  @Param onSideBarChange: (isShow: boolean) => void = () => {};
  @Param topRectHeight: number = 0;
  @Param selectedSessionId: string = '';
  @Event onSessionChange: (sessionId: string) => void = () => {};

  @Local isSideBarContainerMask: boolean = false;
  @Local isAutoHide: boolean = false;
  @Local selectedApp: string = '云养猫互动应用开发'; // 当前选中的应用
  @Local sessionListRefreshSignal: number = 0;

  @Monitor('isShowSidebar')
  watchSidebarStatus() {
    if (this.isShowSidebar) {
      this.sessionListRefreshSignal++;
    }
  }

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
          this.handleCreateApp();
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
      Scroll() {
        Column() {
          SessionList({
            selectedSessionId: this.selectedSessionId,
            refreshSignal: this.sessionListRefreshSignal,
            onSessionClick: (sessionId: string) => {
              this.onSessionChange(sessionId);
              // TODO: 触发主页面切换到对应会话
              this.onSideBarChange(false);
            }
          })
        }
        .padding({ left: 16, right: 16, top: 16, bottom: 16 })
      }
      .scrollBar(BarState.Auto)
      .edgeEffect(EdgeEffect.Spring)
      .layoutWeight(1) // 让列表占据剩余空间，但保留底部 Footer
      .align(Alignment.TopStart)

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

  private async handleCreateApp() {
    const DEFAULT_TITLE = '未命名应用';
    
    try {
      // 1. 获取所有会话
      const sessionsResult = await chatSessionApi.getUserSessions();
      let targetSessionId: string = '';

      if (sessionsResult.success && sessionsResult.data) {
        // 2. 查找是否有同名默认会话
        const candidateSessions = sessionsResult.data.filter((s) => s.title === DEFAULT_TITLE);
        
        // 3. 检查这些会话是否为空
        for (const session of candidateSessions) {
          const messagesResult = await chatSessionApi.getSessionMessages(session.sessionId);
          if (messagesResult.success && messagesResult.data && messagesResult.data.length === 0) {
            targetSessionId = session.sessionId;
            break; // 找到一个空的默认会话，直接复用
          }
        }
      }

      if (targetSessionId) {
        // 复用现有会话
        this.onSessionChange(targetSessionId);
      } else {
        // 创建新会话
        const createResult = await chatSessionApi.createSession(DEFAULT_TITLE);
        if (createResult.success && createResult.data) {
          this.onSessionChange(createResult.data.sessionId);
        } else {
          console.error('[Sidebar] Failed to create session:', createResult.error);
        }
      }
    } catch (error) {
      console.error('[Sidebar] Error in handleCreateApp:', error);
    } finally {
      // 无论成功失败，关闭侧边栏
      this.onSideBarChange(false);
    }
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

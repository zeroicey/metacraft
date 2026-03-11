import { SessionSidebar } from '../../session/components/SessionSidebar';
import { BottomSheetMenu } from '../../../shared/layout/BottomSheetMenu';
import { setOnUnauthorizedCallback } from '../../../core/http/Interceptors';
import {
  LoginSheetUtils,
  LoginInfoOptions,
  LoginController,
  ApiController,
  LoginParams,
  LoginChannelType,
  LoginFailError
} from 'aggregated_login';
import { authService } from '../../auth/service/AuthService';
import { ChatPanel } from '../../chat/components/ChatPanel';
import { PromptAction } from '@kit.ArkUI';
import { sessionService } from '../../session/service/SessionService';
import { readSafeAreaHeights, resolveInitialSessionId } from '../state/WorkspaceState';

@ComponentV2
export struct WorkspaceScreen {
  topRectHeight: number = 0;
  bottomRectHeight: number = 0;
  @Local showSideBar: boolean = false;
  @Local currentSessionId: string = '';

  private loginController: LoginController = new LoginController();
  private promptAction?: PromptAction;

  aboutToAppear(): void {
    const GeneratedDestructArray_1 = readSafeAreaHeights();
    const topRectHeight = GeneratedDestructArray_1[0];
    const bottomRectHeight = GeneratedDestructArray_1[1];
    this.topRectHeight = topRectHeight;
    this.bottomRectHeight = bottomRectHeight;

    setOnUnauthorizedCallback(() => {
      console.info('[Index] 收到未授权回调，弹出登录弹窗');
      this.openLoginSheet();
    });

    if (!authService.isLoggedIn()) {
      console.info('[Index] 未检测到token，弹出登录弹窗');
      setTimeout(() => {
        this.openLoginSheet();
      }, 500);
    } else {
      console.info('[Index] 已检测到token，跳过登录');
      this.initSessions();
    }
  }

  async initSessions() {
    const result = await sessionService.getUserSessions();
    if (result.success && result.data) {
      this.currentSessionId = resolveInitialSessionId(result.data.map(session => session.sessionId));
    } else {
      console.info('[Index] No existing sessions');
    }
  }

  private handleLoginSuccess = (loginType: LoginChannelType): void => {
    console.info('[Index] 登录成功，登录类型:', loginType);
    this.initSessions();
  }

  private huaweiLogin = async (_loginParams: LoginParams): Promise<void> => {
    console.info('[Index] 华为账号登录成功');
    LoginSheetUtils.close();
    this.handleLoginSuccess(LoginChannelType.HUAWEI);
  }

  private wxLogin = async (): Promise<void> => {}

  private verifyCodeLogin = async (_loginParams: LoginParams): Promise<void> => {}

  private obtainVerifyCode = async () => {}

  private handleLoginFail = async (loginChannelType: LoginChannelType, error: LoginFailError) => {
    console.info('[Index] ===== 华为账号登录失败，尝试后端登录 =====');
    console.info('[Index] 登录渠道:', loginChannelType);
    console.info('[Index] 错误信息:', JSON.stringify(error));

    if (loginChannelType === LoginChannelType.HUAWEI) {
      const result = await authService.login();

      if (result.success) {
        console.info('[Index] 后端登录成功');
        if (this.promptAction) {
          this.promptAction.showToast({ message: '登录成功' });
        }
        this.loginController.clearLoginInfo();
        LoginSheetUtils.close();
        this.handleLoginSuccess(LoginChannelType.HUAWEI);
      } else {
        console.error('[Index] 后端登录失败:', result.error);
        if (this.promptAction) {
          this.promptAction.showToast({ message: '登录失败：' + (result.error || '未知错误') });
        }
      }
    }
  }

  private getLoginOptions(): LoginInfoOptions {
    return {
      icon: $r('app.media.startIcon'),
      themeColor: '#0A59F7',
      isSheet: true,
      autoSheet: false,
      isPersistence: false,
      loginTypes: [],
      privacyContent: [
        {
          privacyLabel: '《用户协议》',
          privacyUrl: '<html><body><h1>用户协议</h1></body></html>',
          privacyTag: '用户协议'
        },
        {
          privacyLabel: '《隐私政策》',
          privacyUrl: '<html><body><h1>隐私政策</h1></body></html>',
          privacyTag: '隐私政策'
        },
        {
          privacyLabel: '《华为账号用户认证协议》',
          privacyUrl: '<html><body><h1>华为账号用户认证协议</h1></body></html>',
          privacyTag: '华为账号用户认证协议'
        }
      ],
      apiController: new ApiController(this.huaweiLogin, this.wxLogin, this.verifyCodeLogin),
      handleLoginFail: this.handleLoginFail,
      obtainVerifyCode: this.obtainVerifyCode,
      loginController: this.loginController,
    };
  }

  openLoginSheet(): void {
    LoginSheetUtils.open(this.getLoginOptions());
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
  buildContent() {
    Column() {
      this.buildTopBar();

      ChatPanel({
        bottomPadding: this.getUIContext().px2vp(this.bottomRectHeight) + 5,
        sessionId: this.currentSessionId
      })
        .width('100%')
        .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
  }

  build() {
    Stack() {
      SessionSidebar({
        isShowSidebar: this.showSideBar,
        onSideBarChange: (isShow: boolean) => {
          this.showSideBar = isShow;
        },
        topRectHeight: this.getUIContext().px2vp(this.topRectHeight),
        selectedSessionId: this.currentSessionId,
        onSessionChange: (sessionId: string) => {
          this.currentSessionId = sessionId;
        },
        contentBuilder: () => {
          this.buildContent();
        }
      })
    }
    .width('100%')
    .height('100%')
    .onAppear(() => {
      this.promptAction = this.getUIContext().getPromptAction();
    })
  }

  handleMenuClick(menuId: string) {
    console.info('点击了菜单项:', menuId);
    switch (menuId) {
      case 'profile':
        break;
      case 'settings':
        break;
      case 'about':
        break;
    }
  }
}
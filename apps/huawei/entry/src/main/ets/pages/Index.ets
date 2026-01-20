import { Sidebar } from '../components/Sidebar';
import { BottomSheetMenu } from '../components/BottomSheetMenu';
import { setOnUnauthorizedCallback } from '../utils/http/Interceptors';
import { LoginSheetUtils, LoginInfoOptions, LoginController, ApiController, LoginParams, LoginChannelType, LoginFailError } from 'aggregated_login';
import { authApi } from '../api/auth';
import { aiChatApi } from '../api/aiChat';
import { ChatPanel } from '../components/ChatPanel';
import { PromptAction } from '@kit.ArkUI';

interface Message {
  id: number;
  text: string;
  isUser: boolean;
}

@Entry
@ComponentV2
struct Index {
  topRectHeight: number = 0;
  bottomRectHeight: number = 0;
  @Local messages: Message[] = [];
  @Local inputText: string = '';

  @Local showSideBar: boolean = false;
  
  private loginController: LoginController = new LoginController();
  private promptAction?: PromptAction;

  aboutToAppear(): void {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
    this.bottomRectHeight = AppStorage.get<number>('bottomRectHeight') ?? 0;
    
    // 设置401/403未授权回调，自动弹出登录
    setOnUnauthorizedCallback(() => {
      console.info('[Index] 收到未授权回调，弹出登录弹窗');
      this.openLoginSheet();
    });
    
    // 检测token，如果没有则弹出登录弹窗
    if (!authApi.isLoggedIn()) {
      console.info('[Index] 未检测到token，弹出登录弹窗');
      setTimeout(() => {
        this.openLoginSheet();
      }, 100);
    } else {
      console.info('[Index] 已检测到token，跳过登录');
    }

    
  }
  
  // 登录成功处理
  private handleLoginSuccess = (loginType: LoginChannelType): void => {
    console.info('[Index] 登录成功，登录类型:', loginType);
  }
  
  // 华为登录成功回调 - 实际上不会调用，因为会走失败流程
  private huaweiLogin = async (loginParams: LoginParams): Promise<void> => {
    console.info('[Index] 华为账号登录成功');
    LoginSheetUtils.close();
    this.handleLoginSuccess(LoginChannelType.HUAWEI);
  }
  
  // 微信登录空实现
  private wxLogin = async (): Promise<void> => {}
  
  // 验证码登录空实现
  private verifyCodeLogin = async (loginParams: LoginParams): Promise<void> => {}
  
  // 获取验证码空实现
  private obtainVerifyCode = async () => {}
  
  // 登录失败处理 - 直接调用后端API登录
  private handleLoginFail = async (loginChannelType: LoginChannelType, error: LoginFailError) => {
    console.info('[Index] ===== 华为账号登录失败，尝试后端登录 =====');
    console.info('[Index] 登录渠道:', loginChannelType);
    console.info('[Index] 错误信息:', JSON.stringify(error));
    
    if (loginChannelType === LoginChannelType.HUAWEI) {
      const result = await authApi.login();
      
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
  
  // 获取登录配置
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
  
  // 打开登录弹窗
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
        bottomPadding: this.getUIContext().px2vp(this.bottomRectHeight) + 5
      })
        .width('100%')
        .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
  }

  build() {
    Stack() {
      Sidebar({
        isShowSidebar: this.showSideBar,
        topRectHeight: this.getUIContext().px2vp(this.topRectHeight),
        onSideBarChange: (isShow: boolean) => {
          this.showSideBar = isShow;
        },
        contentBuilder: () => {
          this.buildContent();
        }
      })
    }
    .width('100%')
    .height('100%')
    .onAppear(() => {
      // 初始化promptAction
      this.promptAction = this.getUIContext().getPromptAction();
    })
  }

  // 菜单项点击处理
  handleMenuClick(menuId: string) {
    console.info('点击了菜单项:', menuId);
    switch (menuId) {
      case 'profile':
        // 跳转到个人中心
        break;
      case 'settings':
        // 跳转到设置页面
        break;
      case 'about':
        // 显示关于信息
        break;
    }
  }
}

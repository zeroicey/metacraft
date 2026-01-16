import {
  Channel,
  LoginController,
  LoginChannelType,
  ApiController,
  LoginInfoOptions,
  LoginParams,
  LoginSheetUtils,
  LoginType,
  LoginFailError
} from 'aggregated_login';
import { PromptAction } from '@kit.ArkUI';
import { BusinessError } from '@kit.BasicServicesKit';

/**
 * 登录管理器组件
 * 提供半模态登录功能,仅支持华为账号一键登录
 * 
 * @使用示例:
 * ```typescript
 * import { LoginManager } from '../components/LoginManager';
 * import { LoginChannelType } from 'aggregated_login';
 * 
 * // 在你的页面中使用:
 * LoginManager({
 *   onLoginSuccess: (loginType: LoginChannelType) => {
 *     console.info('登录成功,登录类型:', loginType);
 *     // 这里处理登录后的业务逻辑
 *     // 比如:
 *     // 1. 调用后端接口验证 unionID
 *     // 2. 获取用户信息
 *     // 3. 跳转到主页
 *   }
 * })
 * ```
 * 
 * @登录流程:
 * 1. 用户点击登录按钮
 * 2. 弹出华为账号一键登录半模态
 * 3. 用户选择华为账号并授权
 * 4. 登录成功后回调 onLoginSuccess,传入 LoginChannelType.HUAWEI
 * 5. 在回调中可以获取 unionID 和 authorizationCode(查看控制台日志)
 * 
 * @配置需求:
 * 1. 在 module.json5 中配置有效的 client_id
 * 2. 配置应用签名指纹
 * 3. 在华为开发者联盟申请账号服务权限
 */
@ComponentV2
export struct LoginManager {
  @Require @Param onLoginSuccess: (loginType: LoginChannelType) => void = () => {};

  private promptAction: PromptAction = this.getUIContext().getPromptAction();
  private controller: LoginController = new LoginController();

  // 华为一键登录成功回调
  private huaweiLogin = async (loginParams: LoginParams): Promise<void> => {
    console.info('===== 华为账号登录成功 =====');
    console.info('unionID:', loginParams.unionID);
    console.info('authorizationCode:', loginParams.authorizationCode);
    console.info('完整参数:', JSON.stringify(loginParams));
    console.info('========================');
    
    this.promptAction.showToast({ 
      message: `华为登录成功\nunionID: ${loginParams.unionID?.substring(0, 8)}...` 
    });
    LoginSheetUtils.close();
    this.onLoginSuccess(LoginChannelType.HUAWEI);
  }

  // 微信登录空实现(库要求)
  private wxLogin = async (): Promise<void> => {
    // 不使用微信登录
  }

  // 验证码登录空实现(库要求)
  private verifyCodeLogin = async (loginParams: LoginParams): Promise<void> => {
    // 不使用验证码登录
  }

  // 获取验证码空实现(库要求)
  private obtainVerifyCode = async () => {
    // 不使用验证码
  }

  // 登录失败处理
  private handleLoginFail = (loginChannelType: LoginChannelType, error: LoginFailError) => {
    console.error('===== 华为账号登录失败 =====');
    console.error('登录渠道:', loginChannelType);
    console.error('错误信息:', JSON.stringify(error));
    console.error('========================');
    
    if (loginChannelType === LoginChannelType.HUAWEI) {
      const businessError = error as BusinessError;
      const errorMsg = ERROR_MSG_MAP[businessError.code] ?? '未知错误。';
      
      console.error('错误码:', businessError.code);
      console.error('错误描述:', errorMsg);
      
      this.promptAction.showDialog({
        title: '温馨提示',
        message: errorMsg + '是否先模拟登录成功?',
        buttons: [
          { text: '取消', color: '#000000' },
          { text: '确认', color: '#000000' },
        ]
      }, (err: BusinessError, data) => {
        if (data.index === 1) {
          console.info('用户选择模拟登录成功');
          this.promptAction.showToast({ message: '华为一键登录成功' });
          this.controller.clearLoginInfo();
          LoginSheetUtils.close();
          this.onLoginSuccess(LoginChannelType.HUAWEI);
        }
      });
      return;
    }
  }

  // 登录配置参数
  private getLoginParams(): LoginInfoOptions {
    return {
      icon: $r('app.media.startIcon'),
      themeColor: '#0A59F7',
      isSheet: true,
      autoSheet: false,
      isPersistence: false,
      loginTypes: [], // 只使用华为账号一键登录
      privacyContent: [
        {
          privacyLabel: '《用户协议》',
          privacyUrl: ProtocolData.termOfServiceProtocol,
          privacyTag: '用户协议'
        },
        {
          privacyLabel: '《隐私政策》',
          privacyUrl: ProtocolData.privacyProtocol,
          privacyTag: '隐私政策'
        },
        {
          privacyLabel: '《华为账号用户认证协议》',
          privacyUrl: ProtocolData.huaweiUserProtocol,
          privacyTag: '华为账号用户认证协议'
        }
      ],
      apiController: new ApiController(this.huaweiLogin, this.wxLogin, this.verifyCodeLogin),
      handleLoginFail: this.handleLoginFail,
      obtainVerifyCode: this.obtainVerifyCode,
      loginController: this.controller,
    };
  }

  /**
   * 打开登录弹窗
   */
  openLoginSheet() {
    LoginSheetUtils.open(this.getLoginParams());
  }

  build() {
    Button('登录')
      .backgroundColor(Color.Transparent)
      .fontSize(14)
      .fontColor($r('sys.color.ohos_id_color_titlebar_icon'))
      .height(40)
      .onClick(() => {
        this.openLoginSheet();
      })
  }
}

// 协议数据
class ProtocolData {
  static termOfServiceProtocol: string =
    '<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>用户协议</title></head><body><h1>用户协议</h1><p>这是用户协议的示例内容。</p></body></html>';
  static privacyProtocol: string =
    '<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>隐私政策</title></head><body><h1>隐私政策</h1><p>这是隐私政策的示例内容。</p></body></html>';
  static huaweiUserProtocol: string =
    '<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>华为账号用户认证协议</title></head><body><h1>华为账号用户认证协议</h1><p>这是华为账号用户认证协议的示例内容。</p></body></html>';
}

// 错误码映射表
const ERROR_MSG_MAP: Record<number, string> = {
  1001502001: '用户未登录华为账号,请前往设置-登录华为账号。',
  1001500001: '应用指纹证书校验失败,请检查client_id、应用指纹证书配置。',
  1001502003: 'Client ID无效,请检查module.json5中的client_id配置是否正确。',
  1001502014: '应用未申请scopes或permissions权限。',
};

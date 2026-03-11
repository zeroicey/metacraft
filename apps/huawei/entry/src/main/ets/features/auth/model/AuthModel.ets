import { preferences } from '@kit.ArkData';
import { BusinessError } from '@kit.BasicServicesKit';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponseData {
  token: string;
  type: string;
  expiresIn: number;
}

export class TokenService {
  private static instance: TokenService;
  private dataPreferences: preferences.Preferences | null = null;
  private static readonly PREFERENCES_NAME = 'auth_store';
  private static readonly TOKEN_KEY = 'auth_token';
  private static readonly TOKEN_TYPE_KEY = 'token_type';
  private static readonly EXPIRES_IN_KEY = 'expires_in';

  private constructor() {}

  public static getInstance(): TokenService {
    if (!TokenService.instance) {
      TokenService.instance = new TokenService();
    }
    return TokenService.instance;
  }

  public init(context: Context): void {
    try {
      const options: preferences.Options = { name: TokenService.PREFERENCES_NAME };
      this.dataPreferences = preferences.getPreferencesSync(context, options);
      console.info('[TokenService] Preferences initialized');
    } catch (error) {
      const err = error as BusinessError;
      console.error(`[TokenService] Failed to init preferences: ${err.code} - ${err.message}`);
    }
  }

  public saveToken(loginData: LoginResponseData): void {
    if (!this.dataPreferences) {
      console.error('[TokenService] Preferences not initialized');
      return;
    }
    try {
      this.dataPreferences.putSync(TokenService.TOKEN_KEY, loginData.token);
      this.dataPreferences.putSync(TokenService.TOKEN_TYPE_KEY, loginData.type);
      this.dataPreferences.putSync(TokenService.EXPIRES_IN_KEY, loginData.expiresIn);
      this.dataPreferences.flush((err: BusinessError) => {
        if (err) {
          console.error(`[TokenService] Failed to flush: ${err.code} - ${err.message}`);
          return;
        }
        console.info('[TokenService] Token saved successfully');
      });
    } catch (error) {
      const err = error as BusinessError;
      console.error(`[TokenService] Failed to save token: ${err.code} - ${err.message}`);
    }
  }

  public getToken(): string | null {
    if (!this.dataPreferences) {
      console.error('[TokenService] Preferences not initialized');
      return null;
    }
    try {
      const token = this.dataPreferences.getSync(TokenService.TOKEN_KEY, '') as string;
      return token || null;
    } catch (error) {
      const err = error as BusinessError;
      console.error(`[TokenService] Failed to get token: ${err.code} - ${err.message}`);
      return null;
    }
  }

  public getTokenType(): string {
    if (!this.dataPreferences) {
      return 'Bearer';
    }
    try {
      return this.dataPreferences.getSync(TokenService.TOKEN_TYPE_KEY, 'Bearer') as string;
    } catch (error) {
      return 'Bearer';
    }
  }

  public hasToken(): boolean {
    if (!this.dataPreferences) {
      return false;
    }
    try {
      return this.dataPreferences.hasSync(TokenService.TOKEN_KEY);
    } catch (error) {
      return false;
    }
  }

  public clearToken(): void {
    if (!this.dataPreferences) {
      console.error('[TokenService] Preferences not initialized');
      return;
    }
    try {
      this.dataPreferences.deleteSync(TokenService.TOKEN_KEY);
      this.dataPreferences.deleteSync(TokenService.TOKEN_TYPE_KEY);
      this.dataPreferences.deleteSync(TokenService.EXPIRES_IN_KEY);
      this.dataPreferences.flush((err: BusinessError) => {
        if (err) {
          console.error(`[TokenService] Failed to flush after clear: ${err.code} - ${err.message}`);
          return;
        }
        console.info('[TokenService] Token cleared successfully');
      });
    } catch (error) {
      const err = error as BusinessError;
      console.error(`[TokenService] Failed to clear token: ${err.code} - ${err.message}`);
    }
  }
}

export const tokenService = TokenService.getInstance();
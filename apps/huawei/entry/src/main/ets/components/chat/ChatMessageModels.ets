export class GenMessageContent {
  chatBeforeGen: string;
  plan: string;

  constructor(chatBeforeGen: string, plan: string) {
    this.chatBeforeGen = chatBeforeGen;
    this.plan = plan;
  }
}

@Observed
export class ChatMessage {
  id: number;
  text: string;
  chatBeforeGen?: string;
  plan?: string;
  isUser: boolean;
  previewUrl?: string;
  appName?: string;
  appDescription?: string;
  logoUrl?: string;
  isGenMessage?: boolean;

  constructor(
    id: number,
    text: string,
    isUser: boolean,
    previewUrl?: string,
    appName?: string,
    appDescription?: string,
    logoUrl?: string,
    plan?: string,
    chatBeforeGen?: string,
    isGenMessage?: boolean
  ) {
    this.id = id;
    this.text = text;
    this.chatBeforeGen = chatBeforeGen;
    this.plan = plan;
    this.isUser = isUser;
    this.previewUrl = previewUrl;
    this.appName = appName;
    this.appDescription = appDescription;
    this.logoUrl = logoUrl;
    this.isGenMessage = isGenMessage;
  }
}
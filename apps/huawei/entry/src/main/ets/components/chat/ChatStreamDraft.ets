import { ChatMessage } from './ChatMessageModels';

export class ChatStreamDraft {
  private currentText: string = '';
  private currentChatBeforeGen: string = '';
  private currentPlan: string = '';
  private currentIntent: string = '';

  setIntent(intent: string) {
    this.currentIntent = intent.toUpperCase();
  }

  appendTextChunk(text: string) {
    if (this.currentIntent === 'GEN') {
      this.currentChatBeforeGen += text;
      return;
    }
    this.currentText += text;
  }

  appendPlanChunk(text: string) {
    this.currentPlan += text;
  }

  applyToMessage(message: ChatMessage) {
    message.text = this.currentText;
    message.chatBeforeGen = this.currentChatBeforeGen;
    message.plan = this.currentPlan;
    message.isGenMessage = this.currentIntent === 'GEN';
  }

  getText(): string {
    return this.currentText;
  }

  getChatBeforeGen(): string {
    return this.currentChatBeforeGen;
  }

  getPlan(): string {
    return this.currentPlan;
  }
}
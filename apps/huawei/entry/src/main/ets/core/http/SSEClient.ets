import { rcp } from '@kit.RemoteCommunicationKit';
import { BusinessError } from '@kit.BasicServicesKit';
import util from '@ohos.util';
import { AuthInterceptor } from './Interceptors';
import { toApiUrl } from './ApiConfig';

export interface SSEListener {
	onMessage: (data: string) => void;
	onError: (error: string) => void;
	onDone?: () => void;
	onIntent?: (intent: string) => void;
	onPlan?: (data: string) => void;
	onAppInfo?: (appName?: string, appDescription?: string) => void;
	onLogoGenerated?: (logoUrl: string) => void;
	onAppGenerated?: (previewUrl: string, appName?: string, appDescription?: string, logoUrl?: string) => void;
}

class SSEWriteStream implements rcp.WriteStream {
	private decoder: util.TextDecoder;
	private buffer: string = '';
	private listener: SSEListener;
	private previewUrl: string = '';
	private appName: string = '';
	private appDescription: string = '';
	private logoUrl: string = '';

	constructor(listener: SSEListener) {
		this.listener = listener;
		this.decoder = util.TextDecoder.create('utf-8', { ignoreBOM: true });
	}

	async write(buffer: ArrayBuffer): Promise<number | void> {
		const chunk = this.decoder.decodeToString(new Uint8Array(buffer), { stream: true });
		this.buffer += chunk;
		this.buffer = this.buffer.replace(/\r\n/g, '\n');
		let eventEndIndex: number;

		while ((eventEndIndex = this.buffer.indexOf('\n\n')) !== -1) {
			const messageChunk = this.buffer.substring(0, eventEndIndex);
			this.buffer = this.buffer.substring(eventEndIndex + 2);
			this.parseSSEMessage(messageChunk);
		}

		return buffer.byteLength;
	}

	private parseSSEMessage(chunk: string) {
		const lines = chunk.split('\n');
		let eventName = '';
		let dataBuffer: string[] = [];

		for (const line of lines) {
			const trimmedLine = line.trimStart();

			if (trimmedLine.startsWith('event:')) {
				eventName = trimmedLine.substring(6).trim();
			} else if (trimmedLine.startsWith('data:')) {
				let content = trimmedLine.substring(5);
				if (content.startsWith(' ')) {
					content = content.substring(1);
				}
				dataBuffer.push(content);
			}
		}

		const eventData = dataBuffer.join('\n');
		let extractedContent: string = '';

		try {
			const parsed: Record<string, Object> = JSON.parse(eventData);

			if (eventName === 'intent') {
				extractedContent = parsed.intent as string;
				console.info(`[SSEWriteStream] Intent: ${extractedContent}`);
			} else if (eventName === 'message') {
				extractedContent = parsed.content as string;
			} else if (eventName === 'plan') {
				extractedContent = parsed.plan as string;
			} else if (eventName === 'app_info') {
				this.appName = parsed.name as string;
				this.appDescription = parsed.description as string;
				console.info(`[SSEWriteStream] App info received - Name: ${this.appName}`);
				if (this.listener.onAppInfo) {
					this.listener.onAppInfo(this.appName, this.appDescription);
				}
				return;
			} else if (eventName === 'logo_generated') {
				const logoUuid = parsed.uuid as string;
				if (logoUuid && logoUuid.length > 0) {
					this.logoUrl = toApiUrl(`/api/logo/${logoUuid}`);
					console.info(`[SSEWriteStream] Logo generated - UUID: ${logoUuid}`);
					if (this.listener.onLogoGenerated) {
						this.listener.onLogoGenerated(this.logoUrl);
					}
				}
				return;
			} else if (eventName === 'done') {
				extractedContent = '';
			} else if (eventName === 'error') {
				extractedContent = parsed.error as string;
			} else if (eventName === 'app_generated') {
				const appUuid = parsed.uuid as string;
				this.previewUrl = appUuid && appUuid.length > 0
					? toApiUrl(`/api/preview/${appUuid}`)
					: '';
				console.info(`[SSEWriteStream] App generated - UUID: ${appUuid}, Name: ${this.appName}`);
				this.notifyAppGenerated();
				return;
			} else {
				extractedContent = eventData;
			}
		} catch (e) {
			console.info('[SSEWriteStream] JSON parse failed, using raw data:', eventData);
			extractedContent = eventData;
		}

		if (!eventName || eventName === 'message') {
			if (extractedContent.length > 0) {
				this.listener.onMessage(extractedContent);
			}
		} else if (eventName === 'plan') {
			if (this.listener.onPlan && extractedContent.length > 0) {
				this.listener.onPlan(extractedContent);
			}
		} else if (eventName === 'intent') {
			if (this.listener.onIntent && extractedContent.length > 0) {
				this.listener.onIntent(extractedContent);
			}
		} else if (eventName === 'error') {
			if (extractedContent.length > 0) {
				this.listener.onError(extractedContent);
			}
		} else if (eventName === 'done') {
			if (this.listener.onDone) this.listener.onDone();
		}
	}

	private notifyAppGenerated() {
		if (this.listener.onAppGenerated && this.previewUrl.length > 0) {
			this.listener.onAppGenerated(this.previewUrl, this.appName, this.appDescription, this.logoUrl);
		}
	}
}

export class SSEClient {
	private session: rcp.Session | null = null;
	private listener: SSEListener;

	constructor(listener: SSEListener) {
		this.listener = listener;
	}

	public async connect(url: string, body?: object): Promise<void> {
		try {
			this.session = rcp.createSession({
				interceptors: [new AuthInterceptor()],
				headers: {
					'Content-Type': 'application/json',
					'Accept': 'text/event-stream'
				},
				requestConfiguration: {
					transfer: {
						timeout: {
							connectMs: 60000,
							transferMs: 3600000
						}
					}
				}
			});
		} catch (error) {
			const errorObj = error as BusinessError;
			console.error('[SSEClient] Failed to create rcp session:', `code=${errorObj.code}`, `message=${errorObj.message}`);
			this.listener.onError(`Session creation failed: ${errorObj.message}`);
			return;
		}

		const sseStream = new SSEWriteStream({
			onMessage: this.listener.onMessage,
			onError: this.listener.onError,
			onDone: () => {
				this.close();
				if (this.listener.onDone) {
					this.listener.onDone();
				}
			},
			onPlan: this.listener.onPlan,
			onIntent: this.listener.onIntent,
			onAppInfo: this.listener.onAppInfo,
			onLogoGenerated: this.listener.onLogoGenerated,
			onAppGenerated: this.listener.onAppGenerated
		});

		try {
			const content: rcp.RequestContent = body ? body : {};

			await this.session.post(url, content, {
				kind: 'stream',
				stream: sseStream
			});
		} catch (err) {
			const errorObj = err as BusinessError;

			if (errorObj.code === 1007900992) {
				console.info('[SSEClient] Request canceled by client (Normal termination)');
				return;
			}

			console.error('[SSEClient] Stream request exception:', JSON.stringify(errorObj), errorObj.message);
			this.listener.onError(errorObj.message || 'Network request failed');
		} finally {
			this.close();
		}
	}

	public close(): void {
		if (this.session) {
			try {
				this.session.close();
			} catch (e) {
			}
			this.session = null;
		}
	}
}
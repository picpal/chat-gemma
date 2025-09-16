import { Client, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { ChatMessage, ChatMessageRequest } from '../types/chat'

class WebSocketService {
  private client: Client | null = null
  private connected = false
  private connecting = false
  private connectPromise: Promise<void> | null = null
  private messageHandlers = new Map<string, (message: ChatMessage) => void>()
  private subscriptions = new Map<string, StompSubscription>()

  connect(): Promise<void> {
    if (this.connected) return Promise.resolve()
    if (this.connecting && this.connectPromise) return this.connectPromise

    this.connecting = true
    this.connectPromise = new Promise((resolve, reject) => {
      try {
        // If a previous client exists, ensure it is deactivated first
        if (this.client) {
          try { this.client.deactivate() } catch { /* noop */ }
        }

        const socket = new SockJS('http://localhost:8080/ws')
        const client = new Client({
          webSocketFactory: () => socket as any,
          reconnectDelay: 5000, // 5초 후 재연결 시도
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          debug: (str) => {
            console.log('[WebSocket]', str)
          },
          onConnect: (frame) => {
            console.log('✅ WebSocket connected successfully')
            console.log('🔗 [WebSocket] Connection frame:', {
              headers: frame.headers,
              sessionId: frame.headers?.['session'] || frame.headers?.['user-name'],
              command: frame.command
            })
            this.client = client
            this.connected = true
            this.connecting = false
            resolve()
          },
          onDisconnect: () => {
            console.log('❌ WebSocket disconnected')
            this.connected = false
            this.connecting = false
          },
          onStompError: (frame) => {
            console.error('WebSocket STOMP error:', frame)
            this.connecting = false
            this.connected = false
            reject(new Error('WebSocket connection failed'))
          }
        })

        this.client = client
        client.activate()
      } catch (e) {
        this.connecting = false
        this.connected = false
        reject(e)
      }
    })

    return this.connectPromise
  }

  disconnect(): void {
    // Unsubscribe all before deactivation
    for (const sub of this.subscriptions.values()) {
      try { sub.unsubscribe() } catch { /* noop */ }
    }
    this.subscriptions.clear()
    this.messageHandlers.clear()
    if (this.client) {
      try { this.client.deactivate() } catch { /* noop */ }
    }
    this.client = null
    this.connected = false
    this.connecting = false
    this.connectPromise = null
  }

  subscribeToChat(chatId: number, onMessage: (message: ChatMessage) => void): () => void {
    if (!this.client || !this.connected) {
      throw new Error('WebSocket not connected')
    }

    // 채팅방 브로드캐스트 방식 사용
    const destination = `/topic/chat/${chatId}`

    console.log('🎯 [WebSocket] Subscribing to destination:', destination)

    this.messageHandlers.set(chatId.toString(), onMessage)

    // 기존 구독 제거
    const existing = this.subscriptions.get(`chat-${chatId}`)
    if (existing) {
      try { existing.unsubscribe() } catch { /* noop */ }
      this.subscriptions.delete(`chat-${chatId}`)
    }

    const messageHandler = (message: any) => {
      try {
        console.log(`🔥 [WebSocket] Raw STOMP message received:`, {
          destination: message.headers?.destination,
          messageId: message.headers?.['message-id'],
          subscription: message.headers?.subscription,
          body: message.body
        })

        const chatMessage: ChatMessage = JSON.parse(message.body)

        console.log(`🔥 [WebSocket] Parsed message:`, {
          id: chatMessage.id,
          role: chatMessage.role,
          content: chatMessage.content,
          isStreaming: chatMessage.isStreaming,
          isError: chatMessage.isError
        })
        onMessage(chatMessage)
      } catch (error) {
        console.error(`❌ [WebSocket] Failed to parse message:`, error, message.body)
      }
    }

    // 채팅방 구독
    const subscription = this.client.subscribe(destination, messageHandler)

    this.subscriptions.set(`chat-${chatId}`, subscription)

    return () => {
      try { subscription.unsubscribe() } catch { /* noop */ }
      this.subscriptions.delete(`chat-${chatId}`)
      this.messageHandlers.delete(chatId.toString())
    }
  }

  sendMessage(request: ChatMessageRequest): void {
    if (!this.client || !this.connected) {
      throw new Error('WebSocket not connected')
    }

    console.log('🚀 [WebSocket] Sending message:', request)
    this.client.publish({
      destination: '/app/chat.sendMessage',
      body: JSON.stringify(request)
    })
    console.log('📤 [WebSocket] Message published successfully')
  }

  joinChat(chatId: number): void {
    if (!this.client || !this.connected) {
      throw new Error('WebSocket not connected')
    }

    this.client.publish({
      destination: '/app/chat.join',
      body: chatId.toString()
    })
  }

  isConnected(): boolean {
    return this.connected
  }
}

export const webSocketService = new WebSocketService()
export type { ChatMessage, ChatMessageRequest } from '../types/chat'

import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { ChatMessage, ChatMessageRequest } from '../types/chat'

class WebSocketService {
  private client: Client | null = null
  private connected = false
  private messageHandlers = new Map<string, (message: ChatMessage) => void>()

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.connected) {
        resolve()
        return
      }

      const socket = new SockJS('http://localhost:8080/ws')
      this.client = new Client({
        webSocketFactory: () => socket,
        debug: (str) => {
          console.log('[WebSocket]', str)
        },
        onConnect: () => {
          console.log('WebSocket connected')
          this.connected = true
          resolve()
        },
        onDisconnect: () => {
          console.log('WebSocket disconnected')
          this.connected = false
        },
        onStompError: (frame) => {
          console.error('WebSocket STOMP error:', frame)
          reject(new Error('WebSocket connection failed'))
        }
      })

      this.client.activate()
    })
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate()
      this.connected = false
    }
  }

  subscribeToChat(chatId: string, onMessage: (message: ChatMessage) => void): () => void {
    if (!this.client || !this.connected) {
      throw new Error('WebSocket not connected')
    }

    const destination = `/user/queue/chat/${chatId}`
    this.messageHandlers.set(chatId, onMessage)

    const subscription = this.client.subscribe(destination, (message) => {
      try {
        const chatMessage: ChatMessage = JSON.parse(message.body)
        onMessage(chatMessage)
      } catch (error) {
        console.error('Failed to parse message:', error)
      }
    })

    return () => {
      subscription.unsubscribe()
      this.messageHandlers.delete(chatId)
    }
  }

  sendMessage(request: ChatMessageRequest): void {
    if (!this.client || !this.connected) {
      throw new Error('WebSocket not connected')
    }

    this.client.publish({
      destination: '/app/chat.sendMessage',
      body: JSON.stringify(request)
    })
  }

  joinChat(chatId: string): void {
    if (!this.client || !this.connected) {
      throw new Error('WebSocket not connected')
    }

    this.client.publish({
      destination: '/app/chat.join',
      body: chatId
    })
  }

  isConnected(): boolean {
    return this.connected
  }
}

export const webSocketService = new WebSocketService()
export type { ChatMessage, ChatMessageRequest } from '../types/chat'
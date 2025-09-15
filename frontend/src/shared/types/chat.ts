export type ChatMessage = {
  id: string
  chatId: string
  content: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  timestamp: string
  imageUrl?: string
  isStreaming?: boolean
  isError?: boolean
}

export type ChatMessageRequest = {
  chatId: string
  content: string
  imageUrl?: string
}

export type Chat = {
  id: string
  title: string
  updatedAt: string
  messageCount: number
}

export type Message = {
  id: string
  content: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  timestamp: string
  imageUrl?: string
  isStreaming?: boolean
  isError?: boolean
}

export type ChatData = {
  id: number
  userId: number
  title: string
  createdAt: string
  updatedAt: string
  deleted: boolean
}

export type MessageData = {
  id: number
  chatId: number
  role: 'USER' | 'ASSISTANT'
  content: string
  imageUrl?: string
  createdAt: string
}

// Explicit exports for better module resolution
export { ChatData, MessageData };
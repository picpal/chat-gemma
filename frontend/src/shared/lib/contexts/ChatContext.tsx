import { createContext, useContext, useState, ReactNode, useEffect } from 'react'
import { webSocketService } from '@/shared/api/websocket'
import { chatApi } from '@/shared/api/chat'
import type { ChatMessage, ChatMessageRequest, Chat, Message, ChatData } from '@/shared/types/chat'

interface ChatContextType {
  chats: Chat[]
  currentChatId: number | null
  isLoading: boolean
  searchQuery: string
  sidebarCollapsed: boolean
  messages: Record<number, Message[]>
  isConnected: boolean

  // Actions
  setCurrentChatId: (id: number | null) => void
  createNewChat: () => Promise<void>
  deleteChat: (id: number) => Promise<void>
  updateChatTitle: (id: number, title: string) => Promise<void>
  setSearchQuery: (query: string) => void
  setSidebarCollapsed: (collapsed: boolean) => void
  setChats: (chats: Chat[]) => void
  sendMessage: (content: string, imageUrl?: string) => void
  getCurrentMessages: () => Message[]
  loadChats: () => Promise<void>
}

const ChatContext = createContext<ChatContextType | null>(null)

// 타입 변환 함수
const convertChatDataToChat = (chatData: ChatData): Chat => ({
  id: chatData.id.toString(),
  title: chatData.title,
  updatedAt: chatData.updatedAt,
  messageCount: 0 // TODO: 메시지 개수 계산
})

export function ChatProvider({ children }: { children: ReactNode }) {
  const [chats, setChats] = useState<Chat[]>([])
  const [currentChatId, setCurrentChatId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [messages, setMessages] = useState<Record<number, Message[]>>({})
  const [isConnected, setIsConnected] = useState(false)

  // WebSocket 연결 초기화
  useEffect(() => {
    const initWebSocket = async () => {
      try {
        await webSocketService.connect()
        setIsConnected(true)
        console.log('WebSocket connected successfully')
      } catch (error) {
        console.error('Failed to connect to WebSocket:', error)
        setIsConnected(false)
      }
    }

    initWebSocket()

    return () => {
      webSocketService.disconnect()
      setIsConnected(false)
    }
  }, [])

  // 현재 채팅 변경 시 구독 설정
  useEffect(() => {
    if (!currentChatId || !isConnected) return

    const handleMessage = (chatMessage: ChatMessage) => {
      const message: Message = {
        id: chatMessage.id,
        content: chatMessage.content,
        role: chatMessage.role,
        timestamp: chatMessage.timestamp,
        imageUrl: chatMessage.imageUrl,
        isStreaming: chatMessage.isStreaming,
        isError: chatMessage.isError
      }

      setMessages(prev => {
        const currentMessages = prev[currentChatId] || []

        // 스트리밍 메시지인 경우 기존 메시지에 추가하거나 업데이트
        if (message.isStreaming && message.role === 'ASSISTANT') {
          const lastMessage = currentMessages[currentMessages.length - 1]
          if (lastMessage && lastMessage.role === 'ASSISTANT' && lastMessage.isStreaming) {
            // 기존 스트리밍 메시지에 내용 추가
            const updatedMessages = [...currentMessages]
            updatedMessages[updatedMessages.length - 1] = {
              ...lastMessage,
              content: lastMessage.content + message.content
            }
            return {
              ...prev,
              [currentChatId]: updatedMessages
            }
          } else {
            // 새로운 스트리밍 메시지 시작
            return {
              ...prev,
              [currentChatId]: [...currentMessages, message]
            }
          }
        } else {
          // 일반 메시지 추가
          return {
            ...prev,
            [currentChatId]: [...currentMessages, message]
          }
        }
      })
    }

    const unsubscribe = webSocketService.subscribeToChat(currentChatId, handleMessage)
    webSocketService.joinChat(currentChatId)

    return unsubscribe
  }, [currentChatId, isConnected])

  const createNewChat = async () => {
    try {
      setIsLoading(true)
      const newChatData = await chatApi.createChat()
      const newChat = convertChatDataToChat(newChatData)
      setChats(prev => [newChat, ...prev])
      setCurrentChatId(newChatData.id)
    } catch (error) {
      console.error('Failed to create new chat:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const deleteChat = async (id: number) => {
    try {
      setIsLoading(true)
      await chatApi.deleteChat(id)
      setChats(prev => prev.filter(chat => parseInt(chat.id) !== id))
      setMessages(prev => {
        const newMessages = { ...prev }
        delete newMessages[id]
        return newMessages
      })
      if (currentChatId === id) {
        const remainingChats = chats.filter(chat => parseInt(chat.id) !== id)
        setCurrentChatId(remainingChats.length > 0 ? parseInt(remainingChats[0].id) : null)
      }
    } catch (error) {
      console.error('Failed to delete chat:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const updateChatTitle = async (id: number, title: string) => {
    try {
      setIsLoading(true)
      const updatedChatData = await chatApi.updateChatTitle(id, title)
      const updatedChat = convertChatDataToChat(updatedChatData)
      setChats(prev => prev.map(chat =>
        parseInt(chat.id) === id ? updatedChat : chat
      ))
    } catch (error) {
      console.error('Failed to update chat title:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const sendMessage = (content: string, imageUrl?: string) => {
    if (!currentChatId || !isConnected) {
      console.error('Cannot send message: no active chat or WebSocket not connected')
      return
    }

    const request: ChatMessageRequest = {
      chatId: currentChatId,
      content,
      imageUrl
    }

    try {
      webSocketService.sendMessage(request)
    } catch (error) {
      console.error('Failed to send message:', error)
    }
  }

  const loadChats = async () => {
    try {
      setIsLoading(true)
      const chatDataList = await chatApi.getUserChats()
      const chatList = chatDataList.map(convertChatDataToChat)
      setChats(chatList)
      if (chatList.length > 0 && !currentChatId) {
        setCurrentChatId(parseInt(chatList[0].id))
      }
    } catch (error) {
      console.error('Failed to load chats:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const getCurrentMessages = (): Message[] => {
    return currentChatId ? messages[currentChatId] || [] : []
  }

  // 초기 데이터 로드
  useEffect(() => {
    loadChats()
  }, [])

  const value: ChatContextType = {
    chats,
    currentChatId,
    isLoading,
    searchQuery,
    sidebarCollapsed,
    messages,
    isConnected,
    setCurrentChatId,
    createNewChat,
    deleteChat,
    updateChatTitle,
    setSearchQuery,
    setSidebarCollapsed,
    setChats,
    sendMessage,
    getCurrentMessages,
    loadChats
  }

  return (
    <ChatContext.Provider value={value}>
      {children}
    </ChatContext.Provider>
  )
}

export function useChatContext() {
  const context = useContext(ChatContext)
  if (!context) {
    throw new Error('useChatContext must be used within a ChatProvider')
  }
  return context
}

export type { Message, Chat } from '../../types/chat'
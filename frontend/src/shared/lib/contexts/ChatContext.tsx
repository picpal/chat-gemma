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
  isAiResponding: boolean

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
  const [isSending, setIsSending] = useState(false)
  const [aiRespondingChats, setAiRespondingChats] = useState<Record<number, boolean>>({})

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

  // 현재 채팅 변경 시 메시지 히스토리 로드
  useEffect(() => {
    if (!currentChatId) return

    // 채팅방 변경 시 이전 채팅의 AI 응답 상태 정리
    console.log('🔄 [ChatContext] Chat changed, cleaning up AI states for chat:', currentChatId)

    const loadChatMessages = async () => {
      try {
        console.log('📚 [ChatContext] Loading chat messages for chatId:', currentChatId)
        const messageDataList = await chatApi.getChatMessages(currentChatId)

        // MessageData를 Message 타입으로 변환
        const loadedMessages: Message[] = messageDataList.map(msgData => ({
          id: msgData.id.toString(),
          content: msgData.content,
          role: msgData.role,
          timestamp: msgData.createdAt,
          imageUrl: msgData.imageUrl,
          isStreaming: false,
          isError: false
        }))

        setMessages(prev => ({
          ...prev,
          [currentChatId]: loadedMessages
        }))

        // 로드된 메시지 중에 완료되지 않은 AI 응답이 있는지 확인
        const hasIncompleteAiResponse = loadedMessages.some(msg =>
          msg.role === 'ASSISTANT' && msg.isStreaming
        )

        // 완료되지 않은 AI 응답이 없으면 상태 정리
        if (!hasIncompleteAiResponse) {
          setAiRespondingChats(prev => ({ ...prev, [currentChatId]: false }))
        }

        console.log('✅ [ChatContext] Loaded', loadedMessages.length, 'messages for chat:', currentChatId)
      } catch (error) {
        console.error('❌ [ChatContext] Failed to load chat messages:', error)
      }
    }

    loadChatMessages()
  }, [currentChatId])

  // WebSocket 구독 설정
  useEffect(() => {
    if (!currentChatId || !isConnected) return

    const handleMessage = (chatMessage: ChatMessage) => {
      console.log('🔄 [ChatContext] Received WebSocket message:', {
        id: chatMessage.id,
        role: chatMessage.role,
        content: chatMessage.content,
        isStreaming: chatMessage.isStreaming,
        isError: chatMessage.isError,
        currentChatId
      })

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
        console.log('💬 [ChatContext] Current messages before processing:', currentMessages.length)

        // AI 응답 스트리밍 처리
        if (message.role === 'ASSISTANT') {
          console.log('🤖 [ChatContext] Processing ASSISTANT message:', {
            messageId: message.id,
            newContent: message.content,
            isStreaming: message.isStreaming,
            messagesCount: currentMessages.length
          })

          // 같은 ID의 기존 메시지를 찾아서 업데이트
          const existingMessageIndex = currentMessages.findIndex(msg =>
            msg.id === message.id && msg.role === 'ASSISTANT'
          )

          if (existingMessageIndex !== -1) {
            // 기존 메시지가 있으면 업데이트
            console.log('📝 [ChatContext] Updating existing AI message at index:', existingMessageIndex)
            const updatedMessages = [...currentMessages]
            const existingMessage = updatedMessages[existingMessageIndex]

            // 스트리밍 완료 신호인 경우 (빈 content + isStreaming=false)
            if (message.content === '' && !message.isStreaming) {
              console.log('🏁 [ChatContext] Streaming completion signal received')
              updatedMessages[existingMessageIndex] = {
                ...existingMessage,
                isStreaming: false // 스트리밍 완료 표시
              }
              setAiRespondingChats(prev => ({ ...prev, [currentChatId]: false })) // AI 응답 완료
            } else {
              // 일반적인 청크 누적
              updatedMessages[existingMessageIndex] = {
                ...existingMessage,
                content: existingMessage.content + message.content,
                isStreaming: message.isStreaming,
                timestamp: message.timestamp
              }

              // AI 응답 완료 처리 (여러 조건으로 체크)
              if (!message.isStreaming || message.content === '') {
                console.log('🏁 [ChatContext] AI response completed via isStreaming=false')
                setAiRespondingChats(prev => ({ ...prev, [currentChatId]: false }))
              }
            }

            console.log('✅ [ChatContext] Updated existing AI message:', updatedMessages[existingMessageIndex].content.substring(0, 50))

            return {
              ...prev,
              [currentChatId]: updatedMessages
            }
          } else {
            // 새로운 AI 응답 시작
            console.log('🆕 [ChatContext] Creating new AI message with ID:', message.id)
            const newMessage = { ...message, isStreaming: true }
            setAiRespondingChats(prev => ({ ...prev, [currentChatId]: false })) // AI 응답 시작 시 로딩 상태 즉시 해제

            return {
              ...prev,
              [currentChatId]: [...currentMessages, newMessage]
            }
          }
        } else {
          // 사용자 메시지는 중복 방지 (이미 sendMessage에서 추가함)
          console.log('👤 [ChatContext] Processing USER message')
          const isDuplicate = currentMessages.some(msg =>
            msg.role === 'USER' &&
            msg.content === message.content &&
            Math.abs(new Date(msg.timestamp).getTime() - new Date(message.timestamp).getTime()) < 5000
          )

          if (!isDuplicate) {
            console.log('✅ [ChatContext] Adding new USER message')
            return {
              ...prev,
              [currentChatId]: [...currentMessages, message]
            }
          } else {
            console.log('⚠️ [ChatContext] Duplicate USER message ignored')
          }
        }

        console.log('🔄 [ChatContext] No changes made to messages')
        return prev
      })
    }

    const unsubscribe = webSocketService.subscribeToChat(currentChatId, handleMessage)
    webSocketService.joinChat(currentChatId)

    return unsubscribe
  }, [currentChatId, isConnected, aiRespondingChats])

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

    if (isSending) {
      console.warn('🚫 [ChatContext] Message sending in progress, ignoring duplicate request')
      return
    }

    setIsSending(true)
    console.log('📤 [ChatContext] Starting message send:', { content, imageUrl, currentChatId })

    // 사용자 메시지를 즉시 UI에 표시
    const userMessage: Message = {
      id: Date.now().toString(), // 임시 ID 문자열화로 안정적인 key 보장
      content,
      role: 'USER',
      timestamp: new Date().toISOString(),
      imageUrl,
      isStreaming: false,
      isError: false
    }

    setMessages(prev => {
      const currentMessages = prev[currentChatId] || []
      return {
        ...prev,
        [currentChatId]: [...currentMessages, userMessage]
      }
    })

    const request: ChatMessageRequest = {
      chatId: currentChatId.toString(),
      content,
      imageUrl
    }

    try {
      webSocketService.sendMessage(request)
      setAiRespondingChats(prev => ({ ...prev, [currentChatId]: true })) // AI 응답 시작
      console.log('✅ [ChatContext] Message sent successfully via WebSocket')
    } catch (error) {
      console.error('❌ [ChatContext] Failed to send message:', error)
      setAiRespondingChats(prev => ({ ...prev, [currentChatId]: false })) // 오류 시 AI 응답 상태 해제
      // 오류 시 사용자 메시지에 오류 표시
      setMessages(prev => {
        const currentMessages = prev[currentChatId] || []
        const updatedMessages = [...currentMessages]
        const lastMessageIndex = updatedMessages.length - 1
        if (lastMessageIndex >= 0 && updatedMessages[lastMessageIndex].id === userMessage.id) {
          updatedMessages[lastMessageIndex] = {
            ...updatedMessages[lastMessageIndex],
            isError: true
          }
        }
        return {
          ...prev,
          [currentChatId]: updatedMessages
        }
      })
    } finally {
      // 전송 상태 해제 (성공/실패 관계없이)
      setIsSending(false)
      console.log('🔓 [ChatContext] Message sending state reset')
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
    const msgs = currentChatId ? messages[currentChatId] || [] : []
    console.log('📋 [ChatContext] getCurrentMessages called:', {
      currentChatId,
      messageCount: msgs.length,
      messages: msgs.map(m => ({ id: m.id, role: m.role, content: m.content.substring(0, 30) + '...', isStreaming: m.isStreaming }))
    })
    return msgs
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
    isAiResponding: currentChatId ? (aiRespondingChats[currentChatId] || false) : false,
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

import { ChatSidebar } from "@/widgets/chat-sidebar"
import { MessageThread } from "@/widgets/message-thread"
import { ChatInput } from "@/widgets/chat-input"
import { useChatContext } from "@/shared/lib/contexts/ChatContext"
import { useAuth } from "@/shared/lib/contexts/AuthContext"

export function ChatPage() {
  const { currentChatId, chats, getCurrentMessages, sendMessage, isConnected, isAiResponding } = useChatContext()
  const { user } = useAuth()

  const messages = getCurrentMessages()

  const handleSendMessage = async (content: string, image?: File) => {
    if (!content.trim() && !image) return
    if (!isConnected) {
      console.error('WebSocket is not connected')
      return
    }

    // Convert image to base64 for sending to backend
    let imageUrl: string | undefined
    if (image) {
      const reader = new FileReader()
      reader.onload = () => {
        imageUrl = reader.result as string
        sendMessage(content, imageUrl)
      }
      reader.readAsDataURL(image)
    } else {
      sendMessage(content)
    }
  }

  const handleImageClick = (imageUrl: string) => {
    // TODO: Open image in modal
    console.log('Opening image:', imageUrl)
  }

  return (
    <div className="flex h-screen bg-background">
      {/* Sidebar */}
      <div className="border-r border-gray-200">
        <ChatSidebar
          user={user}
        />
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <div className="h-16 border-b border-gray-200 flex items-center justify-between px-6 bg-gray-50">
          <div>
            <h1 className="text-lg font-semibold text-foreground/90">ChatGemma</h1>
            <p className="text-sm text-muted-foreground/70">
              Gemma 3n 기반 오프라인 AI 채팅
            </p>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-sm text-muted-foreground/60">
              현재 채팅: {chats.find(c => c.id === currentChatId)?.title || '채팅을 선택하세요'}
            </div>
            <div className={`text-sm ${isConnected ? 'text-green-600' : 'text-red-600'}`}>
              {isConnected ? '● 연결됨' : '● 연결 끊김'}
            </div>
          </div>
        </div>

        {/* Message Thread */}
        <div className="flex-1 overflow-hidden">
          <MessageThread
            messages={messages}
            isLoading={false}
            onImageClick={handleImageClick}
          />
        </div>

        {/* Chat Input */}
        <ChatInput
          onSendMessage={handleSendMessage}
          disabled={!isConnected}
          isAiResponding={isAiResponding}
          placeholder={
            !isConnected
              ? "WebSocket 연결 대기 중..."
              : isAiResponding
                ? "AI가 응답 중입니다..."
                : "메시지를 입력하세요..."
          }
        />
      </div>
    </div>
  )
}
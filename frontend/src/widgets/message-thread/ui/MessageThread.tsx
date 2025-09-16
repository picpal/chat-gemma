import { ScrollArea } from "@/shared/ui/scroll-area"
import { Avatar } from "@/shared/ui/avatar"
import { cn } from "@/shared/lib/utils"
import { Bot, User } from "lucide-react"
import type { Message } from "@/shared/types/chat"
import { useEffect, useRef } from "react"

interface MessageThreadProps {
  messages?: Message[]
  isLoading?: boolean
  onImageClick?: (imageUrl: string) => void
}

export function MessageThread({
  messages = [],
  isLoading = false,
  onImageClick,
}: MessageThreadProps) {
  const scrollAreaRef = useRef<HTMLDivElement>(null)

  console.log('🎨 [MessageThread] Render with:', {
    messageCount: messages.length,
    messages: messages.map(m => ({ id: m.id, role: m.role, content: m.content.substring(0, 30) + '...', isStreaming: m.isStreaming }))
  })

  // 자동 스크롤 기능
  useEffect(() => {
    const scrollToBottom = () => {
      if (scrollAreaRef.current) {
        const viewport = scrollAreaRef.current.querySelector('[data-radix-scroll-area-viewport]')
        if (viewport) {
          // 부드러운 스크롤
          viewport.scrollTo({
            top: viewport.scrollHeight,
            behavior: 'smooth'
          })
        }
      }
    }

    // 약간의 지연을 주어 DOM 업데이트 완료 후 스크롤
    const timeoutId = setTimeout(scrollToBottom, 100)

    return () => clearTimeout(timeoutId)
  }, [messages])

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <ScrollArea className="flex-1 p-4" ref={scrollAreaRef}>
        <div className="max-w-4xl mx-auto space-y-4">
          {messages.map((message) => (
            <div
              key={message.id}
              className={cn(
                "flex gap-3",
                message.role === 'USER' ? 'justify-end' : 'justify-start'
              )}
            >
              {message.role === 'ASSISTANT' && (
                <Avatar className="h-8 w-8 mt-1 flex-shrink-0">
                  <div className="bg-accent text-accent-foreground rounded-full w-full h-full flex items-center justify-center">
                    <Bot className="h-4 w-4" />
                  </div>
                </Avatar>
              )}

              <div className="flex flex-col max-w-[70%] mt-0.5">
                <div
                  className={cn(
                    "rounded-lg px-4 py-2 text-sm",
                    message.role === 'USER'
                      ? "bg-blue-500 text-white"
                      : "bg-muted text-foreground"
                  )}
                >
                  {message.imageUrl && (
                    <div className="mb-2">
                      <img
                        src={message.imageUrl}
                        alt="Uploaded image"
                        className="max-w-full h-auto rounded cursor-pointer"
                        onClick={() => onImageClick?.(message.imageUrl!)}
                      />
                    </div>
                  )}
                  <div className="whitespace-pre-wrap">{message.content}</div>
                  {message.isStreaming && message.role === 'ASSISTANT' && (
                    <div className="inline-block w-2 h-4 ml-1 bg-current animate-pulse" />
                  )}
                </div>
              </div>

              {message.role === 'USER' && (
                <Avatar className="h-8 w-8 mt-1">
                  <div className="bg-secondary text-secondary-foreground rounded-full w-full h-full flex items-center justify-center">
                    <User className="h-4 w-4" />
                  </div>
                </Avatar>
              )}
            </div>
          ))}

          {isLoading && (
            <div className="flex gap-3 justify-start">
              <Avatar className="h-8 w-8 mt-1 flex-shrink-0">
                <div className="bg-accent text-accent-foreground rounded-full w-full h-full flex items-center justify-center">
                  <Bot className="h-4 w-4" />
                </div>
              </Avatar>

              <div className="bg-muted text-foreground rounded-lg px-4 py-2 text-sm mt-0.5">
                <div
                  className="bg-gradient-to-r from-muted-foreground/30 via-muted-foreground to-muted-foreground/30 bg-clip-text text-transparent"
                  style={{
                    backgroundSize: '200% 100%',
                    animation: 'shimmer 2s ease-in-out infinite'
                  }}
                >
                  답변을 위해 생각중...
                </div>
              </div>
            </div>
          )}
        </div>
      </ScrollArea>
    </div>
  )
}
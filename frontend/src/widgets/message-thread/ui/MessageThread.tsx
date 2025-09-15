import { ScrollArea } from "@/shared/ui/scroll-area"
import { Avatar } from "@/shared/ui/avatar"
import { cn } from "@/shared/lib/utils"
import { Bot, User } from "lucide-react"
import type { Message } from "@/shared/types/chat"

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
  return (
    <div className="flex flex-col h-full">
      <ScrollArea className="flex-1 p-4">
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
                <Avatar className="h-8 w-8 mt-1">
                  <div className="bg-accent text-accent-foreground rounded-full w-full h-full flex items-center justify-center">
                    <Bot className="h-4 w-4" />
                  </div>
                </Avatar>
              )}

              <div className="flex flex-col max-w-[70%]">
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
                </div>

                <div className={cn(
                  "text-xs text-muted-foreground mt-1",
                  message.role === 'USER' ? 'text-right' : 'text-left'
                )}>
                  {new Date(message.timestamp).toLocaleTimeString()}
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
              <Avatar className="h-8 w-8 mt-1">
                <div className="bg-accent text-accent-foreground rounded-full w-full h-full flex items-center justify-center">
                  <Bot className="h-4 w-4" />
                </div>
              </Avatar>

              <div className="bg-muted text-foreground rounded-lg px-4 py-2 text-sm">
                <div className="flex space-x-1">
                  <div className="animate-pulse">●</div>
                  <div className="animate-pulse" style={{ animationDelay: '0.2s' }}>●</div>
                  <div className="animate-pulse" style={{ animationDelay: '0.4s' }}>●</div>
                </div>
              </div>
            </div>
          )}
        </div>
      </ScrollArea>
    </div>
  )
}
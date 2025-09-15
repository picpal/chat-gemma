import { useState, useRef } from "react"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"
import { Paperclip, Send, X } from "lucide-react"
import { cn } from "@/shared/lib/utils"

interface ChatInputProps {
  onSendMessage?: (content: string, image?: File) => void
  disabled?: boolean
  placeholder?: string
}

export function ChatInput({
  onSendMessage,
  disabled = false,
  placeholder = "메시지를 입력하세요...",
}: ChatInputProps) {
  const [message, setMessage] = useState("")
  const [selectedImage, setSelectedImage] = useState<File | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleImageSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) {
      setSelectedImage(file)
      const reader = new FileReader()
      reader.onload = (e) => setImagePreview(e.target?.result as string)
      reader.readAsDataURL(file)
    }
  }

  const handleRemoveImage = () => {
    setSelectedImage(null)
    setImagePreview(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ""
    }
  }

  const handleSend = () => {
    if (!message.trim() && !selectedImage) return
    if (disabled) return

    onSendMessage?.(message, selectedImage || undefined)
    setMessage("")
    setSelectedImage(null)
    setImagePreview(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ""
    }

    // Reset textarea height
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
    }
  }

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      handleSend()
    }
  }

  const handleTextareaChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    setMessage(event.target.value)

    // Auto-resize textarea
    const textarea = event.target
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px'
  }

  return (
    <div className="p-4 bg-background">
      <div className="max-w-4xl mx-auto">
        {/* Image Preview */}
        {imagePreview && (
          <Card className="mb-3 p-3">
            <div className="flex items-start gap-3">
              <img
                src={imagePreview}
                alt="Preview"
                className="w-20 h-20 object-cover rounded"
              />
              <div className="flex-1">
                <div className="text-sm font-medium">{selectedImage?.name}</div>
                <div className="text-xs text-muted-foreground">
                  {selectedImage && (selectedImage.size / 1024).toFixed(1)} KB
                </div>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={handleRemoveImage}
                className="h-6 w-6"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          </Card>
        )}

        {/* Input Area */}
        <Card className="p-3">
          <div className="flex gap-3 items-end">
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handleImageSelect}
            />

            <Button
              variant="ghost"
              size="icon"
              onClick={() => fileInputRef.current?.click()}
              disabled={disabled}
              className="h-10 w-10 flex-shrink-0"
            >
              <Paperclip className="h-4 w-4" />
            </Button>

            <div className="flex-1">
              <textarea
                ref={textareaRef}
                value={message}
                onChange={handleTextareaChange}
                onKeyDown={handleKeyDown}
                placeholder={placeholder}
                disabled={disabled}
                className={cn(
                  "w-full resize-none border-0 bg-transparent p-0 text-sm focus:outline-none focus:ring-0",
                  "placeholder:text-muted-foreground",
                  "min-h-[24px] max-h-[120px]"
                )}
                rows={1}
              />
            </div>

            <Button
              onClick={handleSend}
              disabled={disabled || (!message.trim() && !selectedImage)}
              size="icon"
              className="h-10 w-10 flex-shrink-0"
            >
              <Send className="h-4 w-4" />
            </Button>
          </div>
        </Card>
      </div>
    </div>
  )
}
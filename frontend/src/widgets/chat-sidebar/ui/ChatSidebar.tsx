import { Button } from "@/shared/ui/button"
import { ScrollArea } from "@/shared/ui/scroll-area"
import { Input } from "@/shared/ui/input"
import { Avatar } from "@/shared/ui/avatar"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu"
import {
  Edit3,
  MessageSquare,
  Trash2,
  Search,
  PanelLeftClose,
  PanelLeft,
  User,
  LogOut,
  Settings,
  MoreHorizontal,
  Check,
  X
} from "lucide-react"
import { cn } from "@/shared/lib/utils"
import { useChatContext } from "@/shared/lib/contexts/ChatContext"
import { useAuth } from "@/shared/lib/contexts/AuthContext"
import { useState } from "react"

interface User {
  id: string
  email: string
  name?: string
}

interface ChatSidebarProps {
  user?: User
}

export function ChatSidebar({ user }: ChatSidebarProps) {
  const {
    chats,
    currentChatId,
    searchQuery,
    sidebarCollapsed,
    setCurrentChatId,
    createNewChat,
    deleteChat,
    updateChatTitle,
    setSearchQuery,
    setSidebarCollapsed
  } = useChatContext()
  const { logout } = useAuth()
  const [editingChatId, setEditingChatId] = useState<string | null>(null)
  const [editingTitle, setEditingTitle] = useState('')

  const filteredChats = chats.filter(chat =>
    chat.title.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const handleStartEdit = (chatId: string, currentTitle: string) => {
    setEditingChatId(chatId)
    setEditingTitle(currentTitle)
  }

  const handleSaveEdit = async () => {
    if (editingChatId && editingTitle.trim()) {
      await updateChatTitle(parseInt(editingChatId), editingTitle.trim())
      setEditingChatId(null)
      setEditingTitle('')
    }
  }

  const handleCancelEdit = () => {
    setEditingChatId(null)
    setEditingTitle('')
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSaveEdit()
    } else if (e.key === 'Escape') {
      handleCancelEdit()
    }
  }

  if (sidebarCollapsed) {
    return (
      <div className="flex flex-col h-full w-12 bg-white">
        {/* Collapsed Header */}
        <div className="p-2">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setSidebarCollapsed(false)}
            className="h-8 w-8"
          >
            <PanelLeft className="h-4 w-4" />
          </Button>
        </div>

        {/* New Chat Button */}
        <div className="p-2">
          <Button
            onClick={createNewChat}
            variant="ghost"
            size="icon"
            className="h-8 w-8"
          >
            <Edit3 className="h-4 w-4" />
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full w-64 bg-white">
      {/* Header */}
      <div className="flex items-center justify-between p-3 pb-2">
        <div className="flex items-center gap-2">
          <Button
            onClick={createNewChat}
            variant="ghost"
            size="icon"
            className="h-8 w-8 hover:bg-muted/60"
          >
            <Edit3 className="h-4 w-4" />
          </Button>
        </div>
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setSidebarCollapsed(true)}
          className="h-8 w-8 hover:bg-muted/60"
        >
          <PanelLeftClose className="h-4 w-4" />
        </Button>
      </div>

      {/* Search Bar */}
      <div className="px-3 pb-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="검색"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9 h-9 bg-gray-100 border-none focus-visible:ring-1 focus-visible:ring-blue-500"
          />
        </div>
      </div>

      {/* Chat List */}
      <ScrollArea className="flex-1 px-2">
        <div className="space-y-1">
          {filteredChats.map((chat) => (
            <div
              key={chat.id}
              className={cn(
                "group relative flex items-center gap-3 p-3 rounded-lg transition-all duration-200 hover:bg-muted/40",
                currentChatId === parseInt(chat.id) && "bg-muted/60"
              )}
            >
              <MessageSquare className="h-4 w-4 text-muted-foreground/70 flex-shrink-0" />
              <div className="flex-1 min-w-0" onClick={() => editingChatId !== chat.id && setCurrentChatId(parseInt(chat.id))}>
                {editingChatId === chat.id ? (
                  <div className="flex items-center gap-2">
                    <Input
                      value={editingTitle}
                      onChange={(e) => setEditingTitle(e.target.value)}
                      onKeyDown={handleKeyDown}
                      className="h-7 px-2 text-sm"
                      autoFocus
                    />
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-6 w-6 hover:bg-green-100 hover:text-green-600"
                      onClick={handleSaveEdit}
                    >
                      <Check className="h-3 w-3" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-6 w-6 hover:bg-red-100 hover:text-red-600"
                      onClick={handleCancelEdit}
                    >
                      <X className="h-3 w-3" />
                    </Button>
                  </div>
                ) : (
                  <div className="text-sm font-medium truncate text-foreground/90 cursor-pointer">
                    {chat.title}
                  </div>
                )}
              </div>
              {editingChatId !== chat.id && (
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="opacity-0 group-hover:opacity-100 transition-opacity h-6 w-6 hover:bg-muted/60"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <MoreHorizontal className="h-3 w-3" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-48">
                    <DropdownMenuItem
                      onClick={(e) => {
                        e.stopPropagation()
                        handleStartEdit(chat.id, chat.title)
                      }}
                    >
                      <Edit3 className="mr-2 h-4 w-4" />
                      <span>이름 변경</span>
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem
                      className="text-destructive focus:text-destructive"
                      onClick={(e) => {
                        e.stopPropagation()
                        deleteChat(parseInt(chat.id))
                      }}
                    >
                      <Trash2 className="mr-2 h-4 w-4" />
                      <span>삭제</span>
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              )}
            </div>
          ))}
        </div>
      </ScrollArea>

      {/* User Profile */}
      <div className="p-3 pt-2 border-t border-gray-200">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              className="w-full justify-start gap-3 h-auto p-2 hover:bg-muted/60"
            >
              <Avatar className="h-8 w-8">
                <div className="bg-primary text-primary-foreground rounded-full w-full h-full flex items-center justify-center">
                  <User className="h-4 w-4" />
                </div>
              </Avatar>
              <div className="flex flex-col items-start min-w-0 flex-1">
                <div className="text-sm font-medium text-foreground/90 truncate w-full">
                  {user?.email || 'picpal@kakao.com'}
                </div>
              </div>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="w-56">
            <DropdownMenuLabel className="font-normal">
              <div className="flex flex-col space-y-1">
                <p className="text-sm font-medium leading-none">
                  {user?.email || 'picpal@kakao.com'}
                </p>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem>
              <Settings className="mr-2 h-4 w-4" />
              <span>설정</span>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onClick={logout}
              className="text-destructive focus:text-destructive"
            >
              <LogOut className="mr-2 h-4 w-4" />
              <span>로그아웃</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  )
}
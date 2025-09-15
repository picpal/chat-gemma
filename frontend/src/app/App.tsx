import { useState } from 'react'
import { ChatPage } from '@/pages/chat'
import { LoginPage } from '@/pages/auth/ui/LoginPage'
import { SignupPage } from '@/pages/auth/ui/SignupPage'
import { AuthProvider, useAuth } from '@/shared/lib/contexts/AuthContext'
import { ChatProvider } from '@/shared/lib/contexts/ChatContext'
import './globals.css'

type AuthMode = 'login' | 'signup'

function AuthScreen() {
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const { login, signup } = useAuth()

  const handleSwitchMode = () => {
    setAuthMode(authMode === 'login' ? 'signup' : 'login')
  }

  if (authMode === 'login') {
    return (
      <LoginPage
        onLogin={login}
        onSwitchToSignup={handleSwitchMode}
      />
    )
  }

  return (
    <SignupPage
      onSignup={signup}
      onSwitchToLogin={handleSwitchMode}
    />
  )
}

function AppContent() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
          <p className="mt-2 text-muted-foreground">로딩 중...</p>
        </div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <AuthScreen />
  }

  return (
    <ChatProvider>
      <div className="h-screen">
        <ChatPage />
      </div>
    </ChatProvider>
  )
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  )
}

export default App
import type { ChatData, MessageData } from '@/shared/types/chat';

const BASE_URL = '/api/chats';

const handleResponse = async (response: Response) => {
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  return response.json();
};

export const chatApi = {
  createChat: async (): Promise<ChatData> => {
    const response = await fetch(BASE_URL, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    return handleResponse(response);
  },

  getUserChats: async (): Promise<ChatData[]> => {
    const response = await fetch(BASE_URL, {
      method: 'GET',
      credentials: 'include',
    });
    return handleResponse(response);
  },

  getChat: async (chatId: number): Promise<ChatData> => {
    const response = await fetch(`${BASE_URL}/${chatId}`, {
      method: 'GET',
      credentials: 'include',
    });
    return handleResponse(response);
  },

  getChatMessages: async (chatId: number): Promise<MessageData[]> => {
    const response = await fetch(`${BASE_URL}/${chatId}/messages`, {
      method: 'GET',
      credentials: 'include',
    });
    return handleResponse(response);
  },

  sendMessage: async (chatId: number, message: string, imageUrl?: string): Promise<MessageData> => {
    const response = await fetch(`${BASE_URL}/${chatId}/messages`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        message,
        imageUrl,
      }),
    });
    return handleResponse(response);
  },

  updateChatTitle: async (chatId: number, title: string): Promise<ChatData> => {
    const response = await fetch(`${BASE_URL}/${chatId}/title`, {
      method: 'PUT',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ title }),
    });
    return handleResponse(response);
  },

  deleteChat: async (chatId: number): Promise<{ message: string }> => {
    const response = await fetch(`${BASE_URL}/${chatId}`, {
      method: 'DELETE',
      credentials: 'include',
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const text = await response.text();
    return { message: text };
  },
};
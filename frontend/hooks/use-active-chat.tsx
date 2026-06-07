"use client";

import { usePathname } from "next/navigation";
import {
  createContext,
  type Dispatch,
  type ReactNode,
  type SetStateAction,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import useSWR from "swr";
import { DEFAULT_CHAT_MODEL } from "@/lib/ai/models";
import type { ChatMessage } from "@/lib/types";
import { fetcher, generateUUID } from "@/lib/utils";
import { useWebSocketChat } from "./use-websocket-chat";

type ActiveChatContextValue = {
  chatId: string;
  messages: ChatMessage[];
  setMessages: ReturnType<typeof useWebSocketChat>["setMessages"];
  sendMessage: ReturnType<typeof useWebSocketChat>["sendMessage"];
  status: ReturnType<typeof useWebSocketChat>["status"];
  stop: ReturnType<typeof useWebSocketChat>["stop"];
  regenerate: ReturnType<typeof useWebSocketChat>["regenerate"];
  addToolApprovalResponse: (response: unknown) => void;
  input: string;
  setInput: Dispatch<SetStateAction<string>>;
  visibilityType: "private";
  isReadonly: boolean;
  isLoading: boolean;
  votes: never[] | undefined;
  currentModelId: string;
  setCurrentModelId: (id: string) => void;
  showCreditCardAlert: boolean;
  setShowCreditCardAlert: Dispatch<SetStateAction<boolean>>;
};

const ActiveChatContext = createContext<ActiveChatContextValue | null>(null);

function extractChatId(pathname: string): string | null {
  const match = pathname.match(/\/chat\/([^/]+)/);
  return match ? match[1] : null;
}

export function ActiveChatProvider({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const chatIdFromUrl = extractChatId(pathname);
  const isNewChat = !chatIdFromUrl;
  const newChatIdRef = useRef(generateUUID());
  const prevPathnameRef = useRef(pathname);

  if (isNewChat && prevPathnameRef.current !== pathname) {
    newChatIdRef.current = generateUUID();
  }
  prevPathnameRef.current = pathname;
  const chatId = chatIdFromUrl ?? newChatIdRef.current;

  const [currentModelId, setCurrentModelId] = useState(DEFAULT_CHAT_MODEL);
  const [input, setInput] = useState("");
  const [showCreditCardAlert, setShowCreditCardAlert] = useState(false);

  // Load messages when viewing a past chat
  const { data: chatData } = useSWR(
    isNewChat ? null : `${process.env.NEXT_PUBLIC_BASE_PATH ?? ""}/api/messages?chatId=${chatId}`,
    fetcher
  );
  const initialMessages: ChatMessage[] = chatData?.messages ?? [];

  // Use WebSocket instead of AI SDK useChat
  const {
    messages,
    setMessages,
    sendMessage,
    status,
    stop,
    regenerate,
    isLoading,
  } = useWebSocketChat(initialMessages);

  const hasAppendedQueryRef = useRef(false);
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const query = params.get("query");
    if (query && !hasAppendedQueryRef.current) {
      hasAppendedQueryRef.current = true;
      window.history.replaceState(
        {},
        "",
        `${process.env.NEXT_PUBLIC_BASE_PATH ?? ""}/chat/${chatId}`
      );
      sendMessage({
        role: "user" as const,
        parts: [{ type: "text", text: query }],
      });
    }
  }, [sendMessage, chatId]);

  const value = useMemo<ActiveChatContextValue>(
    () => ({
      chatId,
      messages,
      setMessages,
      sendMessage,
      status,
      stop,
      regenerate,
      addToolApprovalResponse: () => {},
      input,
      setInput,
      visibilityType: "private" as const,
      isReadonly: false,
      isLoading,
      votes: undefined,
      currentModelId,
      setCurrentModelId,
      showCreditCardAlert,
      setShowCreditCardAlert,
    }),
    [
      chatId, messages, setMessages, sendMessage, status, stop, regenerate,
      input, isLoading, currentModelId, showCreditCardAlert,
    ]
  );

  return (
    <ActiveChatContext.Provider value={value}>
      {children}
    </ActiveChatContext.Provider>
  );
}

export function useActiveChat() {
  const context = useContext(ActiveChatContext);
  if (!context) {
    throw new Error("useActiveChat must be used within ActiveChatProvider");
  }
  return context;
}

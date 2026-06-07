"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { flushSync } from "react-dom";

type AgentStep = {
  type: string;
  content?: string;
  tool?: string;
  params?: string;
};

type ChatMessage = {
  id: string;
  role: "user" | "assistant" | "system" | "data";
  parts: Array<{ type: string; text?: string; [key: string]: unknown }>;
  createdAt?: Date;
};

type UseWebSocketChatOptions = {
  onFinish?: (message: ChatMessage) => void;
  onError?: (error: Error) => void;
};

type Status = "ready" | "submitted" | "streaming" | "error";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080/ws/chat";

export function useWebSocketChat(initialMessages: ChatMessage[] = [], options: UseWebSocketChatOptions = {}) {
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages);
  const [status, setStatus] = useState<Status>("ready");
  const [input, setInput] = useState("");
  const wsRef = useRef<WebSocket | null>(null);

  // Sync messages when navigating to a different past chat
  const initRef = useRef(initialMessages);
  useEffect(() => {
    if (initialMessages !== initRef.current && initialMessages.length > 0) {
      initRef.current = initialMessages;
      setMessages(initialMessages);
    }
  }, [initialMessages]);
  const pendingRef = useRef<string | null>(null);
  const streamMsgIdRef = useRef<string | null>(null);
  const msgCounterRef = useRef(0);

  const nextId = () => `msg-${++msgCounterRef.current}`;

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    const socket = new WebSocket(WS_URL);
    wsRef.current = socket;

    socket.onopen = () => {
      if (pendingRef.current) {
        socket.send(pendingRef.current);
        pendingRef.current = null;
      }
    };

    socket.onmessage = (event) => {
      const step: AgentStep = JSON.parse(event.data);
      handleStep(step);
    };

    socket.onclose = () => {
      setStatus("ready");
    };

    socket.onerror = () => {
      setStatus("error");
      options.onError?.(new Error("WebSocket connection error"));
    };
  }, []);

  useEffect(() => {
    connect();
    return () => wsRef.current?.close();
  }, [connect]);

  const sendMessage = useCallback(
    (message: { role: "user"; parts: Array<{ type: string; text?: string }> }) => {
      const text = message.parts
        .filter((p) => p.type === "text")
        .map((p) => p.text)
        .join("");

      if (!text) return;

      // Add user message
      const userMsg: ChatMessage = {
        id: nextId(),
        role: "user",
        parts: [{ type: "text", text }],
        createdAt: new Date(),
      };

      setMessages((prev) => [...prev, userMsg]);
      streamMsgIdRef.current = null;
      setStatus("submitted");

      const payload = JSON.stringify({ content: text });

      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send(payload);
      } else {
        pendingRef.current = payload;
        connect();
      }
    },
    [connect]
  );

  const handleStep = useCallback(
    (step: AgentStep) => {
      switch (step.type) {
        case "THOUGHT":
        case "ACTION":
        case "OBSERVATION":
          // Don't show intermediate tool calls — just keep loading
          setStatus("streaming");
          break;

        case "TOKEN":
          // Force synchronous render for typewriter streaming effect
          flushSync(() => {
            setMessages((prev) => {
              if (streamMsgIdRef.current) {
                return prev.map(m => {
                  if (m.id === streamMsgIdRef.current) {
                    return {
                      ...m,
                      parts: m.parts.map(p =>
                        p.type === "text" ? { ...p, text: (p.text || "") + (step.content || "") } : p
                      ),
                    };
                  }
                  return m;
                });
              }
              const id = nextId();
              streamMsgIdRef.current = id;
              const streamMsg: ChatMessage = {
                id,
                role: "assistant",
                parts: [{ type: "text", text: step.content || "" }],
              };
              return [...prev, streamMsg];
            });
          });
          setStatus("streaming");
          break;

        case "FINAL_PLAN": {
          // Replace streaming message with final plan
          const planId = streamMsgIdRef.current;
          streamMsgIdRef.current = null;
          setMessages((prev) => {
            if (planId && prev.some(m => m.id === planId)) {
              return prev.map(m => m.id === planId
                ? { ...m, id: m.id, role: "assistant" as const, parts: [{ type: "text" as const, text: step.content || "" }], createdAt: new Date() }
                : m);
            }
            const planMsg: ChatMessage = {
              id: nextId(),
              role: "assistant",
              parts: [{ type: "text", text: step.content || "" }],
              createdAt: new Date(),
            };
            return [...prev, planMsg];
          });
          setStatus("ready");
          options.onFinish?.(planMsg);
          window.dispatchEvent(new Event("chat-updated"));
          break;
        }

        case "CONFIRMING":
          setStatus("streaming");
          break;

        case "DONE":
          setMessages((prev) => {
            const done: ChatMessage = {
              id: nextId(),
              role: "assistant",
              parts: [{ type: "text", text: `🎉 ${step.content || "全部搞定!"}` }],
            };
            return [...prev, done];
          });
          setStatus("ready");
          window.dispatchEvent(new Event("chat-updated"));
          break;

        case "ERROR":
          setMessages((prev) => {
            const err: ChatMessage = {
              id: nextId(),
              role: "assistant",
              parts: [{ type: "text", text: `❌ ${step.content || "未知错误"}` }],
            };
            return [...prev, err];
          });
          setStatus("error");
          options.onError?.(new Error(step.content || "Unknown error"));
          break;
      }
    },
    [options]
  );

  const stop = useCallback(() => {
    wsRef.current?.close();
    setStatus("ready");
  }, []);

  const regenerate = useCallback(() => {
    // Find last user message and resend
    const lastUserMsg = [...messages]
      .reverse()
      .find((m) => m.role === "user");
    if (lastUserMsg) {
      const text = lastUserMsg.parts
        .filter((p) => p.type === "text")
        .map((p) => p.text)
        .join("");
      // Remove messages after the last user message
      const idx = messages.indexOf(lastUserMsg);
      setMessages(messages.slice(0, idx + 1));
      // Resend
      if (text && wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ content: text }));
        setStatus("submitted");
      }
    }
  }, [messages]);

  const isLoading = status === "submitted" || status === "streaming";

  return {
    messages,
    setMessages,
    sendMessage,
    status,
    stop,
    regenerate,
    input,
    setInput,
    isLoading,
  };
}

import { useState, useEffect, useRef, useCallback } from 'react';

export default function useChat() {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const ws = useRef(null);
  const pendingMsg = useRef(null);

  const connect = useCallback(() => {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const socket = new WebSocket(`${protocol}//${location.host}/ws/chat`);
    ws.current = socket;

    socket.onmessage = (event) => {
      const step = JSON.parse(event.data);
      setMessages((prev) => {
        const last = prev[prev.length - 1];
        // If the last message is a "round" group and step continues it, append
        if (last?.type === 'round' && ['THOUGHT', 'ACTION', 'OBSERVATION'].includes(step.type)) {
          const updated = { ...last, children: [...last.children, toMsg(step)] };
          return [...prev.slice(0, -1), updated];
        }
        // Start new round for THOUGHT
        if (step.type === 'THOUGHT') {
          return [...prev, { id: Date.now(), type: 'round', children: [toMsg(step)] }];
        }
        // Standalone messages
        return [...prev, toMsg(step)];
      });
      if (step.type === 'FINAL_PLAN' || step.type === 'ERROR' || step.type === 'DONE') {
        setLoading(false);
      }
    };

    socket.onclose = () => setLoading(false);
    socket.onerror = () => setLoading(false);
  }, []);

  useEffect(() => {
    connect();
    return () => ws.current?.close();
  }, [connect]);

  const send = useCallback((text) => {
    if (!ws.current || ws.current.readyState !== WebSocket.OPEN) {
      connect(); return;
    }
    setMessages((prev) => [...prev, { id: Date.now(), role: 'user', content: text }]);
    ws.current.send(JSON.stringify({ content: text }));
    setLoading(true);
  }, [connect]);

  const confirmPlan = useCallback(() => {
    if (!ws.current || ws.current.readyState !== WebSocket.OPEN) return;
    ws.current.send(JSON.stringify({ type: 'confirm', content: '确认执行此方案' }));
    setLoading(true);
  }, []);

  return { messages, loading, send, confirmPlan };
}

function toMsg(step) {
  switch (step.type) {
    case 'THOUGHT': return { type: 'thought', content: step.content };
    case 'ACTION':  return { type: 'action', content: step.content || '', tool: step.tool, params: step.params };
    case 'OBSERVATION': return { type: 'observation', content: step.content };
    case 'FINAL_PLAN':  return { id: Date.now(), type: 'plan', content: step.content, role: 'assistant' };
    case 'CONFIRMING':  return { id: Date.now(), type: 'confirming', content: step.content };
    case 'DONE':     return { id: Date.now(), type: 'done', content: step.content };
    case 'ERROR':    return { id: Date.now(), type: 'error', content: step.content };
    default:         return { id: Date.now(), type: 'text', content: step.content || '', role: 'assistant' };
  }
}

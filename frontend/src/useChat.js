import { useState, useEffect, useRef, useCallback } from 'react';

export default function useChat() {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const ws = useRef(null);
  const pending = useRef(null);

  useEffect(() => {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const socket = new WebSocket(`${protocol}//${location.host}/ws/chat`);
    ws.current = socket;
    socket.onopen = () => {
      setLoading(false);
      if (pending.current) {
        socket.send(pending.current);
        pending.current = null;
      }
    };
    socket.onmessage = (event) => {
      const step = JSON.parse(event.data);
      setMessages(prev => handleStep(prev, step));
      if (step.type === 'FINAL_PLAN' || step.type === 'ERROR' || step.type === 'DONE') {
        setLoading(false);
      }
    };
    socket.onclose = () => setLoading(false);
    socket.onerror = () => setLoading(false);
    return () => socket.close();
  }, []);

  const send = useCallback((text) => {
    setMessages(prev => [...prev, { id: Date.now(), role: 'user', content: text }]);
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify({ content: text }));
      setLoading(true);
    } else {
      pending.current = JSON.stringify({ content: text });
      setLoading(true);
    }
  }, []);

  return { messages, loading, send };
}

function handleStep(prev, step) {
  const last = prev[prev.length - 1];
  if (step.type === 'THOUGHT') {
    return [...prev, { id: Date.now(), type: 'round', children: [toChild(step)] }];
  }
  if (['ACTION', 'OBSERVATION'].includes(step.type) && last?.type === 'round') {
    const updated = { ...last, children: [...last.children, toChild(step)] };
    return [...prev.slice(0, -1), updated];
  }
  return [...prev, toMsg(step)];
}

function toChild(step) {
  if (step.type === 'THOUGHT') return { type: 'thought', content: step.content };
  if (step.type === 'ACTION') return { type: 'action', tool: step.tool, params: step.params };
  if (step.type === 'OBSERVATION') return { type: 'observation', content: step.content };
  return { type: 'unknown' };
}

function toMsg(step) {
  const base = { id: Date.now() };
  if (step.type === 'FINAL_PLAN') return { ...base, type: 'plan', content: step.content };
  if (step.type === 'CONFIRMING') return { ...base, type: 'confirming', content: step.content };
  if (step.type === 'DONE') return { ...base, type: 'done', content: step.content };
  if (step.type === 'ERROR') return { ...base, type: 'error', content: step.content };
  return { ...base, type: 'text', content: step.content || '' };
}

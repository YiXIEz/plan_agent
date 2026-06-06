import { useState, useEffect, useMemo } from 'react';
import { Bubble, Sender, Conversations, XProvider } from '@ant-design/x';
import { Button, Space, Typography, theme, Switch } from 'antd';
import { RobotOutlined, PlusOutlined } from '@ant-design/icons';
import { marked } from 'marked';
import useChat from './useChat';

marked.setOptions({ breaks: true, gfm: true });

const { Text } = Typography;

export default function App() {
  const { messages, loading, send, loadHistory } = useChat();
  const [convos, setConvos] = useState([]);
  const [activeKey, setActiveKey] = useState(null);
  const [inputVal, setInputVal] = useState('');
  const [deepThinking, setDeepThinking] = useState(false);
  const { token } = theme.useToken();

  function refreshSessions() {
    fetch('/api/sessions').then(r => r.json()).then(data => {
      setConvos(data.map(s => ({ key: s.session_id, label: s.title || '对话' })));
    }).catch(() => {});
  }

  useEffect(() => { refreshSessions(); }, []);
  useEffect(() => { refreshSessions(); }, [messages.length]);

  return (
    <XProvider>
      <div style={{ display: 'flex', height: '100vh', background: token.colorBgLayout }}>
        {/* Sidebar */}
        <div style={{
          width: 260, background: token.colorBgContainer, borderRight: `1px solid ${token.colorBorderSecondary}`,
          display: 'flex', flexDirection: 'column', flexShrink: 0
        }}>
          <div style={{ padding: '16px 16px 8px' }}>
            <Space><RobotOutlined style={{ fontSize: 20, color: token.colorPrimary }} />
              <Text strong style={{ fontSize: 16 }}>活动规划助手</Text>
            </Space>
          </div>
          <div style={{ padding: '0 12px 12px' }}>
            <Button type="primary" block icon={<PlusOutlined />} onClick={() => location.reload()}>新对话</Button>
          </div>
          <div style={{ flex: 1, overflow: 'auto', padding: '0 8px' }}>
            <Conversations
              items={convos}
              activeKey={activeKey}
              onActiveChange={(key) => {
                if (key) {
                  setActiveKey(key);
                  fetch(`/api/sessions/${key}/messages`).then(r => r.json()).then(rows => loadHistory(rows)).catch(() => {});
                }
              }}
            />
          </div>
        </div>

        {/* Main */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
          {loading && (
            <div style={{ padding: '10px 24px', background: token.colorInfoBg, borderBottom: `1px solid ${token.colorInfoBorder}`,
              display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, color: token.colorInfoText }}>
              <span style={{ animation: 'pulse 1.5s infinite', fontSize: 16 }}>🤔</span> Agent 正在分析规划中，请稍候...
            </div>
          )}

          <div style={{ flex: 1, overflow: 'auto', padding: '16px 24px' }}>
            {messages.length === 0 && !loading && (
              <div style={{ textAlign: 'center', paddingTop: 80, color: token.colorTextQuaternary }}>
                <RobotOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                <div style={{ fontSize: 15 }}>输入你想安排的活动，AI 助手为你规划完整方案</div>
              </div>
            )}
            <Bubble.List
              items={messages.filter(m => m.type !== 'round').map(msg => {
                if (msg.role === 'user') return { key: msg.id, role: 'user', content: msg.content };
                if (msg.type === 'plan') return { key: msg.id, role: 'ai', content: '',
                  contentRender: () => <PlanCard content={msg.content} token={token} /> };
                if (msg.type === 'done') return { key: msg.id, role: 'ai', content: msg.content };
                if (msg.type === 'error') return { key: msg.id, role: 'ai', content: msg.content };
                return { key: msg.id, role: 'ai', content: msg.content || '' };
              })}
            />
          </div>

          <div style={{ padding: 16, borderTop: `1px solid ${token.colorBorderSecondary}` }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
              <Switch checked={deepThinking} onChange={setDeepThinking} size="small" />
              <Text type="secondary" style={{ fontSize: 12 }}>深度思考</Text>
            </div>
            <Sender loading={loading} value={inputVal} onChange={setInputVal} placeholder="输入你想安排的活动..." onSubmit={(val) => { if (val.trim()) { send(val, deepThinking); setInputVal(''); } }} />
          </div>
        </div>
      </div>
    </XProvider>
  );
}

function PlanCard({ content, token }) {
  if (!content) return null;
  const html = useMemo(() => marked.parse(content || ''), [content]);
  return <div style={{ background: token.colorWarningBg, border: `1px solid ${token.colorWarningBorder}`,
    borderRadius: 12, padding: '16px 20px', lineHeight: 1.7, fontSize: 14 }}>
    <div dangerouslySetInnerHTML={{ __html: html }}
      style={{ wordBreak: 'break-word' }} /></div>;
}

function formatJson(str) {
  try { return JSON.stringify(JSON.parse(str), null, 2); } catch (e) { return str; }
}

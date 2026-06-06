import { useState, useEffect } from 'react';
import { Bubble, Sender, Conversations, ThoughtChain, XProvider } from '@ant-design/x';
import { Button, Tag, Space, Typography, theme, Switch } from 'antd';
import { RobotOutlined, PlusOutlined } from '@ant-design/icons';
import useChat from './useChat';

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
          <div style={{ height: 36, borderBottom: `1px solid ${token.colorBorderSecondary}`,
            display: 'flex', alignItems: 'center', padding: '0 24px', fontSize: 12, color: token.colorTextTertiary }}>
            {loading ? 'Agent 思考中...' : '就绪'}
          </div>

          <div style={{ flex: 1, overflow: 'auto', padding: '16px 24px' }}>
            {messages.length === 0 && (
              <div style={{ textAlign: 'center', paddingTop: 80, color: token.colorTextQuaternary }}>
                <RobotOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                <div style={{ fontSize: 15 }}>输入你想安排的活动，AI 助手为你规划完整方案</div>
              </div>
            )}
            <Bubble.List
              items={messages.map(msg => {
                if (msg.role === 'user') return { key: msg.id, role: 'user', content: msg.content };
                if (msg.type === 'round') return { key: msg.id, role: 'ai', content: '',
                  contentRender: () => <AgentRound items={msg.children} token={token} /> };
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

function AgentRound({ items, token }) {
  const chainItems = items.map((item, i) => {
    if (item.type === 'thought') return {
      key: i, title: <Text type="secondary" style={{ fontSize: 12 }}>思考</Text>,
      icon: <span style={{ fontSize: 14 }}>💡</span>,
      description: <Text type="secondary" style={{ fontStyle: 'italic' }}>{item.content}</Text>,
    };
    if (item.type === 'action') return {
      key: i, title: <Text type="secondary" style={{ fontSize: 12 }}>调用工具</Text>,
      icon: <span style={{ fontSize: 14 }}>🔧</span>,
      description: <Space size={4}><Tag color="blue" style={{ fontFamily: 'monospace' }}>{item.tool}</Tag>
        {item.params && <Text type="secondary" style={{ fontFamily: 'monospace', fontSize: 12 }}>{item.params?.slice(0, 80)}</Text>}</Space>,
    };
    if (item.type === 'observation') return {
      key: i, title: <Text type="secondary" style={{ fontSize: 12 }}>返回结果</Text>,
      icon: <span style={{ fontSize: 14 }}>👁</span>,
      description: <pre style={{ background: token.colorSuccessBg, border: `1px solid ${token.colorSuccessBorder}`,
        borderRadius: 6, padding: '8px 12px', maxHeight: 160, overflow: 'auto',
        fontFamily: 'monospace', fontSize: 12, color: '#389e0d', whiteSpace: 'pre-wrap', margin: 0 }}>{formatJson(item.content)}</pre>,
    };
    return { key: i, title: '?', description: '' };
  });
  return <div style={{ marginBottom: 8 }}><ThoughtChain items={chainItems} /></div>;
}

function PlanCard({ content, token }) {
  if (!content) return null;
  const html = content
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/^### (.+)$/gm, '<h4 style="margin:10px 0 4px;font-size:15px">$1</h4>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code style="background:rgba(0,0,0,0.06);padding:2px 6px;border-radius:4px;font-size:13px">$1</code>')
    .replace(/^- (.+)$/gm, '<li>$1</li>').replace(/(<li>.*<\/li>)/g, '<ul style="padding-left:20px">$&</ul>')
    .replace(/---/g, '<hr style="border:none;border-top:1px solid rgba(0,0,0,0.06);margin:12px 0">')
    .replace(/\n\n/g, '<br><br>');
  return <div style={{ background: token.colorWarningBg, border: `1px solid ${token.colorWarningBorder}`,
    borderRadius: 12, padding: '16px 20px', lineHeight: 1.7, fontSize: 14 }}>
    <div dangerouslySetInnerHTML={{ __html: html }} /></div>;
}

function formatJson(str) {
  try { return JSON.stringify(JSON.parse(str), null, 2); } catch (e) { return str; }
}

import { useState } from 'react';
import { Bubble, Sender, Conversations, ThoughtChain, XProvider } from '@ant-design/x';
import { Button, Typography, Collapse, Tag, Space, theme } from 'antd';
import { RobotOutlined, ThunderboltOutlined, BulbOutlined, ToolOutlined, EyeOutlined, CheckCircleOutlined } from '@ant-design/icons';
import useChat from './useChat';
import './App.css';

const { Title } = Typography;

const DEFAULT_CONVERSATIONS = [{ key: '0', label: '新对话' }];

export default function App() {
  const { messages, loading, send, confirmPlan } = useChat();
  const [conversations] = useState(DEFAULT_CONVERSATIONS);
  const [activeConvo, setActiveConvo] = useState('0');
  const { token } = theme.useToken();

  // Flatten messages for Bubble.List display
  const bubbleItems = [];
  messages.forEach((msg) => {
    if (msg.role === 'user') {
      bubbleItems.push({ key: msg.id, placement: 'end', content: msg.content, variant: 'filled' });
    } else if (msg.type === 'round') {
      bubbleItems.push({
        key: msg.id,
        placement: 'start',
        variant: 'borderless',
        messageRender: () => <RoundCard items={msg.children} token={token} />,
      });
    } else if (msg.type === 'plan') {
      bubbleItems.push({
        key: msg.id,
        placement: 'start',
        variant: 'borderless',
        messageRender: () => <PlanCard content={msg.content} onConfirm={confirmPlan} />,
      });
    } else if (msg.type === 'done') {
      bubbleItems.push({
        key: msg.id, placement: 'start',
        messageRender: () => <div className="done-msg">{msg.content}</div>,
      });
    } else if (msg.type === 'error') {
      bubbleItems.push({
        key: msg.id, placement: 'start',
        messageRender: () => <div className="error-msg">❌ {msg.content}</div>,
      });
    }
  });

  return (
    <XProvider theme={{ algorithm: [theme.defaultAlgorithm] }}>
      <div className="container">
        {/* Sidebar */}
        <div className="sidebar">
          <div className="sidebar-header">
            <Space>
              <RobotOutlined style={{ fontSize: 20, color: token.colorPrimary }} />
              <span>活动规划助手</span>
            </Space>
            <Button type="primary" block style={{ marginTop: 14 }} onClick={() => window.location.reload()}>
              新对话
            </Button>
          </div>
          <div className="conversation-list">
            <Conversations
              items={conversations.map((c) => ({ ...c, label: '✨ ' + c.label }))}
              activeKey={activeConvo}
              onActiveChange={setActiveConvo}
            />
          </div>
        </div>

        {/* Main chat area */}
        <div className="main">
          <div className="chat-area">
            {messages.length === 0 && (
              <div className="welcome-msg">
                <RobotOutlined style={{ fontSize: 48, color: '#d9d9d9', marginBottom: 16 }} />
                <div>输入你想安排的活动，AI 助手会为你规划完整的周末方案</div>
              </div>
            )}
            <Bubble.List
              items={bubbleItems}
              style={{ flex: 1 }}
            />
          </div>

          <div className="input-area">
            <Sender
              loading={loading ? 'loading' : undefined}
              placeholder="输入你想安排的活动..."
              onSubmit={(text) => { if (text.trim()) send(text); }}
              onCancel={() => {}}
            />
          </div>
        </div>
      </div>
    </XProvider>
  );
}

/** Render a single round: Thought → Action → Observation */
function RoundCard({ items, token }) {
  const chainItems = items.map((item, i) => {
    if (item.type === 'thought') {
      return {
        key: i,
        icon: <BulbOutlined style={{ color: token.colorWarning }} />,
        title: '思考',
        description: item.content,
      };
    }
    if (item.type === 'action') {
      return {
        key: i,
        icon: <ToolOutlined style={{ color: token.colorPrimary }} />,
        title: '调用工具',
        description: (
          <span>
            <Tag color="blue" style={{ fontFamily: 'monospace' }}>{item.tool}</Tag>
            {item.params && <span style={{ fontFamily: 'monospace', fontSize: 12, color: token.colorTextSecondary }}>{truncate(item.params, 100)}</span>}
          </span>
        ),
      };
    }
    if (item.type === 'observation') {
      return {
        key: i,
        icon: <EyeOutlined style={{ color: token.colorSuccess }} />,
        title: '返回结果',
        description: (
          <Collapse
            size="small"
            ghost
            items={[{ key: '1', label: '查看数据', children: <pre className="obs-box">{formatJson(item.content)}</pre> }]}
          />
        ),
      };
    }
    return { key: i, title: '?', description: '' };
  });

  return (
    <div style={{ marginBottom: 8 }}>
      <ThoughtChain items={chainItems} />
    </div>
  );
}

/** Render the final plan with markdown */
function PlanCard({ content, onConfirm }) {
  // Simple inline markdown rendering
  const html = renderMarkdown(content);
  return (
    <div className="plan-card">
      <div dangerouslySetInnerHTML={{ __html: html }} />
      <div className="plan-actions">
        <Button type="primary" icon={<CheckCircleOutlined />} onClick={onConfirm}>确认方案</Button>
        <Button onClick={() => window.dispatchEvent(new CustomEvent('adjust-plan'))}>调整方案</Button>
      </div>
    </div>
  );
}

// ── helpers ──

function renderMarkdown(text) {
  if (!text) return '';
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    // headers
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^# (.+)$/gm, '<h1>$1</h1>')
    // bold
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    // inline code
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // horizontal rules
    .replace(/^---$/gm, '<hr>')
    // tables (simple: | col | col | ... with separator row)
    .replace(/^\|(.+)\|\n\|[-| :]+\|\n((?:\|.+\|\n?)*)/gm, (_, header, rows) => {
      const hcells = header.split('|').map(h => `<th>${h.trim()}</th>`).join('');
      const rrows = rows.trim().split('\n').map(row =>
        `<tr>${row.split('|').map(c => `<td>${c.trim()}</td>`).join('')}</tr>`
      ).join('');
      return `<table><thead><tr>${hcells}</tr></thead><tbody>${rrows}</tbody></table>`;
    })
    // unordered lists
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
    .replace(/<\/li>\n<li>/g, '</li><li>')
    // paragraphs
    .replace(/\n\n/g, '</p><p>')
    .replace(/^(?!<[hutlpc])(.+)$/gm, '<p>$1</p>')
    // cleanup empty tags
    .replace(/<p><\/p>/g, '')
    .replace(/<p>(<[hutl])/g, '$1')
    .replace(/(<\/[hutl]{1,4}>)<\/p>/g, '$1');
}

function formatJson(str) {
  try { return JSON.stringify(JSON.parse(str), null, 2); } catch(e) { return str; }
}

function truncate(s, n) { return s?.length > n ? s.substring(0, n) + '...' : s; }

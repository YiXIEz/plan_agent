-- 对话历史存储
-- 数据库需手动创建: CREATE DATABASE IF NOT EXISTS plan_agent DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS sessions (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    session_id   VARCHAR(64)  NOT NULL UNIQUE COMMENT 'WebSocket session id',
    title        VARCHAR(200) COMMENT '对话标题',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_created (created_at)
);

CREATE TABLE IF NOT EXISTS messages (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    session_id   VARCHAR(64)  NOT NULL COMMENT 'WebSocket session id',
    seq          INT          NOT NULL COMMENT '会话内序号，从0开始',
    role         VARCHAR(16)  NOT NULL COMMENT 'user | assistant | tool',
    content      TEXT         COMMENT '消息正文',
    step_type    VARCHAR(16)  COMMENT 'THOUGHT | ACTION | OBSERVATION | FINAL_PLAN | DONE | ERROR',
    tool_name    VARCHAR(64)  COMMENT '工具名',
    tool_params  VARCHAR(2000) COMMENT '工具参数JSON',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_seq (session_id, seq)
);

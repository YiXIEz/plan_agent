-- 对话历史存储
-- 数据库需手动创建: CREATE DATABASE IF NOT EXISTS plan_agent DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS sessions (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    session_id   VARCHAR(64)  NOT NULL UNIQUE,
    title        VARCHAR(200),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS messages (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    session_id   VARCHAR(64)  NOT NULL,
    seq          INT          NOT NULL,
    role         VARCHAR(16)  NOT NULL,
    content      CLOB,
    step_type    VARCHAR(16),
    tool_name    VARCHAR(64),
    tool_params  VARCHAR(2000),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

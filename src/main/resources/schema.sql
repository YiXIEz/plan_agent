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

CREATE INDEX IF NOT EXISTS idx_session_seq ON messages(session_id, seq);
CREATE INDEX IF NOT EXISTS idx_created ON sessions(created_at);

CREATE TABLE chat_messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT, -- 允许为空，如果是 app 类型则内容存储在文件系统
    type VARCHAR(50) NOT NULL DEFAULT 'text', -- 'text' 或 'app'
    related_app_id BIGINT,      -- 关联的应用ID (方便快速跳转应用详情)
    related_version_id BIGINT,  -- 关联的具体版本ID (核心：用于历史回溯)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- 外键约束
    CONSTRAINT fk_chat_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_app FOREIGN KEY (related_app_id) REFERENCES apps(id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_messages_version FOREIGN KEY (related_version_id) REFERENCES app_versions(id) ON DELETE SET NULL
);

-- 索引
CREATE INDEX idx_chat_messages_session ON chat_messages(session_id);
CREATE INDEX idx_chat_messages_user ON chat_messages(user_id);

-- 注释
COMMENT ON TABLE chat_messages IS '聊天消息表';
COMMENT ON COLUMN chat_messages.type IS '消息类型 (text, app_card)';
COMMENT ON COLUMN chat_messages.related_app_id IS '关联的应用ID (方便查询)';
COMMENT ON COLUMN chat_messages.related_version_id IS '关联的具体版本ID (确保历史记录指向当时生成的快照)';

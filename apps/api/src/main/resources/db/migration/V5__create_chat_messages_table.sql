CREATE TABLE chat_messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'text',
    related_app_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chat_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_app FOREIGN KEY (related_app_id) REFERENCES apps(id) ON DELETE SET NULL
);

-- 加上索引，以后查历史记录才快
CREATE INDEX idx_chat_messages_session ON chat_messages(session_id);
CREATE INDEX idx_chat_messages_user ON chat_messages(user_id);

COMMENT ON TABLE chat_messages IS '聊天消息表';
COMMENT ON COLUMN chat_messages.id IS '主键ID';
COMMENT ON COLUMN chat_messages.user_id IS '用户ID';
COMMENT ON COLUMN chat_messages.session_id IS '会话ID';
COMMENT ON COLUMN chat_messages.role IS '角色 (user/assistant)';
COMMENT ON COLUMN chat_messages.content IS '聊天内容';
COMMENT ON COLUMN chat_messages.created_at IS '创建时间';
COMMENT ON COLUMN chat_messages.type IS '消息类型 (text, app)';
COMMENT ON COLUMN chat_messages.related_app_id IS '关联的应用ID (仅当 type=app 时有效)';

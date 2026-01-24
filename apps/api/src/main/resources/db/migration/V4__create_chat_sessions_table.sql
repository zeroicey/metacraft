CREATE TABLE chat_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL UNIQUE, -- 业务层的UUID，用于前端路由和关联消息
    title TEXT,                             -- 会话标题，可以是第一句问话，也可以是AI总结的
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TRIGGER chat_sessions_updated_at_trigger BEFORE
UPDATE ON chat_sessions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column ();

-- 创建索引
CREATE INDEX idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX idx_chat_sessions_session_id ON chat_sessions(session_id);
CREATE INDEX idx_chat_sessions_updated_at ON chat_sessions(updated_at DESC); -- 方便按时间倒序展示列表

-- 添加注释
COMMENT ON TABLE chat_sessions IS '对话会话表，用于管理对话窗口';
COMMENT ON COLUMN chat_sessions.id IS '主键ID';
COMMENT ON COLUMN chat_sessions.user_id IS '所属用户ID';
COMMENT ON COLUMN chat_sessions.session_id IS '业务会话ID(UUID), 用于关联消息';
COMMENT ON COLUMN chat_sessions.title IS '会话标题(通常取第一条消息的前N个字)';
COMMENT ON COLUMN chat_sessions.created_at IS '创建时间';
COMMENT ON COLUMN chat_sessions.updated_at IS '最后活动时间(用于排序)';


ALTER TABLE chat_messages
ADD CONSTRAINT fk_chat_messages_session
FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE;
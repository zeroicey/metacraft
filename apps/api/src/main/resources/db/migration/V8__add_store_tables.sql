-- 新增字段到 apps 表
ALTER TABLE apps
ADD COLUMN average_rating DOUBLE PRECISION,
ADD COLUMN rating_count INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN apps.average_rating IS 'Average rating (cached)';
COMMENT ON COLUMN apps.rating_count IS 'Number of ratings';

-- 创建 app_ratings 表
CREATE TABLE app_ratings (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (app_id, user_id)
);

CREATE INDEX idx_app_ratings_app_id ON app_ratings(app_id);

COMMENT ON TABLE app_ratings IS 'App ratings table';
COMMENT ON COLUMN app_ratings.app_id IS 'App ID';
COMMENT ON COLUMN app_ratings.user_id IS 'User who rated';
COMMENT ON COLUMN app_ratings.rating IS 'Rating 1-5';

-- 创建 app_comments 表
CREATE TABLE app_comments (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_comments_app_id ON app_comments(app_id);

COMMENT ON TABLE app_comments IS 'App comments table';
COMMENT ON COLUMN app_comments.app_id IS 'App ID';
COMMENT ON COLUMN app_comments.user_id IS 'Comment author';
COMMENT ON COLUMN app_comments.content IS 'Comment content';
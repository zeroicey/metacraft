-- 1. 创建 apps 表 (应用容器)
CREATE TABLE apps (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255),
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    current_version_id BIGINT, -- 指向最新版本的ID (稍后添加外键约束，避免循环依赖)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_apps_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 2. 创建 app_versions 表 (版本快照)
CREATE TABLE app_versions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_id BIGINT NOT NULL,
    version_number INT NOT NULL, -- 版本号，如 1, 2, 3
    storage_path VARCHAR(512) NOT NULL, -- 代码存储路径 (如: storage/apps/1/v1/index.html)
    change_log TEXT,             -- 修改说明 (AI 生成的 commit message)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_app_versions_app FOREIGN KEY (app_id) REFERENCES apps(id) ON DELETE CASCADE
);

-- 3. 回填 apps 表的外键 (current_version_id)
ALTER TABLE apps
ADD CONSTRAINT fk_apps_current_version 
FOREIGN KEY (current_version_id) REFERENCES app_versions(id) ON DELETE SET NULL;

-- 4. 触发器
CREATE TRIGGER apps_updated_at_trigger BEFORE
UPDATE ON apps FOR EACH ROW EXECUTE FUNCTION update_updated_at_column ();

-- 5. 索引
CREATE INDEX idx_apps_user_id ON apps(user_id);
CREATE INDEX idx_apps_updated_at ON apps(updated_at DESC);
CREATE INDEX idx_app_versions_app_id ON app_versions(app_id);

-- 6. 注释
COMMENT ON TABLE apps IS '应用容器表(元数据)';
COMMENT ON COLUMN apps.current_version_id IS '当前最新版本ID';

COMMENT ON TABLE app_versions IS '应用版本历史表(代码快照)';
COMMENT ON COLUMN app_versions.version_number IS '版本序号(递增)';
COMMENT ON COLUMN app_versions.storage_path IS '代码文件存储路径';

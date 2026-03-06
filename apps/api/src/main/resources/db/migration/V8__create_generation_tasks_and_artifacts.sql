CREATE TABLE generation_tasks (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempt INT NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_generation_tasks_run_task UNIQUE (run_id, task_type)
);

CREATE INDEX idx_generation_tasks_run_id ON generation_tasks (run_id);
CREATE INDEX idx_generation_tasks_status ON generation_tasks (status);

CREATE TABLE generation_artifacts (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    artifact_type VARCHAR(32) NOT NULL,
    ref_id BIGINT,
    content_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_generation_artifacts_run_id ON generation_artifacts (run_id);
CREATE INDEX idx_generation_artifacts_type ON generation_artifacts (artifact_type);

CREATE OR REPLACE FUNCTION update_generation_tasks_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trg_generation_tasks_updated_at
    BEFORE UPDATE ON generation_tasks
    FOR EACH ROW
EXECUTE FUNCTION update_generation_tasks_updated_at();

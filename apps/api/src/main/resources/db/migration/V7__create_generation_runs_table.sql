CREATE TABLE generation_runs (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64),
    intent VARCHAR(16),
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_generation_runs_user_created_at
    ON generation_runs (user_id, created_at DESC);

CREATE INDEX idx_generation_runs_status
    ON generation_runs (status);

CREATE OR REPLACE FUNCTION update_generation_runs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trg_generation_runs_updated_at
    BEFORE UPDATE ON generation_runs
    FOR EACH ROW
EXECUTE FUNCTION update_generation_runs_updated_at();

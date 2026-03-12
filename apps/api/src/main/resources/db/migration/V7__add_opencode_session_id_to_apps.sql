ALTER TABLE apps
ADD COLUMN open_code_session_id VARCHAR(128);

COMMENT ON COLUMN apps.open_code_session_id IS 'Bound OpenCode session ID for app generation/edit reuse';
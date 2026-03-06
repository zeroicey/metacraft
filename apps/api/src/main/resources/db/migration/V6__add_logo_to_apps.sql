-- Add logo field to apps table.
-- Store only filename (e.g. uuid.png), actual files are under apps/logos/.
ALTER TABLE apps
ADD COLUMN logo VARCHAR(128);

COMMENT ON COLUMN apps.logo IS 'Logo filename (uuid.ext), files stored under apps/logos/';

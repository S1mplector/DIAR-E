-- Constraints and indexes for DIAR-E

-- Unique category name (case-insensitive)
CREATE UNIQUE INDEX IF NOT EXISTS ux_categories_name ON categories(lower(name));

-- Foreign key helper indexes
CREATE INDEX IF NOT EXISTS ix_towers_category ON towers(category_id);
CREATE INDEX IF NOT EXISTS ix_logs_category ON logs(category_id);

-- Time-based query indexes
CREATE INDEX IF NOT EXISTS ix_logs_created_at ON logs(created_at);
CREATE INDEX IF NOT EXISTS ix_recordings_created_at ON recordings(created_at);

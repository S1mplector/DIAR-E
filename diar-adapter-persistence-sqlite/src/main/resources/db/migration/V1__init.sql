-- Initial schema for DIAR-E

CREATE TABLE IF NOT EXISTS categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    tower_block_target INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS towers (
    id TEXT PRIMARY KEY,
    category_id TEXT NOT NULL,
    block_target INTEGER NOT NULL,
    blocks_completed INTEGER NOT NULL,
    completed_on DATE,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS logs (
    id TEXT PRIMARY KEY,
    category_id TEXT NOT NULL,
    note TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS recordings (
    id TEXT PRIMARY KEY,
    file_path TEXT NOT NULL,
    created_at TEXT NOT NULL,
    duration_seconds INTEGER
);

CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

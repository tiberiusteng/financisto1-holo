ALTER TABLE account ADD COLUMN icon TEXT NOT NULL DEFAULT '';
ALTER TABLE account ADD COLUMN accent_color TEXT NOT NULL DEFAULT '';

CREATE TABLE IF NOT EXISTS account_separator (
    _id             INTEGER     PRIMARY KEY AUTOINCREMENT,
    title           TEXT        NOT NULL DEFAULT '',
    sort_order      INTEGER     NOT NULL DEFAULT 0,
    accent_color    TEXT        NOT NULL DEFAULT ''
);
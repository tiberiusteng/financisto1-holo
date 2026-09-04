ALTER TABLE payee ADD COLUMN aliases TEXT;
ALTER TABLE locations ADD COLUMN aliases TEXT;

CREATE TABLE IF NOT EXISTS payee_aliases (
    _id INTEGER NOT NULL,
    alias TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS locations_aliases (
    _id INTEGER NOT NULL,
    alias TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS payee_aliases_id_idx ON payee_aliases (_id);
CREATE INDEX IF NOT EXISTS payee_aliases_alias_idx ON payee_aliases (alias);

CREATE INDEX IF NOT EXISTS locations_aliases_id_idx ON locations_aliases (_id);
CREATE INDEX IF NOT EXISTS locations_aliases_alias_idx ON locations_aliases (alias);
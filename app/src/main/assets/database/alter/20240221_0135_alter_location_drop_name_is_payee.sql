CREATE TABLE IF NOT EXISTS locations_t (
    _id              INTEGER   PRIMARY KEY AUTOINCREMENT,
    datetime         LONG      NOT NULL,
    provider         TEXT,
    accuracy         FLOAT,
    latitude         DOUBLE,
    longitude        DOUBLE,
    resolved_address TEXT,
    count            INTEGER   NOT NULL
                               DEFAULT 0,
    updated_on       TIMESTAMP DEFAULT 0,
    remote_key       TEXT,
    title            TEXT,
    sort_order       INTEGER   NOT NULL
                               DEFAULT 0,
    is_active        BOOLEAN   NOT NULL
                               DEFAULT 1
);

INSERT INTO locations_t (_id, datetime, provider, accuracy, latitude, longitude, resolved_address,
    count, updated_on, remote_key, title, sort_order, is_active)
    SELECT _id, datetime, provider, accuracy, latitude, longitude, resolved_address, count,
        updated_on, remote_key, title, sort_order, is_active FROM locations;

DROP TABLE locations;

ALTER TABLE locations_t RENAME TO locations;
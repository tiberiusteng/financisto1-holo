CREATE TABLE IF NOT EXISTS sms_template_t (
    _id           INTEGER   PRIMARY KEY AUTOINCREMENT,
    title         TEXT      NOT NULL,
    template      TEXT      NOT NULL,
    category_id   INTEGER   NOT NULL,
    account_id    INTEGER,
    payee_id      INTEGER   NOT NULL
                            DEFAULT 0,
    project_id    INTEGER   NOT NULL
                            DEFAULT 0,
    updated_on    TIMESTAMP DEFAULT 0,
    remote_key    TEXT,
    is_income     BOOLEAN   NOT NULL
                            DEFAULT 0,
    sort_order    INTEGER   NOT NULL
                            DEFAULT 0,
    to_account_id INTEGER   NOT NULL
                            DEFAULT -1,
    note          TEXT,
    is_active     BOOLEAN   NOT NULL
                            DEFAULT 1
);

INSERT INTO sms_template_t (_id, title, template, category_id, account_id, updated_on, remote_key,
    is_income, sort_order, to_account_id, note, is_active)
    SELECT _id, title, template, category_id, account_id, updated_on, remote_key, is_income,
        sort_order, to_account_id, note, is_active FROM sms_template;

DROP TABLE sms_template;

ALTER TABLE sms_template_t RENAME TO sms_template;
DROP INDEX IF EXISTS transaction_pid_idx;

CREATE INDEX IF NOT EXISTS transaction_pid_idx ON transactions (parent_id, _id);
CREATE VIEW v_blotter_with_splits AS
SELECT *
FROM v_all_transactions
WHERE is_template = 0